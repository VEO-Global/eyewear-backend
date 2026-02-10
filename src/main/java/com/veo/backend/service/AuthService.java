package com.veo.backend.service;

import com.veo.backend.dto.request.LoginRequest;
import com.veo.backend.dto.request.RegisterRequest;
import com.veo.backend.dto.response.AuthResponse;

public interface AuthService {
    String login(LoginRequest loginRequest);

    void register(RegisterRequest registerRequest);
}
