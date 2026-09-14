package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.response.PublicFlashSaleResponse;
import com.e_commerce.kento_shopping.entity.BaseEntity;
import com.e_commerce.kento_shopping.entity.FlashSale;
import com.e_commerce.kento_shopping.entity.Product;
import com.e_commerce.kento_shopping.enums.FlashSaleStatus;
import com.e_commerce.kento_shopping.exception.FlashSaleNotFoundException;
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
import java.util.Set;

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
