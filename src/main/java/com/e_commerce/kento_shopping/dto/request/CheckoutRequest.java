package com.e_commerce.kento_shopping.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CheckoutRequest {
    @NotBlank
    private String recipientName;

    @NotBlank
    private String phone;

    @NotBlank
    private String street;

    @NotBlank
    private String ward;

    @NotBlank
    private String district;

    @NotBlank
    private String city;

    private String postalCode;
}

