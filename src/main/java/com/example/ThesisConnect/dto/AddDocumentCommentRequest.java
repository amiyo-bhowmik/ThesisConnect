package com.example.ThesisConnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddDocumentCommentRequest {

    @NotBlank(message = "Comment cannot be empty")
    @Size(max = 1200, message = "Comment cannot exceed 1200 characters")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
