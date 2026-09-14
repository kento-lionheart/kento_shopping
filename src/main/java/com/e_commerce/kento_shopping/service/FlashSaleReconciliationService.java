package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.response.FlashSaleReconciliationResponse;

import java.util.List;

public interface FlashSaleReconciliationService {

    FlashSaleReconciliationResponse check(Long saleId);

    List<FlashSaleReconciliationResponse> checkOpenSales();
}
