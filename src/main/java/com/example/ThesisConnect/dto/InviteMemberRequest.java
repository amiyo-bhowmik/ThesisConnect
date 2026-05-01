package com.example.ThesisConnect.dto;

import jakarta.validation.constraints.NotNull;

public class InviteMemberRequest {

    @NotNull(message = "Student selection is required.")
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
