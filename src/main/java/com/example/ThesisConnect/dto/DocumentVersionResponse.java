package com.example.ThesisConnect.dto;

import java.time.LocalDateTime;

public record DocumentVersionResponse(
        Long versionId,
        int versionNumber,
        String originalFileName,
        long fileSize,
        ProfileResponse uploadedBy,
        LocalDateTime uploadedAt
) {
}

