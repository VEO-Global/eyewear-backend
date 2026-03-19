package com.veo.backend.dto.request;

import lombok.Data;

@Data
public class VirtualTryOnRequest {
    private String inputImageUrl;
    private String preferredFrameStyle;
    private String preferredColor;
}