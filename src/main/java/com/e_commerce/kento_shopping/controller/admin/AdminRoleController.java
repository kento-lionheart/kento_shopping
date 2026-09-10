package com.e_commerce.kento_shopping.controller.admin;

import com.e_commerce.kento_shopping.dto.request.admin.CreateRoleRequest;
import com.e_commerce.kento_shopping.dto.request.admin.UpdateRolePermissionsRequest;
import com.e_commerce.kento_shopping.dto.response.PermissionResponse;
import com.e_commerce.kento_shopping.dto.response.RoleResponse;
import com.e_commerce.kento_shopping.service.RoleAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminRoleController {
    private final RoleAdminService roleAdminService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    public ResponseEntity<List<RoleResponse>> getRoles(){
        return ResponseEntity.ok(roleAdminService.getAllRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    public ResponseEntity<List<PermissionResponse>> getPermissions(){
        return ResponseEntity.ok(roleAdminService.getAllPermissions());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleAdminService.createRole(request));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<RoleResponse> updateRolePermissions(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRolePermissionsRequest request){
        return ResponseEntity.ok(roleAdminService.updateRolePermissions(id, request));
    }
}
