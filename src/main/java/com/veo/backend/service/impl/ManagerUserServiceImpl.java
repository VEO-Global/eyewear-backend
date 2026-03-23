package com.veo.backend.service.impl;

import com.veo.backend.dto.response.UserResponse;
import com.veo.backend.entity.User;
import com.veo.backend.entity.UserAddress;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.service.ManagerUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerUserServiceImpl implements ManagerUserService {
    private final UserRepository userRepository;

    @Override
    public List<UserResponse> getStaffList() {
        List<String> staffRoles = List.of("SALES", "MANAGER", "OPERATION");

        return userRepository.findByRoleNames(staffRoles).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId, boolean status) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        user.setIsActive(status);
        userRepository.save(user);
    }

    private UserResponse mapToUserResponse(User user) {
        UserAddress userAddress = (user.getAddresses() != null) ?user.getAddresses().stream()
                .filter(addr -> Boolean.TRUE.equals(addr.getIsDefault()))
                .findFirst()
                .orElseGet(() -> user.getAddresses().stream().findFirst().orElse(null))
                : null;

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .addressLine(userAddress != null ? userAddress.getAddressLine() : null)
                .addressDetail(userAddress != null ? userAddress.getAddressDetail() : null)
                .ward(userAddress != null ? userAddress.getWard() : null)
                .district(userAddress != null ? userAddress.getDistrict() : null)
                .city(userAddress != null ? userAddress.getCity() : null)
                .build();
    }
}
