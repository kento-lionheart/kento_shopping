package com.e_commerce.kento_shopping.service.impl;

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
import com.e_commerce.kento_shopping.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private AdminUserResponse mapToResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .sorted()
                .toList();
        return new AdminUserResponse(
                user.getId(), user.getEmail(), user.getFullName(),
                user.getPhoneNumber(), roles, permissions);
    }

    private boolean grantsRoleManage(Set<Role> roles) {
        return roles.stream()
                .flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> p.getName().equals(PermissionName.ROLE_MANAGE.name()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(String email, Pageable pageable) {
        // Mapping happens inside the transaction, so the lazy role/permission
        // traversal resolves. This is N+1 by design: an admin user list is small
        // and paged, and a join fetch here would force in-memory pagination.
        Page<User> users = (email != null && !email.isBlank())
                ? userRepository.findByEmailContainingIgnoreCase(email, pageable)
                : userRepository.findAll(pageable);
        return users.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long userId) {
        return mapToResponse(userRepository.findByIdWithAuthorities(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found")));
    }

    @Override
    @Transactional
    public AdminUserResponse assignRoles(User actor, Long userId, AssignRolesRequest request) {
        // Nobody edits their own roles. Prevents an admin locking themselves out,
        // and removes the whole class of self-escalation arguments.
        if (actor.getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot change your own roles");
        }

        User target = userRepository.findByIdWithAuthorities(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Set<Role> newRoles = new HashSet<>();
        for (String name : request.getRoleNames()) {
            newRoles.add(roleRepository.findByNameWithPermissions(name)
                    .orElseThrow(() -> new RoleNotFoundException("Role not found: " + name)));
        }

        // Demoting the last account that can administer roles would leave the
        // system unadministrable with no recovery path short of raw SQL.
        if (grantsRoleManage(target.getRoles()) && !grantsRoleManage(newRoles)
                && userRepository.countByPermission(PermissionName.ROLE_MANAGE.name()) <= 1) {
            throw new IllegalArgumentException(
                    "Cannot demote the last account holding ROLE_MANAGE");
        }

        // An empty set is allowed: the user keeps a valid login but loses the
        // customer endpoints, which is an effective soft deactivation.
        target.getRoles().clear();
        target.getRoles().addAll(newRoles);
        return mapToResponse(target);
    }
}
