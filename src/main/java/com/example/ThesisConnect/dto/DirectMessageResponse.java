package com.example.ThesisConnect.dto;

import java.time.LocalDateTime;

public record DirectMessageResponse(
        Long messageId,
        ProfileResponse sender,
        ProfileResponse receiver,
        String content,
        LocalDateTime timestamp,
        boolean pinned,
        boolean outgoing
) {
}
