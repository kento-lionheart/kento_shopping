package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.request.LoginRequest;
import com.e_commerce.kento_shopping.dto.request.RegisterRequest;
import com.e_commerce.kento_shopping.dto.response.AuthResponse;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.enums.Role;
import com.e_commerce.kento_shopping.exception.EmailAlreadyExistsException;
import com.e_commerce.kento_shopping.exception.InvalidCredentialsException;
import com.e_commerce.kento_shopping.repository.UserRepository;
import com.e_commerce.kento_shopping.service.AuthService;
import com.e_commerce.kento_shopping.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequest request){
        if(!request.getPassword().equals(request.getConfirmPassword())){
            throw new IllegalArgumentException("Passwords don't match !!");
        }
        if(userRepository.existsByEmail(request.getEmail())){
            throw new EmailAlreadyExistsException("This email is already in use honey");
        }
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.CUSTOMER)
                .build();
        userRepository.save(user);
    }
    @Override
    public AuthResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new InvalidCredentialsException("Invalid email or password");
        }
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getRole());
    }
}
