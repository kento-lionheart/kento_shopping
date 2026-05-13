package com.e_commerce.kento_shopping.repository;

import com.e_commerce.kento_shopping.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);
}
