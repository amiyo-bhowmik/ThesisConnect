package com.example.ThesisConnect.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        String message,
        LocalDateTime timestamp,
        boolean read
) {
}
