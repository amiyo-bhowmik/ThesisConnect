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