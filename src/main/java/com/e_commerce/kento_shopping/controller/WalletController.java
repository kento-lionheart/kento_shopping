package com.e_commerce.kento_shopping.controller;

import com.e_commerce.kento_shopping.dto.request.CreateTopUpRequest;
import com.e_commerce.kento_shopping.dto.response.CoinTransactionResponse;
import com.e_commerce.kento_shopping.dto.response.TopUpRequestResponse;
import com.e_commerce.kento_shopping.dto.response.WalletResponse;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.service.TopUpService;
import com.e_commerce.kento_shopping.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;
    private final TopUpService topUpService;

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(walletService.getWallet(user));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<CoinTransactionResponse>> getTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return ResponseEntity.ok(walletService.getTransactions(user, pageable));
    }

    @PostMapping("/top-ups")
    public ResponseEntity<TopUpRequestResponse> requestTopUp(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateTopUpRequest request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(topUpService.request(user, request));
    }

    @GetMapping("/top-ups")
    public ResponseEntity<List<TopUpRequestResponse>> myTopUps(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(topUpService.myRequests(user));
    }

    @DeleteMapping("/top-ups/{id}")
    public ResponseEntity<TopUpRequestResponse> cancelTopUp(
            @AuthenticationPrincipal User user,
            @PathVariable Long id){
        return ResponseEntity.ok(topUpService.cancel(user, id));
    }
}
