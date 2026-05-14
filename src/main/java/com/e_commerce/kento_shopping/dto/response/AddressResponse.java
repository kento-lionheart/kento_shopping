package com.e_commerce.kento_shopping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class AddressResponse {
    private String recipientName;
    private String phone;
    private String street;
    private String ward;
    private String district;
    private String city;
    private String postalCode;
}
