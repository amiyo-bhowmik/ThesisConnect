package com.example.ThesisConnect.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentResponse(
        Long documentId,
        String title,
        String originalFileName,
        String visibility,
        int version,
        long fileSize,
        LocalDateTime uploadDate,
        ProfileResponse uploadedBy,
        boolean currentUserCanView,
        boolean currentUserCanComment,
        boolean currentUserCanUploadNewVersion,
        List<DocumentVersionResponse> versions,
        List<DocumentCommentResponse> comments
) {
}
