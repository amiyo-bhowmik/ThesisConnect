package com.example.ThesisConnect.service;

import com.example.ThesisConnect.domain.GroupMessage;
import com.example.ThesisConnect.domain.Message;
import com.example.ThesisConnect.domain.ThesisGroup;
import com.example.ThesisConnect.domain.User;
import com.example.ThesisConnect.dto.CreateDirectMessageRequest;
import com.example.ThesisConnect.dto.CreateGroupMessageRequest;
import com.example.ThesisConnect.dto.DirectConversationResponse;
import com.example.ThesisConnect.dto.DirectMessageResponse;
import com.example.ThesisConnect.dto.GroupMessageResponse;
import com.example.ThesisConnect.dto.ProfileResponse;
import com.example.ThesisConnect.repository.CommunicationRepository;
import com.example.ThesisConnect.repository.GroupRepository;
import com.example.ThesisConnect.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CommunicationService {

    private final CommunicationRepository communicationRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public CommunicationService(
            CommunicationRepository communicationRepository,
            UserRepository userRepository,
            GroupRepository groupRepository
    ) {
        this.communicationRepository = communicationRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    public DirectConversationResponse getConversation(String currentUserEmail, Long otherUserId) {
        User currentUser = findByEmail(currentUserEmail);
        User otherUser = findById(otherUserId);
        List<DirectMessageResponse> messages = communicationRepository.findConversation(
                        currentUser.getUserId(),
                        otherUserId
                ).stream()
                .map(row -> mapDirectMessage(row, currentUser.getUserId()))
                .toList();
        return new DirectConversationResponse(mapProfileResponse(otherUser), messages);
    }

    @Transactional
    public DirectConversationResponse sendDirectMessage(String currentUserEmail, CreateDirectMessageRequest request) {
        User currentUser = findByEmail(currentUserEmail);
        User recipient = findById(request.getRecipientUserId());
        if (currentUser.getUserId().equals(recipient.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot send a direct message to yourself");
        }

        String content = trimToNull(request.getContent());
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required");
        }

        communicationRepository.createDirectMessage(currentUser.getUserId(), recipient.getUserId(), content);
        groupRepository.createNotification(
                recipient.getUserId(),
                "New direct message from " + currentUser.getName() + "."
        );
        return getConversation(currentUserEmail, recipient.getUserId());
    }

    @Transactional
    public DirectConversationResponse pinDirectMessage(String currentUserEmail, Long messageId) {
        User currentUser = findByEmail(currentUserEmail);
        CommunicationRepository.DirectMessageRow messageRow = communicationRepository.findDirectMessageById(
                        messageId,
                        currentUser.getUserId()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Direct message not found"));
        requireParticipant(currentUser.getUserId(), messageRow.senderUserId(), messageRow.receiverUserId());
        communicationRepository.pinDirectMessage(currentUser.getUserId(), messageId);
        Long otherUserId = currentUser.getUserId().equals(messageRow.senderUserId())
                ? messageRow.receiverUserId()
                : messageRow.senderUserId();
        return getConversation(currentUserEmail, otherUserId);
    }

    @Transactional
    public DirectConversationResponse unpinDirectMessage(String currentUserEmail, Long messageId) {
        User currentUser = findByEmail(currentUserEmail);
        CommunicationRepository.DirectMessageRow messageRow = communicationRepository.findDirectMessageById(
                        messageId,
                        currentUser.getUserId()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Direct message not found"));
        requireParticipant(currentUser.getUserId(), messageRow.senderUserId(), messageRow.receiverUserId());
        communicationRepository.unpinDirectMessage(currentUser.getUserId(), messageId);
        Long otherUserId = currentUser.getUserId().equals(messageRow.senderUserId())
                ? messageRow.receiverUserId()
                : messageRow.senderUserId();
        return getConversation(currentUserEmail, otherUserId);
    }

    public List<GroupMessageResponse> getGroupMessages(String currentUserEmail, Long groupId) {
        User currentUser = findByEmail(currentUserEmail);
        requireGroupMember(groupId, currentUser.getUserId());
        return communicationRepository.findGroupMessages(groupId, currentUser.getUserId()).stream()
                .map(row -> mapGroupMessage(row, currentUser.getUserId()))
                .toList();
    }

    @Transactional
    public List<GroupMessageResponse> sendGroupMessage(
            String currentUserEmail,
            Long groupId,
            CreateGroupMessageRequest request
    ) {
        User currentUser = findByEmail(currentUserEmail);
        requireGroupMember(groupId, currentUser.getUserId());
        String content = trimToNull(request.getContent());
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required");
        }
        communicationRepository.createGroupMessage(currentUser.getUserId(), groupId, content);
        return getGroupMessages(currentUserEmail, groupId);
    }

    @Transactional
    public List<GroupMessageResponse> pinGroupMessage(String currentUserEmail, Long groupId, Long messageId) {
        User currentUser = findByEmail(currentUserEmail);
        requireGroupMember(groupId, currentUser.getUserId());
        CommunicationRepository.GroupMessageRow messageRow = communicationRepository.findGroupMessageById(
                        messageId,
                        currentUser.getUserId()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group message not found"));
        if (!messageRow.groupId().equals(groupId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message does not belong to this group");
        }
        communicationRepository.pinGroupMessage(currentUser.getUserId(), messageId);
        return getGroupMessages(currentUserEmail, groupId);
    }

    @Transactional
    public List<GroupMessageResponse> unpinGroupMessage(String currentUserEmail, Long groupId, Long messageId) {
        User currentUser = findByEmail(currentUserEmail);
        requireGroupMember(groupId, currentUser.getUserId());
        CommunicationRepository.GroupMessageRow messageRow = communicationRepository.findGroupMessageById(
                        messageId,
                        currentUser.getUserId()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group message not found"));
        if (!messageRow.groupId().equals(groupId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message does not belong to this group");
        }
        communicationRepository.unpinGroupMessage(currentUser.getUserId(), messageId);
        return getGroupMessages(currentUserEmail, groupId);
    }

    private void requireParticipant(Long currentUserId, Long senderUserId, Long receiverUserId) {
        if (!currentUserId.equals(senderUserId) && !currentUserId.equals(receiverUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this message");
        }
    }

    private void requireGroupMember(Long groupId, Long userId) {
        if (!groupRepository.isMember(groupId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group members can access group discussions");
        }
    }

    private DirectMessageResponse mapDirectMessage(CommunicationRepository.DirectMessageRow row, Long currentUserId) {
        Message message = new Message();
        message.setMessageId(row.messageId());
        message.setSender(findById(row.senderUserId()));
        message.setReceiver(findById(row.receiverUserId()));
        message.setContent(row.content());
        message.setTimestamp(row.timestamp());
        message.setPinned(row.pinned());

        return new DirectMessageResponse(
                message.getMessageId(),
                mapProfileResponse(message.getSender()),
                mapProfileResponse(message.getReceiver()),
                message.getContent(),
                message.getTimestamp(),
                message.isPinned(),
                currentUserId.equals(message.getSender().getUserId())
        );
    }

    private GroupMessageResponse mapGroupMessage(CommunicationRepository.GroupMessageRow row, Long currentUserId) {
        GroupMessage message = new GroupMessage();
        message.setMessageId(row.messageId());
        message.setSender(findById(row.senderUserId()));
        ThesisGroup group = new ThesisGroup();
        group.setGroupId(row.groupId());
        message.setGroup(group);
        message.setContent(row.content());
        message.setTimestamp(row.timestamp());
        message.setPinned(row.pinned());

        return new GroupMessageResponse(
                message.getMessageId(),
                mapProfileResponse(message.getSender()),
                message.getContent(),
                message.getTimestamp(),
                message.isPinned(),
                currentUserId.equals(message.getSender().getUserId())
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
