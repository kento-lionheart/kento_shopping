package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.admin.AssignRolesRequest;
import com.e_commerce.kento_shopping.dto.response.AdminUserResponse;
import com.e_commerce.kento_shopping.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserAdminService {
    Page<AdminUserResponse> getUsers(String email, Pageable pageable);

    AdminUserResponse getUser(Long userId);

    AdminUserResponse assignRoles(User actor, Long userId, AssignRolesRequest request);
}
