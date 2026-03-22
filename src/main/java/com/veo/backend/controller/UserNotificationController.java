package com.veo.backend.controller;

import com.veo.backend.dto.request.CreateNotificationRequest;
import com.veo.backend.dto.response.NotificationListResponse;
import com.veo.backend.dto.response.NotificationResponse;
import com.veo.backend.dto.response.UnreadNotificationCountResponse;
import com.veo.backend.service.UserNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class UserNotificationController {
    private final UserNotificationService userNotificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userNotificationService.createNotification(authentication.getName(), request));
    }

    @GetMapping
    public NotificationListResponse getMyNotifications(Authentication authentication) {
        return userNotificationService.getMyNotifications(authentication.getName());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable Long id, Authentication authentication) {
        return userNotificationService.markAsRead(authentication.getName(), id);
    }

    @PatchMapping("/read-all")
    public UnreadNotificationCountResponse markAllAsRead(Authentication authentication) {
        return userNotificationService.markAllAsRead(authentication.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, Authentication authentication) {
        userNotificationService.deleteNotification(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse getUnreadCount(Authentication authentication) {
        return userNotificationService.getUnreadCount(authentication.getName());
    }
}
