package com.example.ThesisConnect.dto;

import java.util.List;

public record DirectConversationResponse(
        ProfileResponse partner,
        List<DirectMessageResponse> messages
) {
}
