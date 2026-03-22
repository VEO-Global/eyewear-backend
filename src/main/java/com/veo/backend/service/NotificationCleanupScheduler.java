package com.veo.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {
    private final UserNotificationService userNotificationService;

    @Scheduled(cron = "${app.notifications.cleanup-cron:0 0 3 * * *}")
    public void cleanupExpiredNotifications() {
        userNotificationService.deleteExpiredNotifications();
    }
}
