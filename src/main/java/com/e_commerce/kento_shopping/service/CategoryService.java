package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories();
}
