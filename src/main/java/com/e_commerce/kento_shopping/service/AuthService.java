package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.LoginRequest;
import com.e_commerce.kento_shopping.dto.request.RegisterRequest;
import com.e_commerce.kento_shopping.dto.response.AuthResponse;
import com.e_commerce.kento_shopping.entity.User;

public interface AuthService {
    void register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest request);

    /**
     * Re-reads the caller's identity and authorities. Needed because the client
     * holds a token but no identity after a refresh, and because the roles it
     * cached at login go stale the moment an admin changes them.
     */
    AuthResponse me(User user);
}
