package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.admin.CreateRoleRequest;
import com.e_commerce.kento_shopping.dto.request.admin.UpdateRolePermissionsRequest;
import com.e_commerce.kento_shopping.dto.response.PermissionResponse;
import com.e_commerce.kento_shopping.dto.response.RoleResponse;

import java.util.List;

public interface RoleAdminService {
    List<RoleResponse> getAllRoles();

    /** The catalogue is read-only: it is defined by PermissionName in source. */
    List<PermissionResponse> getAllPermissions();

    RoleResponse createRole(CreateRoleRequest request);

    RoleResponse updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request);
}
