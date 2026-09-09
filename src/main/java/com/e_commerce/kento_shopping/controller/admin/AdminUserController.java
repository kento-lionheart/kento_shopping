package com.e_commerce.kento_shopping.controller.admin;

import com.e_commerce.kento_shopping.dto.request.admin.AssignRolesRequest;
import com.e_commerce.kento_shopping.dto.response.AdminUserResponse;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserAdminService userAdminService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<Page<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return ResponseEntity.ok(userAdminService.getUsers(email, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable Long id){
        return ResponseEntity.ok(userAdminService.getUser(id));
    }

    // ROLE_ASSIGN, not ROLE_MANAGE: this hands out roles that already exist. It
    // cannot invent one, so it cannot be used to grant a permission the actor
    // does not already have available in some role.
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    public ResponseEntity<AdminUserResponse> assignRoles(
            @AuthenticationPrincipal User actor,
            @PathVariable Long id,
            @Valid @RequestBody AssignRolesRequest request){
        return ResponseEntity.ok(userAdminService.assignRoles(actor, id, request));
    }
}
