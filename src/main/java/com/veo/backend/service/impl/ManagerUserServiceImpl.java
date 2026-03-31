package com.veo.backend.service.impl;

import com.veo.backend.dto.request.ManagerUserStatusRequest;
import com.veo.backend.dto.request.ManagerUserUpdateRequest;
import com.veo.backend.dto.request.UserCreateRequest;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.dto.response.UserCreateResponse;
import com.veo.backend.dto.response.UserResponse;
import com.veo.backend.entity.Role;
import com.veo.backend.entity.User;
import com.veo.backend.entity.UserAddress;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.RoleRepository;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.service.ManagerUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerUserServiceImpl implements ManagerUserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final List<String> MANAGEABLE_ROLES = List.of("SALES", "OPERATIONS");

    @Override
    public PagedResponse<UserResponse> getStaffList(String keyword, String role, Boolean active, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
        var result = userRepository.searchUsersByRoles(
                MANAGEABLE_ROLES,
                normalizeRoleName(role),
                active,
                normalizeKeyword(keyword),
                pageable
        );

        return PagedResponse.<UserResponse>builder()
                .content(result.getContent().stream().map(this::mapToUserResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId, ManagerUserStatusRequest request) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        validateManageableRole(user.getRole() != null ? user.getRole().getName() : null);
        user.setIsActive(request.getIsActive());
        userRepository.save(user);
    }

    @Override
    public UserResponse getStaffById(Long userId) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
        validateManageableRole(user.getRole() != null ? user.getRole().getName() : null);
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserCreateResponse createStaff(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXIST, "Email already exists");
        }

        String roleName = normalizeRoleName(request.getRoleName());
        validateManageableRole(roleName);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND, "Role not found"));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .avatarUrl(request.getAvatarUrl())
                .role(role)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        return UserCreateResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .phone(savedUser.getPhone())
                .avatarUrl(savedUser.getAvatarUrl())
                .role(savedUser.getRole().getName())
                .isActive(savedUser.getIsActive())
                .build();
    }

    @Override
    @Transactional
    public UserResponse updateStaff(Long userId, ManagerUserUpdateRequest request) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
        validateManageableRole(user.getRole() != null ? user.getRole().getName() : null);

        if (request.getFullName() != null) user.setFullName(request.getFullName().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone().trim());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl().trim());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());

        if (request.getRoleName() != null) {
            String roleName = normalizeRoleName(request.getRoleName());
            validateManageableRole(roleName);
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND, "Role not found"));
            user.setRole(role);
        }

        return mapToUserResponse(userRepository.save(user));
    }

    @Override
    public List<String> getManageableRoles() {
        return MANAGEABLE_ROLES;
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

    private void validateManageableRole(String roleName) {
        if (roleName == null || !MANAGEABLE_ROLES.contains(roleName)) {
            throw new AppException(ErrorCode.INVALID_ROLE, "Manager can only manage SALES and OPERATIONS staff");
        }
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }

        String normalized = roleName.trim().toUpperCase();
        if ("SALE".equals(normalized)) {
            return "SALES";
        }
        if ("OPERATION".equals(normalized)) {
            return "OPERATIONS";
        }
        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
