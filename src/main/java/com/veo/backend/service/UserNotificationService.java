package com.veo.backend.service;

import com.veo.backend.dto.request.CreateNotificationRequest;
import com.veo.backend.dto.response.NotificationListResponse;
import com.veo.backend.dto.response.NotificationResponse;
import com.veo.backend.dto.response.UnreadNotificationCountResponse;

public interface UserNotificationService {
    NotificationResponse createNotification(String email, CreateNotificationRequest request);

    NotificationListResponse getMyNotifications(String email);

    NotificationResponse markAsRead(String email, Long notificationId);

    UnreadNotificationCountResponse markAllAsRead(String email);

    void deleteNotification(String email, Long notificationId);

    UnreadNotificationCountResponse getUnreadCount(String email);

    int deleteExpiredNotifications();
}
