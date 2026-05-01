package com.example.ThesisConnect.service;

import com.example.ThesisConnect.domain.JoinRequest;
import com.example.ThesisConnect.domain.Notification;
import com.example.ThesisConnect.domain.RequestStatus;
import com.example.ThesisConnect.domain.RequestType;
import com.example.ThesisConnect.domain.ThesisGroup;
import com.example.ThesisConnect.domain.User;
import com.example.ThesisConnect.dto.CreateGroupRequest;
import com.example.ThesisConnect.dto.GroupMemberResponse;
import com.example.ThesisConnect.dto.JoinRequestResponse;
import com.example.ThesisConnect.dto.NotificationResponse;
import com.example.ThesisConnect.dto.ProfileResponse;
import com.example.ThesisConnect.dto.ThesisGroupResponse;
import com.example.ThesisConnect.dto.ThesisGroupSummaryResponse;
import com.example.ThesisConnect.repository.GroupRepository;
import com.example.ThesisConnect.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ThesisGroupResponse createGroup(String currentUserEmail, CreateGroupRequest request) {
        User currentUser = findByEmail(currentUserEmail);
        Long groupId = groupRepository.createGroup(
                request.getTopic().trim(),
                request.getDescription().trim(),
                currentUser.getUserId()
        );
        groupRepository.createNotification(
                currentUser.getUserId(),
                "You created the thesis group \"" + request.getTopic().trim() + "\"."
        );
        return getGroup(currentUserEmail, groupId);
    }
    public List<ThesisGroupSummaryResponse> listGroups(String currentUserEmail) {
        User currentUser = findByEmail(currentUserEmail);
        return groupRepository.findAllGroups().stream()
                .map(group -> {
                    User admin = findById(group.adminUserId());
                    int memberCount = groupRepository.findGroupMembers(group.groupId()).size();
                    String joinRequestStatus = groupRepository.findPendingJoinRequest(currentUser.getUserId(), group.groupId())
                            .map(request -> request.status().name())
                            .orElse(null);
                    String invitationStatus = groupRepository.findPendingInvitation(currentUser.getUserId(), group.groupId())
                            .map(request -> request.status().name())
                            .orElse(null);
                    return new ThesisGroupSummaryResponse(
                            group.groupId(),
                            group.topic(),
                            group.description(),
                            admin.getName(),
                            memberCount,
                            groupRepository.isMember(group.groupId(), currentUser.getUserId()),
                            groupRepository.isAdmin(group.groupId(), currentUser.getUserId()),
                            joinRequestStatus,
                            invitationStatus
                    );
                })
                .toList();
    }

    public ThesisGroupResponse getGroup(String currentUserEmail, Long groupId) {
        User currentUser = findByEmail(currentUserEmail);
        GroupRepository.GroupRow groupRow = findGroupRow(groupId);
        ThesisGroup thesisGroup = mapGroup(groupRow);
        boolean currentUserMember = groupRepository.isMember(groupId, currentUser.getUserId());
        boolean currentUserAdmin = groupRepository.isAdmin(groupId, currentUser.getUserId());
        String currentUserJoinRequestStatus = groupRepository.findPendingJoinRequest(currentUser.getUserId(), groupId)
                .map(request -> request.status().name())
                .orElse(null);
        String currentUserInvitationStatus = groupRepository.findPendingInvitation(currentUser.getUserId(), groupId)
                .map(request -> request.status().name())
                .orElse(null);

        List<JoinRequestResponse> pendingJoinRequests = currentUserAdmin
                ? groupRepository.findPendingJoinRequestsForGroup(groupId).stream().map(this::mapJoinRequest).toList()
                : List.of();

        List<JoinRequestResponse> pendingInvitations;
        if (currentUserAdmin) {
            pendingInvitations = groupRepository.findPendingInvitationsForGroup(groupId).stream()
                    .map(this::mapJoinRequest)
                    .toList();
        } else {
            pendingInvitations = groupRepository.findPendingInvitation(currentUser.getUserId(), groupId)
                    .map(this::mapJoinRequest)
                    .map(List::of)
                    .orElse(List.of());
        }

        return new ThesisGroupResponse(
                thesisGroup.getGroupId(),
                thesisGroup.getTopic(),
                thesisGroup.getDescription(),
                mapProfileResponse(thesisGroup.getAdmin()),
                buildMemberResponses(groupId, thesisGroup.getMembers()),
                List.copyOf(thesisGroup.getDocuments()),
                pendingJoinRequests,
                pendingInvitations,
                currentUserMember,
                currentUserAdmin,
                currentUserJoinRequestStatus,
                currentUserInvitationStatus
        );
    }

    private GroupRepository.GroupRow findGroupRow(Long groupId) {
        return groupRepository.findGroupById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thesis group not found"));
    }

    private void requireAdmin(Long groupId, Long userId) {
        if (!groupRepository.isAdmin(groupId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group admins can perform this action");
        }
    }

    private ThesisGroup mapGroup(GroupRepository.GroupRow groupRow) {
        ThesisGroup thesisGroup = new ThesisGroup();
        thesisGroup.setGroupId(groupRow.groupId());
        thesisGroup.setTopic(groupRow.topic());
        thesisGroup.setDescription(groupRow.description());
        thesisGroup.setAdmin(findById(groupRow.adminUserId()));
        List<User> members = groupRepository.findGroupMembers(groupRow.groupId()).stream()
                .map(memberRow -> findById(memberRow.userId()))
                .toList();
        thesisGroup.setMembers(members);
        thesisGroup.setDocuments(List.of());
        return thesisGroup;
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ProfileResponse mapProfileResponse(User user) {
        return new ProfileResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getDepartment(),
                user.getUniversity(),
                user.getAcademicDetails(),
                user.getBio(),
                user.getProfilePicture(),
                List.copyOf(user.getResearchInterests()),
                List.copyOf(user.getSkills()),
                user.isLookingForGroup()
        );
    }