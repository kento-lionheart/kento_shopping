package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.admin.FlashSaleRequest;
import com.e_commerce.kento_shopping.dto.response.FlashSaleResponse;
import com.e_commerce.kento_shopping.enums.FlashSaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FlashSaleAdminService {

    Page<FlashSaleResponse> getAll(FlashSaleStatus status, Pageable pageable);

    FlashSaleResponse getById(Long id);

    FlashSaleResponse create(FlashSaleRequest request);

    FlashSaleResponse update(Long id, FlashSaleRequest request);

    FlashSaleResponse cancel(Long id);

    void delete(Long id);
}
