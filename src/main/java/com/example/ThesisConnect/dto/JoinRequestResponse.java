package com.example.ThesisConnect.dto;

public record JoinRequestResponse(
        Long requestId,
        ProfileResponse sender,
        ProfileResponse recipient,
        Long groupId,
        String groupTopic,
        String status,
        String requestType
) {
}
