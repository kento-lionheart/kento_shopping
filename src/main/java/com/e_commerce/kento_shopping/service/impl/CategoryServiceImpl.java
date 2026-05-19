package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.response.CategoryResponse;
import com.e_commerce.kento_shopping.entity.Category;
import com.e_commerce.kento_shopping.repository.CategoryRepository;
import com.e_commerce.kento_shopping.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    private CategoryResponse mapToResponse(Category category){
        return new CategoryResponse(
                category.getName()
        );
    }
    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}
