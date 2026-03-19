package com.veo.backend.service;

import com.veo.backend.dto.request.VirtualTryOnRequest;
import com.veo.backend.dto.response.VirtualTryOnResponse;

import java.util.List;

public interface VirtualTryOnService {
    VirtualTryOnResponse createTryOnSession(String email, VirtualTryOnRequest request);
    List<VirtualTryOnResponse> getMySessions(String email);
}