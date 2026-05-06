package com.example.ThesisConnect.service;

import com.example.ThesisConnect.domain.Comment;
import com.example.ThesisConnect.domain.Document;
import com.example.ThesisConnect.domain.DocumentVersion;
import com.example.ThesisConnect.domain.DocumentVisibility;
import com.example.ThesisConnect.domain.JoinRequest;
import com.example.ThesisConnect.domain.Notification;
import com.example.ThesisConnect.domain.RequestStatus;
import com.example.ThesisConnect.domain.RequestType;
import com.example.ThesisConnect.domain.ThesisGroup;
import com.example.ThesisConnect.domain.User;
import com.example.ThesisConnect.dto.AddDocumentCommentRequest;
import com.example.ThesisConnect.dto.CreateGroupRequest;
import com.example.ThesisConnect.dto.DocumentCommentResponse;
import com.example.ThesisConnect.dto.DocumentResponse;
import com.example.ThesisConnect.dto.DocumentVersionResponse;
import com.example.ThesisConnect.dto.GroupMemberResponse;
import com.example.ThesisConnect.dto.JoinRequestResponse;
import com.example.ThesisConnect.dto.NotificationResponse;
import com.example.ThesisConnect.dto.ProfileResponse;
import com.example.ThesisConnect.dto.ThesisGroupResponse;
import com.example.ThesisConnect.dto.ThesisGroupSummaryResponse;
import com.example.ThesisConnect.repository.DocumentRepository;
import com.example.ThesisConnect.repository.GroupRepository;
import com.example.ThesisConnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class GroupService {

    private static final List<String> ALLOWED_DOCUMENT_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/rtf",
            "application/octet-stream"
    );

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final Path documentUploadRoot;

    public GroupService(
            GroupRepository groupRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository,
            @Value("${app.document-upload.dir:uploads/thesis-documents}") String documentUploadDir
    ) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.documentUploadRoot = Path.of(documentUploadDir).toAbsolutePath().normalize();
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

        List<DocumentResponse> documents = documentRepository.findVisibleDocumentsByGroupId(groupId, currentUser.getUserId())
                .stream()
                .map(documentRow -> mapDocumentResponse(documentRow, currentUser.getUserId()))
                .toList();

        return new ThesisGroupResponse(
                thesisGroup.getGroupId(),
                thesisGroup.getTopic(),
                thesisGroup.getDescription(),
                mapProfileResponse(thesisGroup.getAdmin()),
                buildMemberResponses(groupId, thesisGroup.getMembers()),
                documents,
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

    @Transactional
    public ThesisGroupResponse uploadDocument(
            String currentUserEmail,
            Long groupId,
            String title,
            String visibility,
            MultipartFile file
    ) {
        User currentUser = findByEmail(currentUserEmail);
        requireMember(groupId, currentUser.getUserId());
        validateUpload(file);

        String safeTitle = trimToNull(title);
        if (safeTitle == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document title is required");
        }

        DocumentVisibility documentVisibility = parseVisibility(visibility);
        StoredFile storedFile = storeDocumentFile(groupId, file);

        long documentId = documentRepository.createDocument(
                safeTitle,
                storedFile.path().toString(),
                storedFile.originalFileName(),
                documentVisibility,
                currentUser.getUserId(),
                groupId,
                storedFile.size()
        );
        documentRepository.createDocumentVersion(
                documentId,
                1,
                storedFile.path().toString(),
                storedFile.originalFileName(),
                storedFile.size(),
                currentUser.getUserId()
        );

        return getGroup(currentUserEmail, groupId);
    }

    @Transactional
    public ThesisGroupResponse uploadDocumentVersion(
            String currentUserEmail,
            Long groupId,
            Long documentId,
            MultipartFile file
    ) {
        User currentUser = findByEmail(currentUserEmail);
        requireMember(groupId, currentUser.getUserId());
        validateUpload(file);

        DocumentRepository.DocumentRow documentRow = findDocument(documentId);
        requireDocumentBelongsToGroup(documentRow, groupId);

        int nextVersion = documentRow.version() + 1;
        StoredFile storedFile = storeDocumentFile(groupId, file);
        documentRepository.createDocumentVersion(
                documentId,
                nextVersion,
                storedFile.path().toString(),
                storedFile.originalFileName(),
                storedFile.size(),
                currentUser.getUserId()
        );
        documentRepository.updateDocumentLatestVersion(
                documentId,
                storedFile.path().toString(),
                storedFile.originalFileName(),
                nextVersion,
                storedFile.size()
        );

        return getGroup(currentUserEmail, groupId);
    }

    @Transactional
    public ThesisGroupResponse addDocumentComment(
            String currentUserEmail,
            Long groupId,
            Long documentId,
            AddDocumentCommentRequest request
    ) {
        User currentUser = findByEmail(currentUserEmail);
        DocumentRepository.DocumentRow documentRow = findDocument(documentId);
        requireDocumentBelongsToGroup(documentRow, groupId);
        requireDocumentAccess(documentRow, currentUser.getUserId());

        String content = trimToNull(request.getContent());
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment cannot be empty");
        }

        documentRepository.createComment(content, currentUser.getUserId(), documentId);
        if (!documentRow.uploadedByUserId().equals(currentUser.getUserId())) {
            groupRepository.createNotification(
                    documentRow.uploadedByUserId(),
                    currentUser.getName() + " left feedback on your shared document \"" + documentRow.title() + "\"."
            );
        }
        return getGroup(currentUserEmail, groupId);
    }

    public DocumentDownload downloadDocument(
            String currentUserEmail,
            Long groupId,
            Long documentId,
            Integer versionNumber
    ) {
        User currentUser = findByEmail(currentUserEmail);
        DocumentRepository.DocumentRow documentRow = findDocument(documentId);
        requireDocumentBelongsToGroup(documentRow, groupId);
        requireDocumentAccess(documentRow, currentUser.getUserId());

        String filePath = documentRow.filePath();
        String fileName = documentRow.originalFileName();

        if (versionNumber != null) {
            DocumentRepository.DocumentVersionRow versionRow = documentRepository
                    .findVersionByDocumentIdAndNumber(documentId, versionNumber)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document version not found"));
            filePath = versionRow.filePath();
            fileName = versionRow.originalFileName();
        }

        Path path = Path.of(filePath).toAbsolutePath().normalize();
        if (!path.startsWith(documentUploadRoot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid document path");
        }
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found");
        }

        Resource resource = new FileSystemResource(path);
        return new DocumentDownload(resource, fileName, detectContentType(path));
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

    private DocumentRepository.DocumentRow findDocument(Long documentId) {
        return documentRepository.findDocumentById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
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

    private void requireMember(Long groupId, Long userId) {
        if (!groupRepository.isMember(groupId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group members can perform this action");
        }
    }

    private void requireDocumentBelongsToGroup(DocumentRepository.DocumentRow documentRow, Long groupId) {
        if (!documentRow.groupId().equals(groupId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document does not belong to this thesis group");
        }
    }

    private void requireDocumentAccess(DocumentRepository.DocumentRow documentRow, Long userId) {
        if (documentRow.visibility() == DocumentVisibility.PUBLIC) {
            return;
        }
        if (!groupRepository.isMember(documentRow.groupId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This document is private to the group");
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

    private DocumentResponse mapDocumentResponse(DocumentRepository.DocumentRow row, Long currentUserId) {
        Document document = new Document();
        document.setDocumentId(row.documentId());
        document.setTitle(row.title());
        document.setFilePath(row.filePath());
        document.setOriginalFileName(row.originalFileName());
        document.setUploadedBy(findById(row.uploadedByUserId()));
        document.setGroup(mapGroup(findGroupRow(row.groupId())));
        document.setUploadDate(row.uploadDate());
        document.setVersion(row.version());
        document.setFileSize(row.fileSize());
        document.setVisibility(row.visibility());

        List<DocumentVersion> versions = documentRepository.findVersionsByDocumentId(row.documentId()).stream()
                .map(this::mapVersion)
                .toList();
        document.setVersions(versions);

        List<Comment> comments = documentRepository.findCommentsByDocumentId(row.documentId()).stream()
                .map(this::mapComment)
                .toList();
        document.setComments(comments);

        boolean currentUserMember = groupRepository.isMember(row.groupId(), currentUserId);
        return new DocumentResponse(
                document.getDocumentId(),
                document.getTitle(),
                document.getOriginalFileName(),
                document.getVisibility().name(),
                document.getVersion(),
                document.getFileSize(),
                document.getUploadDate(),
                mapProfileResponse(document.getUploadedBy()),
                document.getVisibility() == DocumentVisibility.PUBLIC || currentUserMember,
                document.getVisibility() == DocumentVisibility.PUBLIC || currentUserMember,
                currentUserMember,
                document.getVersions().stream().map(this::mapVersionResponse).toList(),
                document.getComments().stream()
                        .map(comment -> mapCommentResponse(comment, row.groupId()))
                        .toList()
        );
    }

    private DocumentVersion mapVersion(DocumentRepository.DocumentVersionRow row) {
        DocumentVersion version = new DocumentVersion();
        version.setVersionId(row.versionId());
        version.setVersionNumber(row.versionNumber());
        version.setFilePath(row.filePath());
        version.setOriginalFileName(row.originalFileName());
        version.setFileSize(row.fileSize());
        version.setUploadedBy(findById(row.uploadedByUserId()));
        version.setUploadedAt(row.uploadedAt());
        return version;
    }

    private Comment mapComment(DocumentRepository.CommentRow row) {
        Comment comment = new Comment();
        comment.setCommentId(row.commentId());
        comment.setContent(row.content());
        comment.setAuthor(findById(row.authorUserId()));
        comment.setTimestamp(row.timestamp());
        return comment;
    }

    private DocumentVersionResponse mapVersionResponse(DocumentVersion version) {
        return new DocumentVersionResponse(
                version.getVersionId(),
                version.getVersionNumber(),
                version.getOriginalFileName(),
                version.getFileSize(),
                mapProfileResponse(version.getUploadedBy()),
                version.getUploadedAt()
        );
    }

    private DocumentCommentResponse mapCommentResponse(Comment comment, Long groupId) {
        boolean authorIsGroupMember = groupRepository.isMember(groupId, comment.getAuthor().getUserId());
        return new DocumentCommentResponse(
                comment.getCommentId(),
                comment.getContent(),
                mapProfileResponse(comment.getAuthor()),
                comment.getTimestamp(),
                authorIsGroupMember,
                authorIsGroupMember ? "Group member" : "Outsider"
        );
    }

    private StoredFile storeDocumentFile(Long groupId, MultipartFile file) {
        try {
            Path groupDirectory = documentUploadRoot.resolve("group-" + groupId).normalize();
            Files.createDirectories(groupDirectory);
            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String safeExtension = extension == null || extension.isBlank() ? "bin" : extension.toLowerCase();
            String fileName = UUID.randomUUID() + "." + safeExtension;
            Path destination = groupDirectory.resolve(fileName).normalize();
            if (!destination.startsWith(documentUploadRoot)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid upload path");
            }
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(destination, sanitizeFileName(file.getOriginalFilename()), file.getSize());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store document");
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select a document file");
        }
        if (file.getContentType() != null && !ALLOWED_DOCUMENT_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, DOC, DOCX, TXT, or RTF files are allowed");
        }
    }

    private DocumentVisibility parseVisibility(String visibility) {
        String safeVisibility = trimToNull(visibility);
        if (safeVisibility == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document visibility is required");
        }
        try {
            return DocumentVisibility.valueOf(safeVisibility.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Visibility must be PUBLIC or PRIVATE");
        }
    }

    private String sanitizeFileName(String fileName) {
        String clean = StringUtils.hasText(fileName) ? Path.of(fileName).getFileName().toString() : "document.bin";
        return clean.replaceAll("[\\r\\n]", "_");
    }

    private String detectContentType(Path path) {
        try {
            String contentType = Files.probeContentType(path);
            return contentType == null ? "application/octet-stream" : contentType;
        } catch (IOException exception) {
            return "application/octet-stream";
        }
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

    private record StoredFile(Path path, String originalFileName, long size) {
    }

    public record DocumentDownload(Resource resource, String fileName, String contentType) {
    }
}
