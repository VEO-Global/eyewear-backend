package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VirtualTryOnResponse {
    private Long sessionId;
    private String inputImageUrl;
    private String faceShape;
    private String suggestedFrameSize;
    private String suggestedBrands;
    private String model3dUrl;
    private String note;
}