package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.request.StockRequest;
import com.e_commerce.kento_shopping.dto.request.admin.ProductCreateRequest;
import com.e_commerce.kento_shopping.dto.request.admin.ProductRequest;
import com.e_commerce.kento_shopping.dto.response.ProductResponse;
import com.e_commerce.kento_shopping.entity.Category;
import com.e_commerce.kento_shopping.entity.Inventory;
import com.e_commerce.kento_shopping.entity.Product;
import com.e_commerce.kento_shopping.exception.CategoryNotFoundException;
import com.e_commerce.kento_shopping.exception.ProductNotFoundException;
import com.e_commerce.kento_shopping.repository.CategoryRepository;
import com.e_commerce.kento_shopping.repository.InventoryRepository;
import com.e_commerce.kento_shopping.repository.ProductRepository;
import com.e_commerce.kento_shopping.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
//        inventoryRepository.findByProduct(product)
//                .ifPresent(inventoryRepository::delete);
        productRepository.delete(product);
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

    @Override
    @Transactional
    public void createProduct(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        Product newProduct = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .category(category)
                .description(request.getDescription())
                .build();
        productRepository.save(newProduct);
        Inventory inventory = Inventory.builder()
                .product(newProduct)
                .quantity(request.getQuantity())
                .build();
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void updateProduct(Long id, ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        Product product = productRepository.findById(id)
                        .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        product.setName(request.getName());
        product.setCategory(category);
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setPrice(request.getPrice());
    }

    @Override
    public ProductResponse getProductById(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(()-> new ProductNotFoundException("Product not found"));
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public void updateStock(Long id, StockRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        Inventory inventory = inventoryRepository.findByProduct(product)
                .orElseThrow(() -> new ProductNotFoundException("Inventory not found the product"));
        inventory.setQuantity(request.getQuantity());
    }
}
