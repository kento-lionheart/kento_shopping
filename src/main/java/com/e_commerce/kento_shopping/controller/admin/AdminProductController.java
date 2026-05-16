package com.e_commerce.kento_shopping.controller.admin;

import com.e_commerce.kento_shopping.dto.request.StockRequest;
import com.e_commerce.kento_shopping.dto.request.admin.ProductCreateRequest;
import com.e_commerce.kento_shopping.dto.request.admin.ProductRequest;
import com.e_commerce.kento_shopping.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Void> createProduct(@Valid @RequestBody ProductCreateRequest request){
        productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request){
        productService.updateProduct(id, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<Void> updateStock(@PathVariable Long id, @Valid @RequestBody StockRequest request){
        productService.updateStock(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id){
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
