package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.LoginRequest;
import com.e_commerce.kento_shopping.dto.request.RegisterRequest;
import com.e_commerce.kento_shopping.dto.response.AuthResponse;

public interface AuthService {
    void register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest request);
}
