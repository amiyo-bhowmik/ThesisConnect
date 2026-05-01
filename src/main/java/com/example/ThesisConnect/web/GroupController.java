package com.example.ThesisConnect.web;

import com.example.ThesisConnect.dto.CreateGroupRequest;
import com.example.ThesisConnect.dto.InviteMemberRequest;
import com.example.ThesisConnect.dto.NotificationResponse;
import com.example.ThesisConnect.dto.ThesisGroupResponse;
import com.example.ThesisConnect.dto.ThesisGroupSummaryResponse;
import com.example.ThesisConnect.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<ThesisGroupSummaryResponse> listGroups(Authentication authentication) {
        return groupService.listGroups(authentication.getName());
    }

    @PostMapping
    public ThesisGroupResponse createGroup(
            Authentication authentication,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        return groupService.createGroup(authentication.getName(), request);
    }

    @GetMapping("/{groupId}")
    public ThesisGroupResponse getGroup(Authentication authentication, @PathVariable Long groupId) {
        return groupService.getGroup(authentication.getName(), groupId);
    }

    @PostMapping("/{groupId}/join-requests")
    public ThesisGroupResponse sendJoinRequest(Authentication authentication, @PathVariable Long groupId) {
        return groupService.sendJoinRequest(authentication.getName(), groupId);
    }

    @PostMapping("/{groupId}/invitations")
    public ThesisGroupResponse inviteMember(
            Authentication authentication,
            @PathVariable Long groupId,
            @Valid @RequestBody InviteMemberRequest request
    ) {
        return groupService.inviteMember(authentication.getName(), groupId, request.getUserId());
    }

    @PostMapping("/{groupId}/requests/{requestId}/approve")
    public ThesisGroupResponse approveRequest(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long requestId
    ) {
        return groupService.approveRequest(authentication.getName(), groupId, requestId);
    }

    @PostMapping("/{groupId}/requests/{requestId}/reject")
    public ThesisGroupResponse rejectRequest(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long requestId
    ) {
        return groupService.rejectRequest(authentication.getName(), groupId, requestId);
    }

    @PostMapping("/{groupId}/members/{userId}/admins")
    public ThesisGroupResponse assignAdmin(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long userId
    ) {
        return groupService.assignAdmin(authentication.getName(), groupId, userId);
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> listNotifications(Authentication authentication) {
        return groupService.listNotifications(authentication.getName());
    }
}
