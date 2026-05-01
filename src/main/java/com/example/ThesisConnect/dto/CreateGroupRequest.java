package com.example.ThesisConnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateGroupRequest {

    @NotBlank(message = "Topic is required.")
    @Size(max = 160, message = "Topic must be at most 160 characters.")
    private String topic;

    @NotBlank(message = "Description is required.")
    @Size(max = 1000, message = "Description must be at most 1000 characters.")
    private String description;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
