package com.veo.backend.service;

import com.veo.backend.dto.response.UserResponse;

import java.util.List;

public interface ManagerUserService {
    List<UserResponse> getStaffList();

    void toggleUserStatus(Long userId, boolean status);
}
