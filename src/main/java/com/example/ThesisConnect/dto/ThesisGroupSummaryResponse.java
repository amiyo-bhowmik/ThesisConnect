package com.example.ThesisConnect.dto;

public record ThesisGroupSummaryResponse(
        Long groupId,
        String topic,
        String description,
        String adminName,
        int memberCount,
        boolean currentUserMember,
        boolean currentUserAdmin,
        String currentUserJoinRequestStatus,
        String currentUserInvitationStatus
) {
}
