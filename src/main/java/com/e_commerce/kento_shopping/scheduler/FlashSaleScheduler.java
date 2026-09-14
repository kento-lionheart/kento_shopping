package com.e_commerce.kento_shopping.scheduler;

import com.e_commerce.kento_shopping.enums.FlashSaleStatus;
import com.e_commerce.kento_shopping.service.FlashSaleLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleScheduler {

    private final FlashSaleLifecycleService lifecycle;

    @Scheduled(fixedDelay = 1000)
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        forEach(lifecycle.dueToActivate(now), id -> lifecycle.activate(id, false), "activation");
        forEach(lifecycle.dueToClose(now), id -> lifecycle.beginClose(id, false), "close");
        forEach(lifecycle.idsIn(FlashSaleStatus.ACTIVE), lifecycle::restoreStockKey, "stock key restore");
        forEach(lifecycle.idsIn(FlashSaleStatus.CLOSING), lifecycle::settle, "settlement");
    }

    private void forEach(List<Long> saleIds, Consumer<Long> action, String step) {
        for (Long id : saleIds) {
            try {
                action.accept(id);
            } catch (Exception e) {
                log.warn("Flash sale {} {} failed: {}", id, step, e.getMessage());
            }
        }
    }
}
