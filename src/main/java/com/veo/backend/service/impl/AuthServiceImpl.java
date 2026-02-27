package com.veo.backend.service.impl;

import com.veo.backend.dto.request.LoginRequest;
import com.veo.backend.dto.request.RegisterRequest;
import com.veo.backend.dto.response.AuthResponse;
import com.veo.backend.entity.Role;
import com.veo.backend.entity.User;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.RoleRepository;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.security.JwtService;
import com.veo.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new AppException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found"
                ));

        String jwt = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwt)
                .message("Login successful")
                .build();
    }

    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        Role role = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new AppException(
                        ErrorCode.ROLE_NOT_FOUND,
                        "Role not found"
                ));

        if(userRepository.existsByEmail(registerRequest.getEmail())){
            throw new AppException(
                    ErrorCode.EMAIL_ALREADY_EXIST,
                    "Email already exists"
            );
        }

        if(!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())){
            throw new AppException(
                    ErrorCode.CONFIRM_PASSWORD_NOT_MATCH,
                    "Password and confirm password do not match"
            );
        }

        User user = User.builder()
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .phone(registerRequest.getPhone())
                .avatarUrl(registerRequest.getAvatarUrl())
                .role(role)
                .isActive(true)
                .build();

        userRepository.save(user);
        return AuthResponse.builder()
                .message("Register successful")
                .build();
    }
}