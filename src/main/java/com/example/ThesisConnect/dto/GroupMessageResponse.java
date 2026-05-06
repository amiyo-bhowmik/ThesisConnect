package com.example.ThesisConnect.dto;

import java.time.LocalDateTime;

public record GroupMessageResponse(
        Long messageId,
        ProfileResponse sender,
        String content,
        LocalDateTime timestamp,
        boolean pinned,
        boolean authoredByCurrentUser
) {
}
