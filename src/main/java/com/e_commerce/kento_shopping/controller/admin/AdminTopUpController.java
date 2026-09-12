package com.e_commerce.kento_shopping.controller.admin;

import com.e_commerce.kento_shopping.dto.request.admin.RejectTopUpRequest;
import com.e_commerce.kento_shopping.dto.response.TopUpRequestResponse;
import com.e_commerce.kento_shopping.dto.response.WalletResponse;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.enums.TopUpStatus;
import com.e_commerce.kento_shopping.service.TopUpService;
import com.e_commerce.kento_shopping.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminTopUpController {
    private final TopUpService topUpService;
    private final WalletService walletService;

    @GetMapping("/top-ups")
    @PreAuthorize("hasAuthority('TOPUP_READ_ALL')")
    public ResponseEntity<Page<TopUpRequestResponse>> review(
            @RequestParam(required = false) TopUpStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return ResponseEntity.ok(topUpService.review(status, pageable));
    }

    @PutMapping("/top-ups/{id}/approve")
    @PreAuthorize("hasAuthority('TOPUP_APPROVE')")
    public ResponseEntity<TopUpRequestResponse> approve(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id){
        return ResponseEntity.ok(topUpService.approve(admin, id));
    }

    @PutMapping("/top-ups/{id}/reject")
    @PreAuthorize("hasAuthority('TOPUP_APPROVE')")
    public ResponseEntity<TopUpRequestResponse> reject(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @Valid @RequestBody RejectTopUpRequest request){
        return ResponseEntity.ok(topUpService.reject(admin, id, request));
    }

    @GetMapping("/wallets/{userId}")
    @PreAuthorize("hasAuthority('WALLET_READ_ALL')")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long userId){
        return ResponseEntity.ok(walletService.getWalletOf(userId));
    }
}
