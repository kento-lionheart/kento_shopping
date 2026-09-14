package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.response.PublicFlashSaleResponse;

import java.util.List;

public interface FlashSaleService {

    List<PublicFlashSaleResponse> getLive();

    PublicFlashSaleResponse getLive(Long id);
}
