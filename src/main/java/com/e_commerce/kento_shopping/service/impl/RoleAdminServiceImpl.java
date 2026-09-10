package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.request.admin.CreateRoleRequest;
import com.e_commerce.kento_shopping.dto.request.admin.UpdateRolePermissionsRequest;
import com.e_commerce.kento_shopping.dto.response.PermissionResponse;
import com.e_commerce.kento_shopping.dto.response.RoleResponse;
import com.e_commerce.kento_shopping.entity.Permission;
import com.e_commerce.kento_shopping.entity.Role;
import com.e_commerce.kento_shopping.enums.PermissionName;
import com.e_commerce.kento_shopping.exception.RoleNotFoundException;
import com.e_commerce.kento_shopping.repository.PermissionRepository;
import com.e_commerce.kento_shopping.repository.RoleRepository;
import com.e_commerce.kento_shopping.service.RoleAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleAdminServiceImpl implements RoleAdminService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    private RoleResponse mapToRoleResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream()
                        .map(Permission::getName)
                        .sorted()
                        .toList()
        );
    }

    /**
     * Permission names are a closed set: a permission only has meaning because a
     * @PreAuthorize check refers to it, so a name outside the enum could never be
     * satisfied and is rejected rather than silently stored.
     */
    private Set<Permission> resolvePermissions(Set<String> names) {
        Set<Permission> resolved = new HashSet<>();
        for (String name : names) {
            try {
                PermissionName.valueOf(name);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown permission: " + name);
            }
            resolved.add(permissionRepository.findByName(name)
                    .orElseThrow(() -> new IllegalStateException(
                            "Permission " + name + " is missing from the database")));
        }
        return resolved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAllWithPermissions().stream()
                .map(this::mapToRoleResponse)
                .toList();
    }

    @Override
    public List<PermissionResponse> getAllPermissions() {
        return java.util.Arrays.stream(PermissionName.values())
                .map(p -> new PermissionResponse(p.name(), p.getDescription()))
                .toList();
    }

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        String name = request.getName().trim().toUpperCase();
        if (roleRepository.existsByName(name)) {
            throw new IllegalArgumentException("A role with this name already exists");
        }
        Role role = Role.builder()
                .name(name)
                .description(request.getDescription())
                .permissions(resolvePermissions(request.getPermissions()))
                .build();
        return mapToRoleResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));

        Set<Permission> updated = resolvePermissions(request.getPermissions());

        // Stripping ROLE_MANAGE from the only role that grants it would leave the
        // system unadministrable, with no recovery path short of raw SQL.
        boolean removesRoleManage =
                role.getPermissions().stream().anyMatch(p -> p.getName().equals(PermissionName.ROLE_MANAGE.name()))
                        && updated.stream().noneMatch(p -> p.getName().equals(PermissionName.ROLE_MANAGE.name()));
        if (removesRoleManage && roleRepository.countRolesGrantingPermission(PermissionName.ROLE_MANAGE.name()) <= 1) {
            throw new IllegalArgumentException(
                    "Cannot remove ROLE_MANAGE from the only role that grants it");
        }

        role.getPermissions().clear();
        role.getPermissions().addAll(updated);
        return mapToRoleResponse(role);
    }
}
