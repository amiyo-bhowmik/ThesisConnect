package com.example.ThesisConnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateDirectMessageRequest {

    @NotNull(message = "Recipient is required")
    private Long recipientUserId;

    @NotBlank(message = "Message content is required")
    @Size(max = 1200, message = "Message content must be 1200 characters or fewer")
    private String content;

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Long recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
