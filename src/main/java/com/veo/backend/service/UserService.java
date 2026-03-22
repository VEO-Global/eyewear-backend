package com.veo.backend.service;

import com.veo.backend.dto.request.UpdateProfileRequest;
import com.veo.backend.dto.response.UserAddressResponse;
import com.veo.backend.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse getMyProfile();

    UserResponse updateMyProfile(UpdateProfileRequest request);

    List<UserAddressResponse> getMyAddresses();
}
