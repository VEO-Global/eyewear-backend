package com.veo.backend.service.impl;

import com.veo.backend.dto.request.LoginRequest;
import com.veo.backend.dto.request.RegisterRequest;
import com.veo.backend.dto.response.AuthResponse;
import com.veo.backend.entity.Role;
import com.veo.backend.entity.User;
import com.veo.backend.repository.RoleRepository;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.security.CustomUserDetailsService;
import com.veo.backend.service.AuthService;
import com.veo.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public String login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(loginRequest.getEmail());

        return jwtService.generateToken(userDetails);
    }

    @Override
    public void register(RegisterRequest registerRequest) {
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Role role = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(role);
        user.setFullName(registerRequest.getFullName());
        user.setAvatarUrl(registerRequest.getAvatarUrl());
        user.setCreatedAt(LocalDateTime.now());
        user.setIsActive(true);

        userRepository.save(user);
    }
}