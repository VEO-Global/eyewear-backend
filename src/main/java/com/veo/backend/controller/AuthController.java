package com.veo.backend.controller;

import com.veo.backend.dto.request.LoginRequest;
import com.veo.backend.dto.request.RegisterRequest;
import com.veo.backend.dto.response.AuthResponse;
import com.veo.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request){
        authService.register(request);
        return ResponseEntity.ok("Register successfully");
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request){
        String token = authService.login(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
