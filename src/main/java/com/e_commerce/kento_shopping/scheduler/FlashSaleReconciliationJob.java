package com.e_commerce.kento_shopping.scheduler;

import com.e_commerce.kento_shopping.dto.response.FlashSaleReconciliationResponse;
import com.e_commerce.kento_shopping.service.FlashSaleReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleReconciliationJob {

    private final FlashSaleReconciliationService reconciliation;

    private Map<Long, List<String>> previousProblems = new HashMap<>();

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void run() {
        Map<Long, List<String>> currentProblems = new HashMap<>();
        try {
            for (FlashSaleReconciliationResponse report : reconciliation.checkOpenSales()) {
                if (report.isConsistent()) {
                    continue;
                }
                currentProblems.put(report.getSaleId(), report.getProblems());
                if (report.getProblems().equals(previousProblems.get(report.getSaleId()))) {
                    log.error("Flash sale {} failed reconciliation on two consecutive runs: {} "
                                    + "(allocated={}, redisStock={}, inflight={}, sold={}, persisted={})",
                            report.getSaleId(), report.getProblems(), report.getAllocatedQty(),
                            report.getRedisStock(), report.getRedisInflight(), report.getSoldQty(),
                            report.getPersistedUnits());
                }
            }
        } catch (Exception e) {
            log.warn("Flash-sale reconciliation run failed: {}", e.getMessage());
        }
        previousProblems = currentProblems;
    }
}
