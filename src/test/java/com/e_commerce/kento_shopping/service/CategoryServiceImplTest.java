package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.response.CategoryResponse;
import com.e_commerce.kento_shopping.entity.Category;
import com.e_commerce.kento_shopping.repository.CategoryRepository;
import com.e_commerce.kento_shopping.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void getAllCategoriesMapsEveryCategory() {
        when(categoryRepository.findAll())
                .thenReturn(List.of(newCategory(1L, "Electronics"), newCategory(2L, "Fashion")));

        List<CategoryResponse> responses = categoryService.getAllCategories();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CategoryResponse::getId).containsExactly(1L, 2L);
        assertThat(responses).extracting(CategoryResponse::getName)
                .containsExactly("Electronics", "Fashion");
    }

    @Test
    void getAllCategoriesReturnsEmptyListWhenNoneExist() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        assertThat(categoryService.getAllCategories()).isEmpty();
    }

    private Category newCategory(Long id, String name) {
        Category category = Category.builder().name(name).build();
        category.setId(id);
        return category;
    }
}
