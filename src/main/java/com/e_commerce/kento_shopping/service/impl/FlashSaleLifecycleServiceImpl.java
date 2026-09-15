package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.entity.BaseEntity;
import com.e_commerce.kento_shopping.entity.FlashSale;
import com.e_commerce.kento_shopping.entity.Inventory;
import com.e_commerce.kento_shopping.enums.FlashSaleStatus;
import com.e_commerce.kento_shopping.exception.FlashSaleNotFoundException;
import com.e_commerce.kento_shopping.redis.FlashSaleStockStore;
import com.e_commerce.kento_shopping.repository.FlashSaleRepository;
import com.e_commerce.kento_shopping.service.FlashSaleLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleLifecycleServiceImpl implements FlashSaleLifecycleService {

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleStockStore stockStore;

    @Override
    @Transactional(readOnly = true)
    public List<Long> dueToActivate(LocalDateTime now) {
        return ids(flashSaleRepository.findByStatusAndStartAtLessThanEqual(FlashSaleStatus.SCHEDULED, now));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> dueToClose(LocalDateTime now) {
        return ids(flashSaleRepository.findByStatusInAndEndAtLessThanEqual(
                EnumSet.of(FlashSaleStatus.ACTIVE, FlashSaleStatus.PAUSED), now));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> idsIn(FlashSaleStatus status) {
        return ids(flashSaleRepository.findByStatus(status));
    }

    @Override
    @Transactional
    public void activate(Long saleId, boolean manual) {
        FlashSale sale = findSale(saleId);
        if (sale.getStatus() != FlashSaleStatus.SCHEDULED) {
            throw new IllegalArgumentException("Only scheduled sales can be activated");
        }
        LocalDateTime now = LocalDateTime.now();
        if (manual) {
            if (!sale.getEndAt().isAfter(now)) {
                throw new IllegalArgumentException("This sale's end time has already passed");
            }
            sale.setStartAt(now);
        }

        Inventory inventory = sale.getProduct().getInventory();
        int inStock = inventory == null ? 0 : inventory.getQuantity();
        if (sale.getAllocatedQty() > inStock) {
            throw new IllegalArgumentException(
                    "Cannot allocate " + sale.getAllocatedQty() + " units; only " + inStock + " in stock");
        }
        inventory.setQuantity(inStock - sale.getAllocatedQty());
        sale.setStatus(FlashSaleStatus.ACTIVE);

        int quantity = sale.getAllocatedQty();
        afterCommit(() -> {
            try {
                stockStore.loadIfAbsent(saleId, quantity);
            } catch (Exception e) {
                log.warn("Flash sale {} activated but its stock key was not loaded: {}", saleId, e.getMessage());
            }
        });
    }

    @Override
    @Transactional
    public void beginClose(Long saleId, boolean manual) {
        FlashSale sale = findSale(saleId);
        if (sale.getStatus() != FlashSaleStatus.ACTIVE && sale.getStatus() != FlashSaleStatus.PAUSED) {
            throw new IllegalArgumentException("Only active or paused sales can be closed");
        }
        if (manual) {
            sale.setEndAt(LocalDateTime.now());
        }
        sale.setStatus(FlashSaleStatus.CLOSING);
        afterCommit(() -> {
            try {
                stockStore.stopClaims(saleId);
            } catch (Exception e) {
                log.warn("Flash sale {} is closing but its stock key was not removed: {}", saleId, e.getMessage());
            }
        });
    }

    @Override
    @Transactional
    public void settle(Long saleId) {
        FlashSale sale = findSale(saleId);
        if (sale.getStatus() != FlashSaleStatus.CLOSING) {
            return;
        }
        stockStore.stopClaims(saleId);
        if (stockStore.inflight(saleId) > 0) {
            return;
        }
        Inventory inventory = sale.getProduct().getInventory();
        inventory.setQuantity(inventory.getQuantity() + sale.getAllocatedQty() - sale.getSoldQty());
        sale.setStatus(FlashSaleStatus.ENDED);
        afterCommit(() -> stockStore.clearInflight(saleId));
    }

    @Override
    @Transactional(readOnly = true)
    public void restoreStockKey(Long saleId) {
        FlashSale sale = findSale(saleId);
        if (sale.getStatus() != FlashSaleStatus.ACTIVE) {
            return;
        }
        int remaining = sale.getAllocatedQty() - sale.getSoldQty() - stockStore.inflight(saleId);
        if (stockStore.loadIfAbsent(saleId, Math.max(remaining, 0))) {
            log.warn("Flash sale {} was missing its stock key; restored to {}", saleId, remaining);
        }
    }

    @Override
    @Transactional
    public void pause(Long saleId) {
        FlashSale sale = findSale(saleId);
        if (sale.getStatus() == FlashSaleStatus.ACTIVE) {
            sale.setStatus(FlashSaleStatus.PAUSED);
        }
    }

    @Override
    @Transactional
    public void resume(Long saleId) {
        FlashSale sale = findSale(saleId);
        if (sale.getStatus() == FlashSaleStatus.PAUSED) {
            sale.setStatus(FlashSaleStatus.ACTIVE);
        }
    }

    private FlashSale findSale(Long id) {
        return flashSaleRepository.findById(id)
                .orElseThrow(() -> new FlashSaleNotFoundException("Flash sale not found"));
    }

    private static List<Long> ids(List<FlashSale> sales) {
        return sales.stream().map(BaseEntity::getId).toList();
    }

    private static void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
