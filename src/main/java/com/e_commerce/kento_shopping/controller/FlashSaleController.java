package com.e_commerce.kento_shopping.controller;

import com.e_commerce.kento_shopping.dto.response.PublicFlashSaleResponse;
import com.e_commerce.kento_shopping.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flash-sales")
@RequiredArgsConstructor
public class FlashSaleController {
    private final FlashSaleService flashSaleService;

    @GetMapping
    public ResponseEntity<List<PublicFlashSaleResponse>> getLive(){
        return ResponseEntity.ok(flashSaleService.getLive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicFlashSaleResponse> getLive(@PathVariable Long id){
        return ResponseEntity.ok(flashSaleService.getLive(id));
    }
}
