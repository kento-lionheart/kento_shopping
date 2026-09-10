package com.e_commerce.kento_shopping.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Replaces a user's entire role set. Idempotent, which matches how the admin UI
 * uses it: a checkbox list submitted whole rather than incremental grants.
 */
@Getter
@Setter
public class AssignRolesRequest {
    @NotNull(message = "roleNames is required")
    private Set<String> roleNames;
}
