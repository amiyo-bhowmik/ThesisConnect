package com.example.ThesisConnect.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class CommunicationRepository {

    private static final RowMapper<DirectMessageRow> DIRECT_MESSAGE_ROW_MAPPER = (resultSet, rowNum) -> new DirectMessageRow(
            resultSet.getLong("message_id"),
            resultSet.getLong("sender_user_id"),
            resultSet.getLong("receiver_user_id"),
            resultSet.getString("content"),
            resultSet.getTimestamp("timestamp").toLocalDateTime(),
            resultSet.getBoolean("is_pinned")
    );

    private static final RowMapper<GroupMessageRow> GROUP_MESSAGE_ROW_MAPPER = (resultSet, rowNum) -> new GroupMessageRow(
            resultSet.getLong("message_id"),
            resultSet.getLong("sender_user_id"),
            resultSet.getLong("group_id"),
            resultSet.getString("content"),
            resultSet.getTimestamp("timestamp").toLocalDateTime(),
            resultSet.getBoolean("is_pinned")
    );

    private final JdbcTemplate jdbcTemplate;

    public CommunicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createDirectMessage(Long senderUserId, Long receiverUserId, String content) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO direct_messages (sender_user_id, receiver_user_id, content)
                    VALUES (?, ?, ?)
                    """,
                    new String[]{"message_id"}
            );
            statement.setLong(1, senderUserId);
            statement.setLong(2, receiverUserId);
            statement.setString(3, content);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not create direct message");
        }
        return key.longValue();
    }

    public long createGroupMessage(Long senderUserId, Long groupId, String content) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO group_messages (sender_user_id, group_id, content)
                    VALUES (?, ?, ?)
                    """,
                    new String[]{"message_id"}
            );
            statement.setLong(1, senderUserId);
            statement.setLong(2, groupId);
            statement.setString(3, content);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not create group message");
        }
        return key.longValue();
    }

    public List<DirectMessageRow> findConversation(Long currentUserId, Long otherUserId) {
        return jdbcTemplate.query(
                """
                SELECT dm.message_id,
                       dm.sender_user_id,
                       dm.receiver_user_id,
                       dm.content,
                       dm.timestamp,
                       EXISTS (
                           SELECT 1
                           FROM direct_message_pins dmp
                           WHERE dmp.user_id = ?
                             AND dmp.message_id = dm.message_id
                       ) AS is_pinned
                FROM direct_messages dm
                WHERE (dm.sender_user_id = ? AND dm.receiver_user_id = ?)
                   OR (dm.sender_user_id = ? AND dm.receiver_user_id = ?)
                ORDER BY dm.timestamp ASC, dm.message_id ASC
                """,
                DIRECT_MESSAGE_ROW_MAPPER,
                currentUserId,
                currentUserId,
                otherUserId,
                otherUserId,
                currentUserId
        );
    }

    public Optional<DirectMessageRow> findDirectMessageById(Long messageId, Long currentUserId) {
        List<DirectMessageRow> rows = jdbcTemplate.query(
                """
                SELECT dm.message_id,
                       dm.sender_user_id,
                       dm.receiver_user_id,
                       dm.content,
                       dm.timestamp,
                       EXISTS (
                           SELECT 1
                           FROM direct_message_pins dmp
                           WHERE dmp.user_id = ?
                             AND dmp.message_id = dm.message_id
                       ) AS is_pinned
                FROM direct_messages dm
                WHERE dm.message_id = ?
                """,
                DIRECT_MESSAGE_ROW_MAPPER,
                currentUserId,
                messageId
        );
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    public List<GroupMessageRow> findGroupMessages(Long groupId, Long currentUserId) {
        return jdbcTemplate.query(
                """
                SELECT gm.message_id,
                       gm.sender_user_id,
                       gm.group_id,
                       gm.content,
                       gm.timestamp,
                       EXISTS (
                           SELECT 1
                           FROM group_message_pins gmp
                           WHERE gmp.user_id = ?
                             AND gmp.message_id = gm.message_id
                       ) AS is_pinned
                FROM group_messages gm
                WHERE gm.group_id = ?
                ORDER BY gm.timestamp ASC, gm.message_id ASC
                """,
                GROUP_MESSAGE_ROW_MAPPER,
                currentUserId,
                groupId
        );
    }

    public Optional<GroupMessageRow> findGroupMessageById(Long messageId, Long currentUserId) {
        List<GroupMessageRow> rows = jdbcTemplate.query(
                """
                SELECT gm.message_id,
                       gm.sender_user_id,
                       gm.group_id,
                       gm.content,
                       gm.timestamp,
                       EXISTS (
                           SELECT 1
                           FROM group_message_pins gmp
                           WHERE gmp.user_id = ?
                             AND gmp.message_id = gm.message_id
                       ) AS is_pinned
                FROM group_messages gm
                WHERE gm.message_id = ?
                """,
                GROUP_MESSAGE_ROW_MAPPER,
                currentUserId,
                messageId
        );
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    public void pinDirectMessage(Long userId, Long messageId) {
        jdbcTemplate.update(
                """
                INSERT INTO direct_message_pins (user_id, message_id)
                SELECT ?, ?
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM direct_message_pins
                    WHERE user_id = ? AND message_id = ?
                )
                """,
                userId,
                messageId,
                userId,
                messageId
        );
    }

    public void unpinDirectMessage(Long userId, Long messageId) {
        jdbcTemplate.update(
                """
                DELETE FROM direct_message_pins
                WHERE user_id = ? AND message_id = ?
                """,
                userId,
                messageId
        );
    }

    public void pinGroupMessage(Long userId, Long messageId) {
        jdbcTemplate.update(
                """
                INSERT INTO group_message_pins (user_id, message_id)
                SELECT ?, ?
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM group_message_pins
                    WHERE user_id = ? AND message_id = ?
                )
                """,
                userId,
                messageId,
                userId,
                messageId
        );
    }

    public void unpinGroupMessage(Long userId, Long messageId) {
        jdbcTemplate.update(
                """
                DELETE FROM group_message_pins
                WHERE user_id = ? AND message_id = ?
                """,
                userId,
                messageId
        );
    }

    public record DirectMessageRow(
            Long messageId,
            Long senderUserId,
            Long receiverUserId,
            String content,
            LocalDateTime timestamp,
            boolean pinned
    ) {
    }

    public record GroupMessageRow(
            Long messageId,
            Long senderUserId,
            Long groupId,
            String content,
            LocalDateTime timestamp,
            boolean pinned
    ) {
    }
}
