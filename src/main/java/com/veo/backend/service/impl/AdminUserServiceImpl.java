package com.veo.backend.service.impl;

import com.veo.backend.dto.request.UserCreateRequest;
import com.veo.backend.dto.response.UserCreateResponse;
import com.veo.backend.entity.Role;
import com.veo.backend.entity.User;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.RoleRepository;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserCreateResponse createUser(UserCreateRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND, "Email already exists");
        }

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND, "Role not found: " + request.getRoleName()));

        if (role.getName().equals("CUSTOMER")) {
            throw new AppException(ErrorCode.INVALID_ROLE, "Admin cannot created customer account");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setIsActive(true);
        userRepository.save(user);

        return UserCreateResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(role.getName())
                .isActive(user.getIsActive())
                .build();
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findUserById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        user.setIsActive(false);
        userRepository.save(user);
    }
}
