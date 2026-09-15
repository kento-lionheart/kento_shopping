package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.response.FlashSaleReconciliationResponse;
import com.e_commerce.kento_shopping.entity.FlashSale;
import com.e_commerce.kento_shopping.enums.FlashSaleStatus;
import com.e_commerce.kento_shopping.exception.FlashSaleNotFoundException;
import com.e_commerce.kento_shopping.redis.FlashSaleStockStore;
import com.e_commerce.kento_shopping.redis.RedisCircuitBreaker;
import com.e_commerce.kento_shopping.repository.FlashSaleRepository;
import com.e_commerce.kento_shopping.service.FlashSaleReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FlashSaleReconciliationServiceImpl implements FlashSaleReconciliationService {

    private static final Set<FlashSaleStatus> OPEN_STATUSES =
            EnumSet.of(FlashSaleStatus.ACTIVE, FlashSaleStatus.PAUSED, FlashSaleStatus.CLOSING);
    private static final String REDIS_UNAVAILABLE = "Redis unavailable — stock not checked";

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleStockStore stockStore;
    private final RedisCircuitBreaker breaker;

    @Override
    @Transactional(readOnly = true)
    public FlashSaleReconciliationResponse check(Long saleId) {
        FlashSale sale = flashSaleRepository.findById(saleId)
                .orElseThrow(() -> new FlashSaleNotFoundException("Flash sale not found"));
        return reconcile(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashSaleReconciliationResponse> checkOpenSales() {
        return flashSaleRepository.findByStatusInOrderByEndAtAsc(OPEN_STATUSES).stream()
                .map(this::reconcile)
                .toList();
    }

    private FlashSaleReconciliationResponse reconcile(FlashSale sale) {
        List<String> problems = new ArrayList<>();
        int allocated = sale.getAllocatedQty();
        int sold = sale.getSoldQty();

        long persisted = flashSaleRepository.sumPersistedUnits(sale.getId());
        if (persisted != sold) {
            problems.add("soldQty differs from units in persisted orders by " + (sold - persisted));
        }

        Integer stock = null;
        Integer inflight = null;
        if (OPEN_STATUSES.contains(sale.getStatus())) {
            if (breaker.isOpen()) {
                problems.add(REDIS_UNAVAILABLE);
            } else {
                try {
                    List<Integer> values = stockStore.stockAndInflight(sale.getId());
                    stock = values.get(0);
                    inflight = values.get(1) == null ? 0 : values.get(1);
                } catch (RuntimeException e) {
                    problems.add(REDIS_UNAVAILABLE);
                }
            }
        }

        if (inflight != null) {
            if (inflight < 0) {
                problems.add("In-flight units are negative");
            }
            if (sale.getStatus() == FlashSaleStatus.CLOSING) {
                if (stock != null) {
                    problems.add("Stock key still present while closing");
                }
                if (inflight + sold > allocated) {
                    problems.add("In-flight plus sold exceeds the allocation by " + (inflight + sold - allocated));
                }
            } else if (stock == null) {
                problems.add("Stock key missing");
            } else if (stock + inflight + sold != allocated) {
                problems.add("Redis stock + in-flight + sold is off from the allocation by "
                        + (stock + inflight + sold - allocated));
            }
        }

        return new FlashSaleReconciliationResponse(
                sale.getId(), sale.getStatus(), allocated, stock, inflight, sold, persisted,
                problems.isEmpty(), problems);
    }
}
