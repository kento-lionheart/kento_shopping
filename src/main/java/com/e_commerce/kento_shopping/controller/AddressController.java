package com.e_commerce.kento_shopping.controller;

import com.e_commerce.kento_shopping.dto.request.AddressRequest;
import com.e_commerce.kento_shopping.dto.response.AddressResponse;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/address")
public class AddressController {
    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<AddressResponse> getAddress(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(addressService.getAddress(user));
    }

    @PostMapping
    public ResponseEntity<Void> createAddress(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddressRequest request){
        addressService.createAddress(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping
    public ResponseEntity<Void> updateAddress(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddressRequest request){
        addressService.updateAddress(user,request);
        return ResponseEntity.ok().build();
    }
}
