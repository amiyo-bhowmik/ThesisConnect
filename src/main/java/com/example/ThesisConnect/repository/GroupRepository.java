package com.example.ThesisConnect.repository;

import com.example.ThesisConnect.domain.RequestStatus;
import com.example.ThesisConnect.domain.RequestType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupRepository {

    private static final RowMapper<GroupRow> GROUP_ROW_MAPPER = (resultSet, rowNum) -> new GroupRow(
            resultSet.getLong("group_id"),
            resultSet.getString("topic"),
            resultSet.getString("description"),
            resultSet.getLong("admin_user_id")
    );

    private static final RowMapper<GroupMemberRow> GROUP_MEMBER_ROW_MAPPER = (resultSet, rowNum) -> new GroupMemberRow(
            resultSet.getLong("user_id"),
            resultSet.getBoolean("is_admin")
    );

    private static final RowMapper<JoinRequestRow> JOIN_REQUEST_ROW_MAPPER = (resultSet, rowNum) -> new JoinRequestRow(
            resultSet.getLong("request_id"),
            resultSet.getLong("sender_user_id"),
            getNullableLong(resultSet.getObject("recipient_user_id")),
            resultSet.getLong("group_id"),
            RequestStatus.valueOf(resultSet.getString("status")),
            RequestType.valueOf(resultSet.getString("request_type"))
    );

    private static final RowMapper<NotificationRow> NOTIFICATION_ROW_MAPPER = (resultSet, rowNum) -> new NotificationRow(
            resultSet.getLong("notification_id"),
            resultSet.getLong("user_id"),
            resultSet.getString("message"),
            resultSet.getTimestamp("timestamp").toLocalDateTime(),
            resultSet.getBoolean("is_read")
    );

    private final JdbcTemplate jdbcTemplate;

    public GroupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Long createGroup(String topic, String description, Long adminUserId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO thesis_groups (topic, description, admin_user_id)
                    VALUES (?, ?, ?)
                    """,
                    new String[]{"group_id"}
            );
            statement.setString(1, topic);
            statement.setString(2, description);
            statement.setLong(3, adminUserId);
            return statement;
        }, keyHolder);

        Number generatedKey = keyHolder.getKey();
        if (generatedKey == null) {
            throw new IllegalStateException("Could not create thesis group");
        }

        long groupId = generatedKey.longValue();
        addMember(groupId, adminUserId, true);
        return groupId;
    }

    public List<GroupRow> findAllGroups() {
        return jdbcTemplate.query(
                """
                SELECT group_id, topic, description, admin_user_id
                FROM thesis_groups
                ORDER BY LOWER(topic) ASC, group_id ASC
                """,
                GROUP_ROW_MAPPER
        );
    }

    public Optional<GroupRow> findGroupById(Long groupId) {
        List<GroupRow> groups = jdbcTemplate.query(
                """
                SELECT group_id, topic, description, admin_user_id
                FROM thesis_groups
                WHERE group_id = ?
                """,
                GROUP_ROW_MAPPER,
                groupId
        );
        return groups.isEmpty() ? Optional.empty() : Optional.of(groups.getFirst());
    }

    public List<GroupMemberRow> findGroupMembers(Long groupId) {
        return jdbcTemplate.query(
                """
                SELECT user_id, is_admin
                FROM group_members
                WHERE group_id = ?
                ORDER BY is_admin DESC, user_id ASC
                """,
                GROUP_MEMBER_ROW_MAPPER,
                groupId
        );
    }

    public boolean isMember(Long groupId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM group_members
                WHERE group_id = ? AND user_id = ?
                """,
                Integer.class,
                groupId,
                userId
        );
        return count != null && count > 0;
    }

    public boolean isAdmin(Long groupId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM group_members
                WHERE group_id = ? AND user_id = ? AND is_admin = TRUE
                """,
                Integer.class,
                groupId,
                userId
        );
        return count != null && count > 0;
    }

    public void addMember(Long groupId, Long userId, boolean isAdmin) {
        jdbcTemplate.update(
                """
                INSERT INTO group_members (group_id, user_id, is_admin)
                VALUES (?, ?, ?)
                """,
                groupId,
                userId,
                isAdmin
        );
    }

    public void setAdmin(Long groupId, Long userId, boolean isAdmin) {
        jdbcTemplate.update(
                """
                UPDATE group_members
                SET is_admin = ?
                WHERE group_id = ? AND user_id = ?
                """,
                isAdmin,
                groupId,
                userId
        );
    }

    public Optional<JoinRequestRow> findJoinRequestById(Long requestId) {
        List<JoinRequestRow> requests = jdbcTemplate.query(
                """
                SELECT request_id, sender_user_id, recipient_user_id, group_id, status, request_type
                FROM join_requests
                WHERE request_id = ?
                """,
                JOIN_REQUEST_ROW_MAPPER,
                requestId
        );
        return requests.isEmpty() ? Optional.empty() : Optional.of(requests.getFirst());
    }

    public Optional<JoinRequestRow> findPendingJoinRequest(Long senderUserId, Long groupId) {
        return findPendingRequest(senderUserId, null, groupId, RequestType.JOIN_REQUEST);
    }

    public Optional<JoinRequestRow> findPendingInvitation(Long recipientUserId, Long groupId) {
        List<JoinRequestRow> requests = jdbcTemplate.query(
                """
                SELECT request_id, sender_user_id, recipient_user_id, group_id, status, request_type
                FROM join_requests
                WHERE recipient_user_id = ?
                  AND group_id = ?
                  AND status = 'PENDING'
                  AND request_type = 'INVITATION'
                ORDER BY request_id DESC
                """,
                JOIN_REQUEST_ROW_MAPPER,
                recipientUserId,
                groupId
        );
        return requests.isEmpty() ? Optional.empty() : Optional.of(requests.getFirst());
    }

    public Optional<JoinRequestRow> findPendingInvitationBySender(Long senderUserId, Long recipientUserId, Long groupId) {
        return findPendingRequest(senderUserId, recipientUserId, groupId, RequestType.INVITATION);
    }

    private Optional<JoinRequestRow> findPendingRequest(
            Long senderUserId,
            Long recipientUserId,
            Long groupId,
            RequestType requestType
    ) {
        List<JoinRequestRow> requests;
        if (recipientUserId == null) {
            requests = jdbcTemplate.query(
                    """
                    SELECT request_id, sender_user_id, recipient_user_id, group_id, status, request_type
                    FROM join_requests
                    WHERE sender_user_id = ?
                      AND recipient_user_id IS NULL
                      AND group_id = ?
                      AND status = 'PENDING'
                      AND request_type = ?
                    ORDER BY request_id DESC
                    """,
                    JOIN_REQUEST_ROW_MAPPER,
                    senderUserId,
                    groupId,
                    requestType.name()
            );
        } else {
            requests = jdbcTemplate.query(
                    """
                    SELECT request_id, sender_user_id, recipient_user_id, group_id, status, request_type
                    FROM join_requests
                    WHERE sender_user_id = ?
                      AND recipient_user_id = ?
                      AND group_id = ?
                      AND status = 'PENDING'
                      AND request_type = ?
                    ORDER BY request_id DESC
                    """,
                    JOIN_REQUEST_ROW_MAPPER,
                    senderUserId,
                    recipientUserId,
                    groupId,
                    requestType.name()
            );
        }
        return requests.isEmpty() ? Optional.empty() : Optional.of(requests.getFirst());
    }

    public List<JoinRequestRow> findPendingJoinRequestsForGroup(Long groupId) {
        return jdbcTemplate.query(
                """
                SELECT request_id, sender_user_id, recipient_user_id, group_id, status, request_type
                FROM join_requests
                WHERE group_id = ?
                  AND status = 'PENDING'
                  AND request_type = 'JOIN_REQUEST'
                ORDER BY request_id ASC
                """,
                JOIN_REQUEST_ROW_MAPPER,
                groupId
        );
    }

    public List<JoinRequestRow> findPendingInvitationsForGroup(Long groupId) {
        return jdbcTemplate.query(
                """
                SELECT request_id, sender_user_id, recipient_user_id, group_id, status, request_type
                FROM join_requests
                WHERE group_id = ?
                  AND status = 'PENDING'
                  AND request_type = 'INVITATION'
                ORDER BY request_id ASC
                """,
                JOIN_REQUEST_ROW_MAPPER,
                groupId
        );
    }

    public List<NotificationRow> findNotificationsByUserId(Long userId) {
        return jdbcTemplate.query(
                """
                SELECT notification_id, user_id, message, timestamp, is_read
                FROM notifications
                WHERE user_id = ?
                ORDER BY timestamp DESC, notification_id DESC
                """,
                NOTIFICATION_ROW_MAPPER,
                userId
        );
    }

    public long createJoinRequest(
            Long senderUserId,
            Long recipientUserId,
            Long groupId,
            RequestType requestType
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO join_requests (
                        sender_user_id, recipient_user_id, group_id, status, request_type
                    ) VALUES (?, ?, ?, ?, ?)
                    """,
                    new String[]{"request_id"}
            );
            statement.setLong(1, senderUserId);
            if (recipientUserId == null) {
                statement.setObject(2, null);
            } else {
                statement.setLong(2, recipientUserId);
            }
            statement.setLong(3, groupId);
            statement.setString(4, RequestStatus.PENDING.name());
            statement.setString(5, requestType.name());
            return statement;
        }, keyHolder);

        Number generatedKey = keyHolder.getKey();
        if (generatedKey == null) {
            throw new IllegalStateException("Could not create join request");
        }
        return generatedKey.longValue();
    }

    public void updateJoinRequestStatus(Long requestId, RequestStatus status, Long reviewedByUserId) {
        jdbcTemplate.update(
                """
                UPDATE join_requests
                SET status = ?, resolved_at = ?, reviewed_by_user_id = ?
                WHERE request_id = ?
                """,
                status.name(),
                LocalDateTime.now(),
                reviewedByUserId,
                requestId
        );
    }

    public void rejectOtherPendingRequestsForUserInGroup(Long requestId, Long groupId, Long userId) {
        jdbcTemplate.update(
                """
                UPDATE join_requests
                SET status = 'REJECTED', resolved_at = ?
                WHERE group_id = ?
                  AND status = 'PENDING'
                  AND request_id <> ?
                  AND (sender_user_id = ? OR recipient_user_id = ?)
                """,
                LocalDateTime.now(),
                groupId,
                requestId,
                userId,
                userId
        );
    }

    public void createNotification(Long userId, String message) {
        jdbcTemplate.update(
                """
                INSERT INTO notifications (user_id, message)
                VALUES (?, ?)
                """,
                userId,
                message
        );
    }

    private static Long getNullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    public record GroupRow(
            Long groupId,
            String topic,
            String description,
            Long adminUserId
    ) {
    }

    public record GroupMemberRow(
            Long userId,
            boolean isAdmin
    ) {
    }

    public record JoinRequestRow(
            Long requestId,
            Long senderUserId,
            Long recipientUserId,
            Long groupId,
            RequestStatus status,
            RequestType requestType
    ) {
    }

    public record NotificationRow(
            Long notificationId,
            Long userId,
            String message,
            LocalDateTime timestamp,
            boolean isRead
    ) {
    }
}
