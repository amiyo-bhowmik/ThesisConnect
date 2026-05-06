package com.example.ThesisConnect.dto;

import java.util.List;

public record ThesisGroupResponse(
        Long groupId,
        String topic,
        String description,
        ProfileResponse admin,
        List<GroupMemberResponse> members,
        List<DocumentResponse> documents,
        List<JoinRequestResponse> pendingJoinRequests,
        List<JoinRequestResponse> pendingInvitations,
        boolean currentUserMember,
        boolean currentUserAdmin,
        String currentUserJoinRequestStatus,
        String currentUserInvitationStatus
) {
}
