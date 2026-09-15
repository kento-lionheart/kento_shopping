package com.e_commerce.kento_shopping.controller;

import com.e_commerce.kento_shopping.dto.request.FlashSalePurchaseRequest;
import com.e_commerce.kento_shopping.dto.response.FlashSaleClaimResponse;
import com.e_commerce.kento_shopping.dto.response.PublicFlashSaleResponse;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.service.FlashSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{id}/purchase")
    public ResponseEntity<FlashSaleClaimResponse> purchase(@AuthenticationPrincipal User user,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody FlashSalePurchaseRequest request){
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(flashSaleService.purchase(user, id, request));
    }

    @GetMapping("/claims/{claimId}")
    public ResponseEntity<FlashSaleClaimResponse> getClaim(@AuthenticationPrincipal User user,
                                                           @PathVariable String claimId){
        return ResponseEntity.ok(flashSaleService.getClaim(user, claimId));
    }
}
