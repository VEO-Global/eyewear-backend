package com.veo.backend.service;

import com.veo.backend.dto.response.FavoriteStatusResponse;
import com.veo.backend.dto.response.ProductResponse;

import java.util.List;

public interface FavoriteProductService {
    ProductResponse addFavorite(Long userId, Long productId);

    void removeFavorite(Long userId, Long productId);

    List<ProductResponse> getFavoritesByUser(Long userId);

    FavoriteStatusResponse getFavoriteStatus(Long userId, Long productId);
}
