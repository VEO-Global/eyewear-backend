package com.veo.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageRequest {
    private Long id;
    private String url;
    private String alt;
    private Boolean isPrimary;
    private Integer sortOrder;
}
