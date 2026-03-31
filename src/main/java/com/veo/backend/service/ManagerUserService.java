package com.veo.backend.service;

import com.veo.backend.dto.request.ManagerUserStatusRequest;
import com.veo.backend.dto.request.ManagerUserUpdateRequest;
import com.veo.backend.dto.request.UserCreateRequest;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.dto.response.UserCreateResponse;
import com.veo.backend.dto.response.UserResponse;

import java.util.List;

public interface ManagerUserService {
    PagedResponse<UserResponse> getStaffList(String keyword, String role, Boolean active, int page, int size);

    UserResponse getStaffById(Long userId);

    UserCreateResponse createStaff(UserCreateRequest request);

    UserResponse updateStaff(Long userId, ManagerUserUpdateRequest request);

    void toggleUserStatus(Long userId, ManagerUserStatusRequest request);

    List<String> getManageableRoles();
}
