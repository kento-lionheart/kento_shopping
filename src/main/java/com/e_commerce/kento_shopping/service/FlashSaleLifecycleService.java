package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.enums.FlashSaleStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface FlashSaleLifecycleService {

    List<Long> dueToActivate(LocalDateTime now);

    List<Long> dueToClose(LocalDateTime now);

    List<Long> idsIn(FlashSaleStatus status);

    void activate(Long saleId, boolean manual);

    void beginClose(Long saleId, boolean manual);

    void settle(Long saleId);

    void restoreStockKey(Long saleId);

    void pause(Long saleId);

    void resume(Long saleId);
}
