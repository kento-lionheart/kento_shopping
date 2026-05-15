package com.e_commerce.kento_shopping.controller;

import com.e_commerce.kento_shopping.dto.response.ProductResponse;
import com.e_commerce.kento_shopping.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Sort sortOrder = switch(sort){
            case "price_asc"  -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            default           -> Sort.by("createdAt").descending(); // newest
        };
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size , 1);
        Pageable pageable = PageRequest.of(safePage, safeSize, sortOrder);
        return ResponseEntity.ok(productService.getProducts(search, categoryId, pageable));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id){
        return ResponseEntity.ok(productService.getProductById(id));
    }
}
