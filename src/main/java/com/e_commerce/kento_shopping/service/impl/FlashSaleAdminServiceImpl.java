package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.request.admin.FlashSaleRequest;
import com.e_commerce.kento_shopping.dto.response.FlashSaleResponse;
import com.e_commerce.kento_shopping.entity.FlashSale;
import com.e_commerce.kento_shopping.entity.Inventory;
import com.e_commerce.kento_shopping.entity.Product;
import com.e_commerce.kento_shopping.enums.FlashSaleStatus;
import com.e_commerce.kento_shopping.exception.FlashSaleNotFoundException;
import com.e_commerce.kento_shopping.exception.ProductNotFoundException;
import com.e_commerce.kento_shopping.repository.FlashSaleRepository;
import com.e_commerce.kento_shopping.repository.ProductRepository;
import com.e_commerce.kento_shopping.service.FlashSaleAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FlashSaleAdminServiceImpl implements FlashSaleAdminService {

    private static final Set<FlashSaleStatus> OPEN_STATUSES =
            EnumSet.of(FlashSaleStatus.SCHEDULED, FlashSaleStatus.ACTIVE,
                    FlashSaleStatus.PAUSED, FlashSaleStatus.CLOSING);

    private final FlashSaleRepository flashSaleRepository;
    private final ProductRepository productRepository;

    private FlashSaleResponse mapToResponse(FlashSale sale) {
        Product product = sale.getProduct();
        return new FlashSaleResponse(
                sale.getId(),
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                sale.getName(),
                sale.getSalePrice(),
                sale.getAllocatedQty(),
                sale.getSoldQty(),
                sale.getStartAt(),
                sale.getEndAt(),
                sale.getStatus(),
                sale.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FlashSaleResponse> getAll(FlashSaleStatus status, Pageable pageable) {
        Page<FlashSale> page = (status != null)
                ? flashSaleRepository.findByStatus(status, pageable)
                : flashSaleRepository.findAll(pageable);
        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FlashSaleResponse getById(Long id) {
        return mapToResponse(findSale(id));
    }

    @Override
    @Transactional
    public FlashSaleResponse create(FlashSaleRequest request) {
        Product product = findProduct(request.getProductId());
        validate(product, request);
        if (flashSaleRepository.existsByProductAndStatusIn(product, OPEN_STATUSES)) {
            throw new IllegalArgumentException("This product already has an active or scheduled sale");
        }
        FlashSale sale = FlashSale.builder()
                .product(product)
                .name(request.getName())
                .salePrice(request.getSalePrice())
                .allocatedQty(request.getAllocatedQty())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .build();
        return mapToResponse(flashSaleRepository.save(sale));
    }

    @Override
    @Transactional
    public FlashSaleResponse update(Long id, FlashSaleRequest request) {
        FlashSale sale = requireScheduled(id, "Only scheduled sales can be edited");
        Product product = findProduct(request.getProductId());
        validate(product, request);
        if (flashSaleRepository.existsByProductAndStatusInAndIdNot(product, OPEN_STATUSES, id)) {
            throw new IllegalArgumentException("This product already has an active or scheduled sale");
        }
        sale.setProduct(product);
        sale.setName(request.getName());
        sale.setSalePrice(request.getSalePrice());
        sale.setAllocatedQty(request.getAllocatedQty());
        sale.setStartAt(request.getStartAt());
        sale.setEndAt(request.getEndAt());
        return mapToResponse(sale);
    }

    @Override
    @Transactional
    public FlashSaleResponse cancel(Long id) {
        FlashSale sale = requireScheduled(id, "Only scheduled sales can be cancelled");
        sale.setStatus(FlashSaleStatus.CANCELLED);
        return mapToResponse(sale);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FlashSale sale = findSale(id);
        if (sale.getStatus() != FlashSaleStatus.SCHEDULED && sale.getStatus() != FlashSaleStatus.CANCELLED) {
            throw new IllegalArgumentException("Only scheduled or cancelled sales can be deleted");
        }
        flashSaleRepository.delete(sale);
    }

    private void validate(Product product, FlashSaleRequest request) {
        if (!request.getEndAt().isAfter(request.getStartAt())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (request.getSalePrice().compareTo(product.getPrice()) >= 0) {
            throw new IllegalArgumentException("Sale price must be lower than the product's price");
        }
        Inventory inventory = product.getInventory();
        int inStock = inventory == null ? 0 : inventory.getQuantity();
        if (request.getAllocatedQty() > inStock) {
            throw new IllegalArgumentException(
                    "Cannot allocate " + request.getAllocatedQty() + " units; only " + inStock + " in stock");
        }
    }

    private FlashSale requireScheduled(Long id, String message) {
        FlashSale sale = findSale(id);
        if (sale.getStatus() != FlashSaleStatus.SCHEDULED) {
            throw new IllegalArgumentException(message);
        }
        return sale;
    }

    private FlashSale findSale(Long id) {
        return flashSaleRepository.findById(id)
                .orElseThrow(() -> new FlashSaleNotFoundException("Flash sale not found"));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
    }
}
