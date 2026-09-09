package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.request.LoginRequest;
import com.e_commerce.kento_shopping.dto.request.RegisterRequest;
import com.e_commerce.kento_shopping.dto.response.AuthResponse;
import com.e_commerce.kento_shopping.dto.response.UserSummaryResponse;
import com.e_commerce.kento_shopping.entity.Permission;
import com.e_commerce.kento_shopping.entity.Role;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.exception.EmailAlreadyExistsException;
import com.e_commerce.kento_shopping.exception.InvalidCredentialsException;
import com.e_commerce.kento_shopping.repository.RoleRepository;
import com.e_commerce.kento_shopping.repository.UserRepository;
import com.e_commerce.kento_shopping.service.AuthService;
import com.e_commerce.kento_shopping.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    public static final String DEFAULT_ROLE = "CUSTOMER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public void register(RegisterRequest request){
        if(!request.getPassword().equals(request.getConfirmPassword())){
            throw new IllegalArgumentException("Passwords don't match !!");
        }
        if(userRepository.existsByEmail(request.getEmail())){
            throw new EmailAlreadyExistsException("This email is already in use honey");
        }
        Role customerRole = roleRepository.findByNameWithPermissions(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + DEFAULT_ROLE + " is missing — the database has not been seeded"));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of(customerRole))
                .build();
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request){
        // Authorities must be join-fetched here: the response reads roles and
        // permissions after the transaction would otherwise have closed.
        User user = userRepository.findByEmailWithAuthorities(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new InvalidCredentialsException("Invalid email or password");
        }
        return buildAuthResponse(user, jwtUtil.generateToken(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse me(User user) {
        // Re-read rather than trusting the principal: an admin may have changed
        // this user's roles since the token was issued.
        User fresh = userRepository.findByIdWithAuthorities(user.getId())
                .orElseThrow(() -> new InvalidCredentialsException("User no longer exists"));
        return buildAuthResponse(fresh, null);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
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

        return new AuthResponse(
                token,
                new UserSummaryResponse(user.getId(), user.getEmail(), user.getFullName()),
                roles,
                permissions
        );
    }
}
