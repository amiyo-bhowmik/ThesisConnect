package com.example.ThesisConnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateGroupMessageRequest {

    @NotBlank(message = "Message content is required")
    @Size(max = 1200, message = "Message content must be 1200 characters or fewer")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
