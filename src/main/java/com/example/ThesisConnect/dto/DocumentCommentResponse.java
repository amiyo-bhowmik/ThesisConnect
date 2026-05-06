package com.example.ThesisConnect.dto;

import java.time.LocalDateTime;

public record DocumentCommentResponse(
        Long commentId,
        String content,
        ProfileResponse author,
        LocalDateTime timestamp,
        boolean authorIsGroupMember,
        String authorScopeLabel
) {
}

