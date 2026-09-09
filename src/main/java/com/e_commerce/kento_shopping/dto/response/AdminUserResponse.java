package com.e_commerce.kento_shopping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private List<String> roles;
    private List<String> permissions;
}
