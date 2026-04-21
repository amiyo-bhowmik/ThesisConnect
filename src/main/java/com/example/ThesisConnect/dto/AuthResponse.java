package com.example.ThesisConnect.dto;

public record AuthResponse(
        String token,
        ProfileResponse user
) {
}
