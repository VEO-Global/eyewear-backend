package com.veo.backend.service;

import com.veo.backend.dto.request.UserCreateRequest;
import com.veo.backend.dto.response.UserCreateResponse;

public interface AdminUserService {
    UserCreateResponse createUser(UserCreateRequest request);

    void deleteUser(Long id);
}
