package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.admin.AssignRolesRequest;
import com.e_commerce.kento_shopping.dto.response.AdminUserResponse;
import com.e_commerce.kento_shopping.entity.Permission;
import com.e_commerce.kento_shopping.entity.Role;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.enums.PermissionName;
import com.e_commerce.kento_shopping.exception.RoleNotFoundException;
import com.e_commerce.kento_shopping.exception.UserNotFoundException;
import com.e_commerce.kento_shopping.repository.RoleRepository;
import com.e_commerce.kento_shopping.repository.UserRepository;
import com.e_commerce.kento_shopping.service.impl.UserAdminServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceImplTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserAdminServiceImpl userAdminService;

    private Permission permission(Long id, PermissionName name) {
        Permission permission = Permission.builder()
                .name(name.name())
                .description(name.getDescription())
                .build();
        permission.setId(id);
        return permission;
    }

    private Role role(Long id, String name, PermissionName... permissionNames) {
        Set<Permission> permissions = new HashSet<>();
        long permissionId = 100L;
        for (PermissionName permissionName : permissionNames) {
            permissions.add(permission(permissionId++, permissionName));
        }
        Role role = Role.builder().name(name).description(name + " role").permissions(permissions).build();
        role.setId(id);
        return role;
    }

    private Role adminRole() {
        return role(1L, "ADMIN", PermissionName.ROLE_MANAGE, PermissionName.USER_READ);
    }

    private Role customerRole() {
        return role(2L, "CUSTOMER");
    }

    private Role orderStaffRole() {
        return role(3L, "ORDER_STAFF", PermissionName.ORDER_READ_ALL, PermissionName.ORDER_UPDATE_STATUS);
    }

    private User user(Long id, String email, Role... roles) {
        User user = User.builder()
                .email(email)
                .password("$2a$10$encodedhashvalue")
                .fullName("Nguyen Van An")
                .phoneNumber("0912345678")
                .roles(new HashSet<>(Arrays.asList(roles)))
                .build();
        user.setId(id);
        return user;
    }

    private AssignRolesRequest assignRolesRequest(Set<String> roleNames) {
        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleNames(roleNames);
        return request;
    }

    @Test
    void getUsersFiltersByEmailWhenOneIsGiven() {
        Page<User> page = new PageImpl<>(List.of(user(5L, "nguyen.van.an@gmail.com", customerRole())),
                PAGEABLE, 1);
        when(userRepository.findByEmailContainingIgnoreCase("nguyen", PAGEABLE)).thenReturn(page);

        Page<AdminUserResponse> result = userAdminService.getUsers("nguyen", PAGEABLE);

        assertThat(result.getContent()).singleElement()
                .satisfies(response -> {
                    assertThat(response.getId()).isEqualTo(5L);
                    assertThat(response.getEmail()).isEqualTo("nguyen.van.an@gmail.com");
                    assertThat(response.getFullName()).isEqualTo("Nguyen Van An");
                    assertThat(response.getPhoneNumber()).isEqualTo("0912345678");
                    assertThat(response.getRoles()).containsExactly("CUSTOMER");
                    assertThat(response.getPermissions()).isEmpty();
                });
        verify(userRepository, never()).findAll(PAGEABLE);
    }

    @Test
    void getUsersReturnsEveryUserWhenTheEmailFilterIsNull() {
        Page<User> page = new PageImpl<>(List.of(
                user(1L, "admin@kento.com", adminRole()),
                user(5L, "nguyen.van.an@gmail.com", customerRole(), orderStaffRole())),
                PAGEABLE, 2);
        when(userRepository.findAll(PAGEABLE)).thenReturn(page);

        Page<AdminUserResponse> result = userAdminService.getUsers(null, PAGEABLE);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getRoles()).containsExactly("ADMIN");
        assertThat(result.getContent().get(0).getPermissions())
                .containsExactly("ROLE_MANAGE", "USER_READ");
        assertThat(result.getContent().get(1).getRoles()).containsExactly("CUSTOMER", "ORDER_STAFF");
        assertThat(result.getContent().get(1).getPermissions())
                .containsExactly("ORDER_READ_ALL", "ORDER_UPDATE_STATUS");
    }

    @Test
    void getUsersReturnsEveryUserWhenTheEmailFilterIsBlank() {
        Page<User> page = new PageImpl<>(List.of(user(1L, "admin@kento.com", adminRole())), PAGEABLE, 1);
        when(userRepository.findAll(PAGEABLE)).thenReturn(page);

        Page<AdminUserResponse> result = userAdminService.getUsers("   ", PAGEABLE);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getUserReturnsTheMappedUser() {
        when(userRepository.findByIdWithAuthorities(1L))
                .thenReturn(Optional.of(user(1L, "admin@kento.com", adminRole())));

        AdminUserResponse response = userAdminService.getUser(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("admin@kento.com");
        assertThat(response.getRoles()).containsExactly("ADMIN");
        assertThat(response.getPermissions()).containsExactly("ROLE_MANAGE", "USER_READ");
    }

    @Test
    void getUserThrowsWhenTheUserDoesNotExist() {
        when(userRepository.findByIdWithAuthorities(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.getUser(404L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void assignRolesReplacesTheTargetsWholeRoleSet() {
        User actor = user(1L, "admin@kento.com", adminRole());
        User target = user(5L, "nguyen.van.an@gmail.com", customerRole());
        Role orderStaff = orderStaffRole();
        when(userRepository.findByIdWithAuthorities(5L)).thenReturn(Optional.of(target));
        when(roleRepository.findByNameWithPermissions("ORDER_STAFF")).thenReturn(Optional.of(orderStaff));

        AdminUserResponse response = userAdminService.assignRoles(actor, 5L, assignRolesRequest(Set.of("ORDER_STAFF")));

        assertThat(target.getRoles()).containsExactly(orderStaff);
        assertThat(response.getRoles()).containsExactly("ORDER_STAFF");
        assertThat(response.getPermissions()).containsExactly("ORDER_READ_ALL", "ORDER_UPDATE_STATUS");
        verify(userRepository, never()).save(target);
    }

    @Test
    void assignRolesAcceptsAnEmptyRoleSetAsASoftDeactivation() {
        User actor = user(1L, "admin@kento.com", adminRole());
        User target = user(5L, "nguyen.van.an@gmail.com", customerRole());
        when(userRepository.findByIdWithAuthorities(5L)).thenReturn(Optional.of(target));

        AdminUserResponse response = userAdminService.assignRoles(actor, 5L, assignRolesRequest(Set.of()));

        assertThat(target.getRoles()).isEmpty();
        assertThat(response.getRoles()).isEmpty();
        assertThat(response.getPermissions()).isEmpty();
        verifyNoInteractions(roleRepository);
    }

    @Test
    void assignRolesRefusesToChangeTheActorsOwnRoles() {
        User actor = user(1L, "admin@kento.com", adminRole());

        assertThatThrownBy(() -> userAdminService.assignRoles(actor, 1L, assignRolesRequest(Set.of("CUSTOMER"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You cannot change your own roles");

        verifyNoInteractions(userRepository, roleRepository);
    }

    @Test
    void assignRolesRefusesToDemoteTheLastAccountHoldingRoleManage() {
        User actor = user(1L, "root@kento.com", adminRole());
        Role admin = adminRole();
        User target = user(2L, "admin@kento.com", admin);
        Role customer = customerRole();
        when(userRepository.findByIdWithAuthorities(2L)).thenReturn(Optional.of(target));
        when(roleRepository.findByNameWithPermissions("CUSTOMER")).thenReturn(Optional.of(customer));
        when(userRepository.countByPermission("ROLE_MANAGE")).thenReturn(1L);

        assertThatThrownBy(() -> userAdminService.assignRoles(actor, 2L, assignRolesRequest(Set.of("CUSTOMER"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot demote the last account holding ROLE_MANAGE");

        assertThat(target.getRoles()).containsExactly(admin);
    }

    @Test
    void assignRolesAllowsDemotionWhenAnotherRoleManageHolderRemains() {
        User actor = user(1L, "root@kento.com", adminRole());
        User target = user(2L, "admin@kento.com", adminRole());
        Role customer = customerRole();
        when(userRepository.findByIdWithAuthorities(2L)).thenReturn(Optional.of(target));
        when(roleRepository.findByNameWithPermissions("CUSTOMER")).thenReturn(Optional.of(customer));
        when(userRepository.countByPermission("ROLE_MANAGE")).thenReturn(2L);

        AdminUserResponse response = userAdminService.assignRoles(actor, 2L, assignRolesRequest(Set.of("CUSTOMER")));

        assertThat(target.getRoles()).containsExactly(customer);
        assertThat(response.getRoles()).containsExactly("CUSTOMER");
    }

    @Test
    void assignRolesLeavesTheLastHolderGuardIdleWhenTheNewRolesStillGrantRoleManage() {
        User actor = user(1L, "root@kento.com", adminRole());
        User target = user(2L, "admin@kento.com", adminRole());
        Role admin = adminRole();
        when(userRepository.findByIdWithAuthorities(2L)).thenReturn(Optional.of(target));
        when(roleRepository.findByNameWithPermissions("ADMIN")).thenReturn(Optional.of(admin));

        userAdminService.assignRoles(actor, 2L, assignRolesRequest(Set.of("ADMIN")));

        verify(userRepository, never()).countByPermission(anyString());
    }

    @Test
    void assignRolesThrowsWhenARequestedRoleNameDoesNotExist() {
        User actor = user(1L, "admin@kento.com", adminRole());
        User target = user(5L, "nguyen.van.an@gmail.com", customerRole());
        when(userRepository.findByIdWithAuthorities(5L)).thenReturn(Optional.of(target));
        when(roleRepository.findByNameWithPermissions("SUPER_ADMIN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.assignRoles(actor, 5L, assignRolesRequest(Set.of("SUPER_ADMIN"))))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessage("Role not found: SUPER_ADMIN");

        assertThat(target.getRoles()).containsExactly(customerRole());
    }

    @Test
    void assignRolesThrowsWhenTheTargetUserDoesNotExist() {
        User actor = user(1L, "admin@kento.com", adminRole());
        when(userRepository.findByIdWithAuthorities(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.assignRoles(actor, 404L, assignRolesRequest(Set.of("CUSTOMER"))))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verifyNoInteractions(roleRepository);
    }
}
