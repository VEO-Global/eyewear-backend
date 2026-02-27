package com.veo.backend.service;

import com.veo.backend.dto.request.LoginRequest;
import com.veo.backend.dto.request.RegisterRequest;
import com.veo.backend.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);

    AuthResponse register(RegisterRequest registerRequest);
}
