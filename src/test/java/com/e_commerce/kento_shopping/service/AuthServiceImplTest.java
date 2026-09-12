package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.LoginRequest;
import com.e_commerce.kento_shopping.dto.request.RegisterRequest;
import com.e_commerce.kento_shopping.dto.response.AuthResponse;
import com.e_commerce.kento_shopping.entity.Permission;
import com.e_commerce.kento_shopping.entity.Role;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.exception.EmailAlreadyExistsException;
import com.e_commerce.kento_shopping.exception.InvalidCredentialsException;
import com.e_commerce.kento_shopping.repository.RoleRepository;
import com.e_commerce.kento_shopping.repository.UserRepository;
import com.e_commerce.kento_shopping.service.impl.AuthServiceImpl;
import com.e_commerce.kento_shopping.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.HashSet;
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
class AuthServiceImplTest {

    private static final String RAW_PASSWORD = "Kiet123456";
    private static final String HASHED_PASSWORD = "$2a$10$encodedhashvalue";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private Permission permission(String name) {
        Permission permission = Permission.builder().name(name).description(name).build();
        permission.setId((long) Math.abs(name.hashCode() % 1000));
        return permission;
    }

    private Role role(Long id, String name, String... permissionNames) {
        Set<Permission> permissions = new HashSet<>();
        for (String permissionName : permissionNames) {
            permissions.add(permission(permissionName));
        }
        Role role = Role.builder().name(name).description(name).permissions(permissions).build();
        role.setId(id);
        return role;
    }

    private User user(Long id, String email, Role... roles) {
        User user = User.builder()
                .email(email)
                .password(HASHED_PASSWORD)
                .fullName("Tran Danh Kiet")
                .phoneNumber("0912345678")
                .roles(new HashSet<>(Arrays.asList(roles)))
                .build();
        user.setId(id);
        return user;
    }

    private RegisterRequest registerRequest(String password, String confirmPassword) {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Tran Danh Kiet");
        request.setEmail("kiet@kento.com");
        request.setPhoneNumber("0912345678");
        request.setPassword(password);
        request.setConfirmPassword(confirmPassword);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    @Test
    void registerStoresTheHashedPasswordAndNeverTheRawOne() {
        Role customer = role(1L, "CUSTOMER");
        when(userRepository.existsByEmail("kiet@kento.com")).thenReturn(false);
        when(roleRepository.findByNameWithPermissions("CUSTOMER")).thenReturn(Optional.of(customer));
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);

        authService.register(registerRequest(RAW_PASSWORD, RAW_PASSWORD));

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getPassword()).isEqualTo(HASHED_PASSWORD);
        assertThat(saved.getPassword()).isNotEqualTo(RAW_PASSWORD);
        assertThat(saved.getEmail()).isEqualTo("kiet@kento.com");
        assertThat(saved.getFullName()).isEqualTo("Tran Danh Kiet");
        assertThat(saved.getPhoneNumber()).isEqualTo("0912345678");
    }

    @Test
    void registerAssignsTheDefaultCustomerRole() {
        Role customer = role(1L, "CUSTOMER");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByNameWithPermissions("CUSTOMER")).thenReturn(Optional.of(customer));
        when(passwordEncoder.encode(anyString())).thenReturn(HASHED_PASSWORD);

        authService.register(registerRequest(RAW_PASSWORD, RAW_PASSWORD));

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoles()).containsExactly(customer);
    }

    @Test
    void registerRejectsADuplicateEmail() {
        when(userRepository.existsByEmail("kiet@kento.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest(RAW_PASSWORD, RAW_PASSWORD)))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, roleRepository);
    }

    @Test
    void registerRejectsAConfirmPasswordThatDoesNotMatch() {
        assertThatThrownBy(() -> authService.register(registerRequest(RAW_PASSWORD, "SomethingElse1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Passwords don't match");

        verifyNoInteractions(userRepository, roleRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void registerFailsWhenTheDefaultCustomerRoleIsMissingFromTheDatabase() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByNameWithPermissions("CUSTOMER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(registerRequest(RAW_PASSWORD, RAW_PASSWORD)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has not been seeded");

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsATokenWithTheUserSummary() {
        User user = user(7L, "kiet@kento.com", role(1L, "CUSTOMER"));
        when(userRepository.findByEmailWithAuthorities("kiet@kento.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("a.jwt.token");

        AuthResponse response = authService.login(loginRequest("kiet@kento.com", RAW_PASSWORD));

        assertThat(response.getToken()).isEqualTo("a.jwt.token");
        assertThat(response.getUser().getId()).isEqualTo(7L);
        assertThat(response.getUser().getEmail()).isEqualTo("kiet@kento.com");
        assertThat(response.getUser().getFullName()).isEqualTo("Tran Danh Kiet");
    }

    @Test
    void loginFlattensRolesAndPermissionsSortedAndWithoutDuplicates() {
        User user = user(7L, "dual@kento.com",
                role(1L, "CUSTOMER"),
                role(2L, "ORDER_STAFF", "ORDER_READ_ALL", "ORDER_UPDATE_STATUS"),
                role(3L, "PRODUCT_STAFF", "ORDER_READ_ALL", "PRODUCT_CREATE"));
        when(userRepository.findByEmailWithAuthorities("dual@kento.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("a.jwt.token");

        AuthResponse response = authService.login(loginRequest("dual@kento.com", RAW_PASSWORD));

        assertThat(response.getRoles()).containsExactly("CUSTOMER", "ORDER_STAFF", "PRODUCT_STAFF");
        assertThat(response.getPermissions())
                .containsExactly("ORDER_READ_ALL", "ORDER_UPDATE_STATUS", "PRODUCT_CREATE");
    }

    @Test
    void loginRejectsAnUnknownEmail() {
        when(userRepository.findByEmailWithAuthorities("ghost@kento.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest("ghost@kento.com", RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void loginRejectsAWrongPassword() {
        User user = user(7L, "kiet@kento.com", role(1L, "CUSTOMER"));
        when(userRepository.findByEmailWithAuthorities("kiet@kento.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", HASHED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("kiet@kento.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void meReturnsTheFreshlyReadSummaryRolesAndPermissionsWithoutAToken() {
        User principal = user(7L, "kiet@kento.com");
        User fresh = user(7L, "kiet@kento.com",
                role(1L, "CUSTOMER"),
                role(2L, "ORDER_STAFF", "ORDER_READ_ALL"));
        when(userRepository.findByIdWithAuthorities(7L)).thenReturn(Optional.of(fresh));

        AuthResponse response = authService.me(principal);

        assertThat(response.getToken()).isNull();
        assertThat(response.getUser().getId()).isEqualTo(7L);
        assertThat(response.getRoles()).containsExactly("CUSTOMER", "ORDER_STAFF");
        assertThat(response.getPermissions()).containsExactly("ORDER_READ_ALL");
    }

    @Test
    void meRejectsAPrincipalWhoseAccountNoLongerExists() {
        User principal = user(7L, "kiet@kento.com");
        when(userRepository.findByIdWithAuthorities(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(principal))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("User no longer exists");
    }
}
