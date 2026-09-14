package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.FlashSalePurchaseRequest;
import com.e_commerce.kento_shopping.dto.response.FlashSaleClaimResponse;
import com.e_commerce.kento_shopping.dto.response.PublicFlashSaleResponse;
import com.e_commerce.kento_shopping.entity.User;

import java.util.List;

public interface FlashSaleService {

    List<PublicFlashSaleResponse> getLive();

    PublicFlashSaleResponse getLive(Long id);

    FlashSaleClaimResponse purchase(User user, Long saleId, FlashSalePurchaseRequest request);

    FlashSaleClaimResponse getClaim(User user, String claimId);
}
