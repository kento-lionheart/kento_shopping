package com.e_commerce.kento_shopping.service;

import java.util.Optional;

public interface FlashSaleOrderService {

    Long persistClaim(String claimId, Long saleId, Long userId, int quantity);

    Optional<Long> findPersistedOrderId(String claimId);
}
