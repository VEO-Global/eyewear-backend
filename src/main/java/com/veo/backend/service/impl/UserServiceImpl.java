package com.veo.backend.service.impl;

import com.veo.backend.dto.request.UpdateProfileRequest;
import com.veo.backend.dto.response.UserResponse;
import com.veo.backend.entity.User;
import com.veo.backend.entity.UserAddress;
import com.veo.backend.repository.UserAddressRepository;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToResponse(user);
    }

    @Override
    public UserResponse getMyProfile() {
        User user = getAuthenticatedUser();
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        User user = getAuthenticatedUser();
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone().trim());
        userRepository.save(user);

        UserAddress userAddress = userAddressRepository
                .findFirstByUserIdOrderByIsDefaultDescIdAsc(user.getId())
                .orElseGet(() -> {
                    UserAddress newAddress = new UserAddress();
                    newAddress.setUser(user);
                    newAddress.setIsDefault(true);
                    return newAddress;
                });

        userAddress.setAddressLine(request.getAddress().trim());
        userAddressRepository.save(userAddress);

        return mapToResponse(user);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private UserResponse mapToResponse(User user) {
        String address = userAddressRepository
                .findFirstByUserIdOrderByIsDefaultDescIdAsc(user.getId())
                .map(UserAddress::getAddressLine)
                .orElse(null);

        UserResponse res = new UserResponse();
        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setFullName(user.getFullName());
        res.setPhone(user.getPhone());
        res.setAddress(address);
        res.setAvatarUrl(user.getAvatarUrl());
        res.setIsActive(user.getIsActive());
        res.setRole(user.getRole().getName());
        return res;
    }
}
