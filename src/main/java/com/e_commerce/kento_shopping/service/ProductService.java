package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface ProductService {
    Page<ProductResponse> getProducts(String search, Long categoryId, Pageable pageable);
    ProductResponse getProductById(Long id);
}
