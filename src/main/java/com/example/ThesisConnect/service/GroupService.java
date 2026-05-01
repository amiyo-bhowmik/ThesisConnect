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

    @Transactional
    public ThesisGroupResponse inviteMember(String currentUserEmail, Long groupId, Long targetUserId) {
        User currentUser = findByEmail(currentUserEmail);
        User targetUser = findById(targetUserId);
        GroupRepository.GroupRow groupRow = findGroupRow(groupId);
        requireAdmin(groupId, currentUser.getUserId());

        if (groupRepository.isMember(groupId, targetUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student is already a member of this thesis group");
        }

        if (groupRepository.findPendingInvitationBySender(currentUser.getUserId(), targetUserId, groupId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An invitation is already pending for this student");
        }

        groupRepository.createJoinRequest(currentUser.getUserId(), targetUserId, groupId, RequestType.INVITATION);
        groupRepository.createNotification(
                targetUserId,
                currentUser.getName() + " invited you to join the thesis group \"" + groupRow.topic() + "\"."
        );
        return getGroup(currentUserEmail, groupId);
    }

    @Transactional
    public ThesisGroupResponse sendJoinRequest(String currentUserEmail, Long groupId) {
        User currentUser = findByEmail(currentUserEmail);
        GroupRepository.GroupRow groupRow = findGroupRow(groupId);
        User admin = findById(groupRow.adminUserId());

        if (groupRepository.isMember(groupId, currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a member of this thesis group");
        }

        if (groupRepository.findPendingJoinRequest(currentUser.getUserId(), groupId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have a pending join request for this group");
        }

        groupRepository.createJoinRequest(currentUser.getUserId(), null, groupId, RequestType.JOIN_REQUEST);
        groupRepository.createNotification(
                admin.getUserId(),
                currentUser.getName() + " requested to join the thesis group \"" + groupRow.topic() + "\"."
        );
        return getGroup(currentUserEmail, groupId);
    }

    @Transactional
    public ThesisGroupResponse approveRequest(String currentUserEmail, Long groupId, Long requestId) {
        User currentUser = findByEmail(currentUserEmail);
        GroupRepository.JoinRequestRow requestRow = findJoinRequest(groupId, requestId);
        GroupRepository.GroupRow groupRow = findGroupRow(groupId);

        if (requestRow.requestType() == RequestType.JOIN_REQUEST) {
            requireAdmin(groupId, currentUser.getUserId());
            User requester = findById(requestRow.senderUserId());
            addMemberIfMissing(groupId, requester.getUserId());
            groupRepository.updateJoinRequestStatus(requestId, RequestStatus.APPROVED, currentUser.getUserId());
            groupRepository.rejectOtherPendingRequestsForUserInGroup(requestId, groupId, requester.getUserId());
            groupRepository.createNotification(
                    requester.getUserId(),
                    "Your request to join \"" + groupRow.topic() + "\" was approved."
            );
        } else {
            if (requestRow.recipientUserId() == null || !requestRow.recipientUserId().equals(currentUser.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot accept this invitation");
            }
            addMemberIfMissing(groupId, currentUser.getUserId());
            groupRepository.updateJoinRequestStatus(requestId, RequestStatus.APPROVED, currentUser.getUserId());
            groupRepository.rejectOtherPendingRequestsForUserInGroup(requestId, groupId, currentUser.getUserId());
            groupRepository.createNotification(
                    requestRow.senderUserId(),
                    currentUser.getName() + " accepted the invitation to join \"" + groupRow.topic() + "\"."
            );
        }

        return getGroup(currentUserEmail, groupId);
    }

    @Transactional
    public ThesisGroupResponse rejectRequest(String currentUserEmail, Long groupId, Long requestId) {
        User currentUser = findByEmail(currentUserEmail);
        GroupRepository.JoinRequestRow requestRow = findJoinRequest(groupId, requestId);
        GroupRepository.GroupRow groupRow = findGroupRow(groupId);

        if (requestRow.requestType() == RequestType.JOIN_REQUEST) {
            requireAdmin(groupId, currentUser.getUserId());
            groupRepository.updateJoinRequestStatus(requestId, RequestStatus.REJECTED, currentUser.getUserId());
            groupRepository.createNotification(
                    requestRow.senderUserId(),
                    "Your request to join \"" + groupRow.topic() + "\" was rejected."
            );
        } else {
            if (requestRow.recipientUserId() == null || !requestRow.recipientUserId().equals(currentUser.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot reject this invitation");
            }
            groupRepository.updateJoinRequestStatus(requestId, RequestStatus.REJECTED, currentUser.getUserId());
            groupRepository.createNotification(
                    requestRow.senderUserId(),
                    currentUser.getName() + " rejected the invitation to join \"" + groupRow.topic() + "\"."
            );
        }

        return getGroup(currentUserEmail, groupId);
    }

    @Transactional
    public ThesisGroupResponse assignAdmin(String currentUserEmail, Long groupId, Long targetUserId) {
        User currentUser = findByEmail(currentUserEmail);
        User targetUser = findById(targetUserId);
        requireAdmin(groupId, currentUser.getUserId());

        if (!groupRepository.isMember(groupId, targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only group members can be assigned as admins");
        }

        groupRepository.setAdmin(groupId, targetUserId, true);
        groupRepository.createNotification(
                targetUser.getUserId(),
                "You were assigned as an admin of a thesis group."
        );
        return getGroup(currentUserEmail, groupId);
    }

    public List<NotificationResponse> listNotifications(String currentUserEmail) {
        User currentUser = findByEmail(currentUserEmail);
        return groupRepository.findNotificationsByUserId(currentUser.getUserId()).stream()
                .map(this::mapNotification)
                .toList();
    }

    private GroupRepository.GroupRow findGroupRow(Long groupId) {
        return groupRepository.findGroupById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thesis group not found"));
    }

    private GroupRepository.JoinRequestRow findJoinRequest(Long groupId, Long requestId) {
        GroupRepository.JoinRequestRow requestRow = groupRepository.findJoinRequestById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Join request not found"));
        if (!requestRow.groupId().equals(groupId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Join request does not belong to this group");
        }
        if (requestRow.status() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This join request has already been processed");
        }
        return requestRow;
    }

    private void addMemberIfMissing(Long groupId, Long userId) {
        if (!groupRepository.isMember(groupId, userId)) {
            groupRepository.addMember(groupId, userId, false);
        }
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

    private List<GroupMemberResponse> buildMemberResponses(Long groupId, List<User> members) {
        return members.stream()
                .map(member -> new GroupMemberResponse(
                        member.getUserId(),
                        member.getName(),
                        member.getEmail(),
                        member.getDepartment(),
                        member.getUniversity(),
                        member.getAcademicDetails(),
                        member.getBio(),
                        member.getProfilePicture(),
                        List.copyOf(member.getResearchInterests()),
                        List.copyOf(member.getSkills()),
                        member.isLookingForGroup(),
                        groupRepository.isAdmin(groupId, member.getUserId())
                ))
                .toList();
    }

    private JoinRequestResponse mapJoinRequest(GroupRepository.JoinRequestRow row) {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setRequestId(row.requestId());
        joinRequest.setSender(findById(row.senderUserId()));
        joinRequest.setRecipient(row.recipientUserId() == null ? null : findById(row.recipientUserId()));
        joinRequest.setGroup(mapGroup(findGroupRow(row.groupId())));
        joinRequest.setStatus(row.status());
        joinRequest.setRequestType(row.requestType());
        return new JoinRequestResponse(
                joinRequest.getRequestId(),
                mapProfileResponse(joinRequest.getSender()),
                joinRequest.getRecipient() == null ? null : mapProfileResponse(joinRequest.getRecipient()),
                joinRequest.getGroup().getGroupId(),
                joinRequest.getGroup().getTopic(),
                joinRequest.getStatus().name(),
                joinRequest.getRequestType().name()
        );
    }

    private NotificationResponse mapNotification(GroupRepository.NotificationRow row) {
        Notification notification = new Notification();
        notification.setNotificationId(row.notificationId());
        notification.setUser(findById(row.userId()));
        notification.setMessage(row.message());
        notification.setTimestamp(row.timestamp());
        notification.setRead(row.isRead());
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getMessage(),
                notification.getTimestamp(),
                notification.isRead()
        );
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
}
