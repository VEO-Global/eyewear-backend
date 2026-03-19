package com.veo.backend.service.impl;

import com.veo.backend.dto.request.VirtualTryOnRequest;
import com.veo.backend.dto.response.VirtualTryOnResponse;
import com.veo.backend.entity.User;
import com.veo.backend.entity.VirtualTryOnSession;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.repository.VirtualTryOnSessionRepository;
import com.veo.backend.service.VirtualTryOnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VirtualTryOnServiceImpl implements VirtualTryOnService {
    private final UserRepository userRepository;
    private final VirtualTryOnSessionRepository sessionRepository;

    @Override
    public VirtualTryOnResponse createTryOnSession(String email, VirtualTryOnRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        VirtualTryOnSession session = new VirtualTryOnSession();
        session.setUser(user);
        session.setInputImageUrl(request.getInputImageUrl());
        session.setCreatedAt(LocalDateTime.now());

        // stub face analysis, implement real ML/AR integration later
        session.setFaceShape("Oval");
        session.setSuggestedFrameSize("Medium");
        session.setSuggestedBrands("RayBan, Oakley, GongKienG");
        session.setModel3dUrl("/assets/models/sample-tryon.glb");
        session.setNote("Đề xuất khung phù hợp theo kích thước khuôn mặt và sở thích màu sắc.");

        VirtualTryOnSession saved = sessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Override
    public List<VirtualTryOnResponse> getMySessions(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        return sessionRepository.findByUserId(user.getId()).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private VirtualTryOnResponse mapToResponse(VirtualTryOnSession session) {
        return VirtualTryOnResponse.builder()
                .sessionId(session.getId())
                .inputImageUrl(session.getInputImageUrl())
                .faceShape(session.getFaceShape())
                .suggestedFrameSize(session.getSuggestedFrameSize())
                .suggestedBrands(session.getSuggestedBrands())
                .model3dUrl(session.getModel3dUrl())
                .note(session.getNote())
                .build();
    }
}
