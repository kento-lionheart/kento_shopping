package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.StockRequest;
import com.e_commerce.kento_shopping.dto.request.admin.ProductCreateRequest;
import com.e_commerce.kento_shopping.dto.request.admin.ProductRequest;
import com.e_commerce.kento_shopping.dto.response.ProductResponse;
import com.e_commerce.kento_shopping.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    Page<ProductResponse> getProducts(String search, Long categoryId, Pageable pageable);
    ProductResponse getProductById(Long id);
    void createProduct(ProductCreateRequest request);
    void updateProduct(Long id, ProductRequest request);
    void updateStock(Long id, StockRequest request);
    void deleteProduct(Long id);
}
