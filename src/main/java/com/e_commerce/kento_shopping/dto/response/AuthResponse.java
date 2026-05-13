package com.e_commerce.kento_shopping.dto.response;

import com.e_commerce.kento_shopping.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Role role;
}
