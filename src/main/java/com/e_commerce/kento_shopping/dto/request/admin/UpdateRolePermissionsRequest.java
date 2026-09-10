package com.e_commerce.kento_shopping.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateRolePermissionsRequest {
    @NotNull(message = "permissions is required")
    private Set<String> permissions;
}
