package com.e_commerce.kento_shopping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserSummaryResponse {
    private Long id;
    private String email;
    private String fullName;
}
