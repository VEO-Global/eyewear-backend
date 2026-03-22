package com.veo.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veo.backend.dto.request.CreateNotificationRequest;
import com.veo.backend.dto.response.NotificationListResponse;
import com.veo.backend.dto.response.NotificationResponse;
import com.veo.backend.dto.response.UnreadNotificationCountResponse;
import com.veo.backend.entity.User;
import com.veo.backend.entity.UserNotification;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.UserNotificationRepository;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public NotificationResponse createNotification(String email, CreateNotificationRequest request) {
        User user = getUserByEmail(email);

        UserNotification notification = UserNotification.builder()
                .user(user)
                .type(request.getType())
                .title(trimToNull(request.getTitle()))
                .message(request.getMessage().trim())
                .isRead(false)
                .sourceModule(request.getSourceModule().trim())
                .metadataJson(writeJson(request.getMetadata()))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        userNotificationRepository.save(notification);
        return mapToResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse getMyNotifications(String email) {
        User user = getUserByEmail(email);
        LocalDateTime now = LocalDateTime.now();

        List<NotificationResponse> notifications = userNotificationRepository.findActiveByUserId(user.getId(), now)
                .stream()
                .map(this::mapToResponse)
                .toList();

        long unreadCount = userNotificationRepository.countUnreadByUserId(user.getId(), now);

        return NotificationListResponse.builder()
                .notifications(notifications)
                .unreadCount(unreadCount)
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String email, Long notificationId) {
        User user = getUserByEmail(email);
        UserNotification notification = getActiveNotification(user.getId(), notificationId);
        notification.setIsRead(true);
        userNotificationRepository.save(notification);
        return mapToResponse(notification);
    }

    @Override
    @Transactional
    public UnreadNotificationCountResponse markAllAsRead(String email) {
        User user = getUserByEmail(email);
        LocalDateTime now = LocalDateTime.now();
        userNotificationRepository.markAllAsRead(user.getId(), now);
        return new UnreadNotificationCountResponse(userNotificationRepository.countUnreadByUserId(user.getId(), now));
    }

    @Override
    @Transactional
    public void deleteNotification(String email, Long notificationId) {
        User user = getUserByEmail(email);
        UserNotification notification = getActiveNotification(user.getId(), notificationId);
        userNotificationRepository.delete(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(String email) {
        User user = getUserByEmail(email);
        long unreadCount = userNotificationRepository.countUnreadByUserId(user.getId(), LocalDateTime.now());
        return new UnreadNotificationCountResponse(unreadCount);
    }

    @Override
    @Transactional
    public int deleteExpiredNotifications() {
        return userNotificationRepository.deleteExpired(LocalDateTime.now());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
    }

    private UserNotification getActiveNotification(Long userId, Long notificationId) {
        return userNotificationRepository.findActiveByIdAndUserId(notificationId, userId, LocalDateTime.now())
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found"));
    }

    private NotificationResponse mapToResponse(UserNotification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .sourceModule(notification.getSourceModule())
                .metadata(readJson(notification.getMetadataJson()))
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .expiresAt(notification.getExpiresAt())
                .build();
    }

    private String writeJson(JsonNode metadata) {
        if (metadata == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Invalid notification metadata");
        }
    }

    private JsonNode readJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(metadataJson);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Cannot parse notification metadata");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
