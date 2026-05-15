package com.e_commerce.kento_shopping.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {
    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message =  "Ward is required")
    private String ward;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "City is required")
    private String city;

    private String postalCode;
}
