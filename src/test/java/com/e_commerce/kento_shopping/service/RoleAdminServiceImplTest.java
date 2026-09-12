package com.e_commerce.kento_shopping.service;

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
import com.e_commerce.kento_shopping.service.impl.RoleAdminServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAdminServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleAdminServiceImpl roleAdminService;

    @Captor
    private ArgumentCaptor<Role> roleCaptor;

    private Permission permission(Long id, PermissionName name) {
        Permission permission = Permission.builder()
                .name(name.name())
                .description(name.getDescription())
                .build();
        permission.setId(id);
        return permission;
    }

    private Role role(Long id, String name, Permission... permissions) {
        Role role = Role.builder()
                .name(name)
                .description(name + " role")
                .permissions(new HashSet<>(Arrays.asList(permissions)))
                .build();
        role.setId(id);
        return role;
    }

    private CreateRoleRequest createRoleRequest(String name, Set<String> permissions) {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName(name);
        request.setDescription("Handles the catalogue");
        request.setPermissions(permissions);
        return request;
    }

    private UpdateRolePermissionsRequest updateRequest(Set<String> permissions) {
        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest();
        request.setPermissions(permissions);
        return request;
    }

    @Test
    void getAllRolesMapsEachRoleWithItsSortedPermissionNames() {
        Role productStaff = role(2L, "PRODUCT_STAFF",
                permission(11L, PermissionName.PRODUCT_UPDATE),
                permission(10L, PermissionName.PRODUCT_CREATE));
        Role customer = role(1L, "CUSTOMER");
        when(roleRepository.findAllWithPermissions()).thenReturn(List.of(customer, productStaff));

        List<RoleResponse> responses = roleAdminService.getAllRoles();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(0).getName()).isEqualTo("CUSTOMER");
        assertThat(responses.get(0).getPermissions()).isEmpty();
        assertThat(responses.get(1).getName()).isEqualTo("PRODUCT_STAFF");
        assertThat(responses.get(1).getPermissions()).containsExactly("PRODUCT_CREATE", "PRODUCT_UPDATE");
    }

    @Test
    void getAllPermissionsReturnsTheWholeClosedCatalogueFromTheEnum() {
        List<PermissionResponse> responses = roleAdminService.getAllPermissions();

        assertThat(responses).hasSameSizeAs(PermissionName.values());
        assertThat(responses).extracting(PermissionResponse::getName)
                .contains(PermissionName.ROLE_MANAGE.name(), PermissionName.ROLE_ASSIGN.name());
        assertThat(responses).filteredOn(r -> r.getName().equals(PermissionName.ROLE_MANAGE.name()))
                .singleElement()
                .satisfies(r -> assertThat(r.getDescription())
                        .isEqualTo(PermissionName.ROLE_MANAGE.getDescription()));
        verifyNoInteractions(roleRepository, permissionRepository);
    }

    @Test
    void createRoleNormalisesTheNameAndPersistsTheRequestedPermissions() {
        Permission productCreate = permission(10L, PermissionName.PRODUCT_CREATE);
        when(roleRepository.existsByName("CONTENT_STAFF")).thenReturn(false);
        when(permissionRepository.findByName("PRODUCT_CREATE")).thenReturn(Optional.of(productCreate));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse response = roleAdminService.createRole(
                createRoleRequest("  content_staff  ", Set.of("PRODUCT_CREATE")));

        verify(roleRepository).save(roleCaptor.capture());
        Role saved = roleCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("CONTENT_STAFF");
        assertThat(saved.getDescription()).isEqualTo("Handles the catalogue");
        assertThat(saved.getPermissions()).containsExactly(productCreate);
        assertThat(response.getName()).isEqualTo("CONTENT_STAFF");
        assertThat(response.getPermissions()).containsExactly("PRODUCT_CREATE");
    }

    @Test
    void createRoleRejectsADuplicateRoleName() {
        when(roleRepository.existsByName("PRODUCT_STAFF")).thenReturn(true);

        assertThatThrownBy(() -> roleAdminService.createRole(
                createRoleRequest("product_staff", Set.of("PRODUCT_CREATE"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A role with this name already exists");

        verify(roleRepository, never()).save(any());
        verifyNoInteractions(permissionRepository);
    }

    @Test
    void createRoleRejectsAPermissionNameOutsideThePermissionNameEnum() {
        when(roleRepository.existsByName("CONTENT_STAFF")).thenReturn(false);

        assertThatThrownBy(() -> roleAdminService.createRole(
                createRoleRequest("content_staff", Set.of("PRODUCT_SUPER_DELETE"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown permission: PRODUCT_SUPER_DELETE");

        verify(roleRepository, never()).save(any());
        verify(permissionRepository, never()).findByName(anyString());
    }

    @Test
    void createRoleFailsWhenAnEnumPermissionIsMissingFromTheDatabase() {
        when(roleRepository.existsByName("CONTENT_STAFF")).thenReturn(false);
        when(permissionRepository.findByName("PRODUCT_CREATE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleAdminService.createRole(
                createRoleRequest("content_staff", Set.of("PRODUCT_CREATE"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is missing from the database");

        verify(roleRepository, never()).save(any());
    }

    @Test
    void updateRolePermissionsReplacesTheWholePermissionSet() {
        Permission productCreate = permission(10L, PermissionName.PRODUCT_CREATE);
        Permission inventoryUpdate = permission(13L, PermissionName.INVENTORY_UPDATE);
        Role productStaff = role(2L, "PRODUCT_STAFF", productCreate);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(productStaff));
        when(permissionRepository.findByName("INVENTORY_UPDATE")).thenReturn(Optional.of(inventoryUpdate));

        RoleResponse response = roleAdminService.updateRolePermissions(
                2L, updateRequest(Set.of("INVENTORY_UPDATE")));

        assertThat(productStaff.getPermissions()).containsExactly(inventoryUpdate);
        assertThat(response.getPermissions()).containsExactly("INVENTORY_UPDATE");
        verify(roleRepository, never()).save(any());
    }

    @Test
    void updateRolePermissionsThrowsWhenTheRoleDoesNotExist() {
        when(roleRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleAdminService.updateRolePermissions(
                404L, updateRequest(Set.of("PRODUCT_CREATE"))))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessage("Role not found");

        verifyNoInteractions(permissionRepository);
    }

    @Test
    void updateRolePermissionsRejectsAPermissionNameOutsideThePermissionNameEnum() {
        Role productStaff = role(2L, "PRODUCT_STAFF", permission(10L, PermissionName.PRODUCT_CREATE));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(productStaff));

        assertThatThrownBy(() -> roleAdminService.updateRolePermissions(
                2L, updateRequest(Set.of("PRODUCT_SUPER_DELETE"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown permission: PRODUCT_SUPER_DELETE");

        assertThat(productStaff.getPermissions()).hasSize(1);
        verify(permissionRepository, never()).findByName(anyString());
    }

    @Test
    void updateRolePermissionsRefusesToStripRoleManageFromTheOnlyRoleGrantingIt() {
        Permission roleManage = permission(20L, PermissionName.ROLE_MANAGE);
        Permission userRead = permission(21L, PermissionName.USER_READ);
        Role admin = role(1L, "ADMIN", roleManage, userRead);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(permissionRepository.findByName("USER_READ")).thenReturn(Optional.of(userRead));
        when(roleRepository.countRolesGrantingPermission("ROLE_MANAGE")).thenReturn(1L);

        assertThatThrownBy(() -> roleAdminService.updateRolePermissions(
                1L, updateRequest(Set.of("USER_READ"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot remove ROLE_MANAGE from the only role that grants it");

        assertThat(admin.getPermissions()).contains(roleManage);
    }

    @Test
    void updateRolePermissionsAllowsStrippingRoleManageWhenAnotherRoleStillGrantsIt() {
        Permission roleManage = permission(20L, PermissionName.ROLE_MANAGE);
        Permission userRead = permission(21L, PermissionName.USER_READ);
        Role admin = role(1L, "ADMIN", roleManage, userRead);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(permissionRepository.findByName("USER_READ")).thenReturn(Optional.of(userRead));
        when(roleRepository.countRolesGrantingPermission("ROLE_MANAGE")).thenReturn(2L);

        RoleResponse response = roleAdminService.updateRolePermissions(
                1L, updateRequest(Set.of("USER_READ")));

        assertThat(admin.getPermissions()).containsExactly(userRead);
        assertThat(response.getPermissions()).containsExactly("USER_READ");
    }

    @Test
    void updateRolePermissionsLeavesTheGuardIdleForARoleThatNeverGrantedRoleManage() {
        Permission productCreate = permission(10L, PermissionName.PRODUCT_CREATE);
        Permission inventoryUpdate = permission(13L, PermissionName.INVENTORY_UPDATE);
        Role productStaff = role(2L, "PRODUCT_STAFF", productCreate);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(productStaff));
        when(permissionRepository.findByName("INVENTORY_UPDATE")).thenReturn(Optional.of(inventoryUpdate));

        roleAdminService.updateRolePermissions(2L, updateRequest(Set.of("INVENTORY_UPDATE")));

        verify(roleRepository, never()).countRolesGrantingPermission(anyString());
    }
}
