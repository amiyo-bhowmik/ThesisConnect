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