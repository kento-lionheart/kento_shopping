package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.request.FlashSalePurchaseRequest;
import com.e_commerce.kento_shopping.dto.response.FlashSaleClaimResponse;
import com.e_commerce.kento_shopping.dto.response.PublicFlashSaleResponse;
import com.e_commerce.kento_shopping.entity.BaseEntity;
import com.e_commerce.kento_shopping.entity.FlashSale;
import com.e_commerce.kento_shopping.entity.Product;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.enums.ClaimStatus;
import com.e_commerce.kento_shopping.enums.FlashSaleStatus;
import com.e_commerce.kento_shopping.exception.ClaimNotFoundException;
import com.e_commerce.kento_shopping.exception.FlashSaleNotActiveException;
import com.e_commerce.kento_shopping.exception.FlashSaleNotFoundException;
import com.e_commerce.kento_shopping.exception.FlashSaleSoldOutException;
import com.e_commerce.kento_shopping.exception.ResourceAccessDeniedException;
import com.e_commerce.kento_shopping.redis.FlashSaleStockStore;
import com.e_commerce.kento_shopping.repository.FlashSaleRepository;
import com.e_commerce.kento_shopping.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleServiceImpl implements FlashSaleService {

    private static final Set<FlashSaleStatus> LIVE_STATUSES =
            EnumSet.of(FlashSaleStatus.ACTIVE, FlashSaleStatus.PAUSED);

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleStockStore stockStore;

    private PublicFlashSaleResponse mapToResponse(FlashSale sale, Integer remaining) {
        Product product = sale.getProduct();
        return new PublicFlashSaleResponse(
                sale.getId(),
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                sale.getName(),
                sale.getSalePrice(),
                sale.getAllocatedQty(),
                remaining,
                sale.getStartAt(),
                sale.getEndAt(),
                sale.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicFlashSaleResponse> getLive() {
        List<FlashSale> sales = flashSaleRepository.findByStatusInOrderByEndAtAsc(LIVE_STATUSES);
        List<Integer> remaining = remainingOf(sales);
        List<PublicFlashSaleResponse> responses = new ArrayList<>(sales.size());
        for (int i = 0; i < sales.size(); i++) {
            responses.add(mapToResponse(sales.get(i), remaining.get(i)));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicFlashSaleResponse getLive(Long id) {
        FlashSale sale = flashSaleRepository.findById(id)
                .filter(s -> LIVE_STATUSES.contains(s.getStatus()))
                .orElseThrow(() -> new FlashSaleNotFoundException("Flash sale not found"));
        return mapToResponse(sale, remainingOf(List.of(sale)).get(0));
    }

    @Override
    public FlashSaleClaimResponse purchase(User user, Long saleId, FlashSalePurchaseRequest request) {
        String claimId = UUID.randomUUID().toString();
        int quantity = request.getQuantity();
        long result = stockStore.claim(saleId, user.getId(), claimId, quantity);
        if (result == -1) {
            throw new FlashSaleSoldOutException("Sold out — not enough stock left for this quantity");
        }
        if (result == -3) {
            throw new FlashSaleNotActiveException("This sale is not active");
        }
        if (result == -4) {
            throw new IllegalArgumentException("You may purchase between 1 and 3 units per request");
        }
        return new FlashSaleClaimResponse(claimId, saleId, quantity, ClaimStatus.PROCESSING, null, null);
    }

    @Override
    public FlashSaleClaimResponse getClaim(User user, String claimId) {
        Map<String, String> claim = stockStore.claimStatus(claimId);
        if (claim.isEmpty()) {
            throw new ClaimNotFoundException("Claim not found");
        }
        if (!claim.get("userId").equals(String.valueOf(user.getId()))) {
            throw new ResourceAccessDeniedException("You do not have access to this claim");
        }
        return new FlashSaleClaimResponse(
                claimId,
                Long.valueOf(claim.get("saleId")),
                Integer.valueOf(claim.get("quantity")),
                ClaimStatus.valueOf(claim.get("status")),
                claim.get("message"),
                claim.containsKey("orderId") ? Long.valueOf(claim.get("orderId")) : null);
    }

    private List<Integer> remainingOf(List<FlashSale> sales) {
        if (sales.isEmpty()) {
            return List.of();
        }
        try {
            List<Integer> remaining = stockStore.remaining(sales.stream().map(BaseEntity::getId).toList());
            if (remaining != null) {
                return remaining;
            }
        } catch (Exception e) {
            log.warn("Could not read flash-sale stock from Redis: {}", e.getMessage());
        }
        return Collections.nCopies(sales.size(), null);
    }
}
