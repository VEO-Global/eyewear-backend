package com.veo.backend.service;

import com.veo.backend.dto.request.PromotionRequest;
import com.veo.backend.dto.response.PromotionResponse;

import java.util.List;

public interface PromotionService {
    List<PromotionResponse> getAllPromotions();

    PromotionResponse getPromotionById(Long id);

    PromotionResponse createPromotion(PromotionRequest request);

    PromotionResponse updatePromotion(Long id, PromotionRequest request);

    void deletePromotion(Long id);

    PromotionResponse validateCode(String code);
}
