package com.veo.backend.controller;

import com.veo.backend.dto.response.ProductResponse;
import com.veo.backend.service.FavoriteProductService;
import com.veo.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/favorites")
@RequiredArgsConstructor
public class FavoriteProductController {
    private final FavoriteProductService favoriteProductService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getMyFavorites() {
        Long userId = userService.getMyProfile().getId();
        return ResponseEntity.ok(favoriteProductService.getFavoritesByUser(userId));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ProductResponse> addFavorite(@PathVariable Long productId) {
        Long userId = userService.getMyProfile().getId();
        return ResponseEntity.ok(favoriteProductService.addFavorite(userId, productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long productId) {
        Long userId = userService.getMyProfile().getId();
        favoriteProductService.removeFavorite(userId, productId);
        return ResponseEntity.noContent().build();
    }
}
