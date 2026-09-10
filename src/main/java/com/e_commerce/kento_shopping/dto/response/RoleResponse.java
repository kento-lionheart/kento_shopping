package com.e_commerce.kento_shopping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RoleResponse {
    private Long id;
    private String name;
    private String description;
    private List<String> permissions;
}
