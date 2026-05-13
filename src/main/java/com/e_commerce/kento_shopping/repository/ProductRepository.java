package com.e_commerce.kento_shopping.repository;

import com.e_commerce.kento_shopping.entity.Category;
import com.e_commerce.kento_shopping.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Page<Product> findByCategory(Category category, Pageable pageable);
    boolean existsByName(String name);
}
