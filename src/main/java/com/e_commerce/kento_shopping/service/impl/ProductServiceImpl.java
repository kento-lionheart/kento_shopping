package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.response.ProductResponse;
import com.e_commerce.kento_shopping.entity.Inventory;
import com.e_commerce.kento_shopping.entity.Product;
import com.e_commerce.kento_shopping.exception.ProductNotFoundException;
import com.e_commerce.kento_shopping.repository.ProductRepository;
import com.e_commerce.kento_shopping.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    @Override
    public Page<ProductResponse> getProducts(String search, Long categoryId, Pageable pageable) {
        List<Specification<Product>> specs = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            String keyword = search.trim().toLowerCase();
            specs.add((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), "%" + keyword + "%"),
                            cb.like(cb.lower(root.get("description")), "%" + keyword + "%")
                    )
            );
        }

        if (categoryId != null) {
            specs.add((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId)
            );
        }

        return productRepository.findAll(Specification.allOf(specs), pageable)
                .map(this::mapToResponse);
    }
    private ProductResponse mapToResponse(Product product){
        Inventory inventory = product.getInventory();
        int quantity = inventory != null ? inventory.getQuantity() : 0;
        String stockStatus = quantity > 0 ? "IN_STOCK" : "OUT_OF_STOCK";

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getCategory().getName(),
                quantity,
                stockStatus
        );
    }

    @Override
    public ProductResponse getProductById(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(()-> new ProductNotFoundException("Product not found"));
        return mapToResponse(product);
    }
}
