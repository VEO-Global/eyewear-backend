package com.veo.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnreadNotificationCountResponse {
    private Long unreadCount;
}
