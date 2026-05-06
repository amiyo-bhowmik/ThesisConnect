package com.example.ThesisConnect.repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ThesisConnect.domain.DocumentVisibility;

@Repository
public class DocumentRepository {

    private static final RowMapper<DocumentRow> DOCUMENT_ROW_MAPPER = (resultSet, rowNum) -> new DocumentRow(
            resultSet.getLong("document_id"),
            resultSet.getString("title"),
            resultSet.getString("file_path"),
            resultSet.getString("original_file_name"),
            DocumentVisibility.valueOf(resultSet.getString("visibility")),
            resultSet.getLong("uploaded_by_user_id"),
            resultSet.getLong("group_id"),
            resultSet.getTimestamp("upload_date").toLocalDateTime(),
            resultSet.getInt("version"),
            resultSet.getLong("file_size")
    );

    private static final RowMapper<DocumentVersionRow> DOCUMENT_VERSION_ROW_MAPPER = (resultSet, rowNum) -> new DocumentVersionRow(
            resultSet.getLong("version_id"),
            resultSet.getLong("document_id"),
            resultSet.getInt("version_number"),
            resultSet.getString("file_path"),
            resultSet.getString("original_file_name"),
            resultSet.getLong("file_size"),
            resultSet.getLong("uploaded_by_user_id"),
            resultSet.getTimestamp("uploaded_at").toLocalDateTime()
    );

    private static final RowMapper<CommentRow> COMMENT_ROW_MAPPER = (resultSet, rowNum) -> new CommentRow(
            resultSet.getLong("comment_id"),
            resultSet.getString("content"),
            resultSet.getLong("author_user_id"),
            resultSet.getLong("document_id"),
            resultSet.getTimestamp("timestamp").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public DocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createDocument(
            String title,
            String filePath,
            String originalFileName,
            DocumentVisibility visibility,
            Long uploadedByUserId,
            Long groupId,
            long fileSize
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO documents (
                        title, file_path, original_file_name, visibility,
                        uploaded_by_user_id, group_id, version, file_size
                    ) VALUES (?, ?, ?, ?, ?, ?, 1, ?)
                    """,
                    new String[]{"document_id"}
            );
            statement.setString(1, title);
            statement.setString(2, filePath);
            statement.setString(3, originalFileName);
            statement.setString(4, visibility.name());
            statement.setLong(5, uploadedByUserId);
            statement.setLong(6, groupId);
            statement.setLong(7, fileSize);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not create document");
        }
        return key.longValue();
    }

    public long createDocumentVersion(
            Long documentId,
            int versionNumber,
            String filePath,
            String originalFileName,
            long fileSize,
            Long uploadedByUserId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO document_versions (
                        document_id, version_number, file_path, original_file_name,
                        file_size, uploaded_by_user_id
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    new String[]{"version_id"}
            );
            statement.setLong(1, documentId);
            statement.setInt(2, versionNumber);
            statement.setString(3, filePath);
            statement.setString(4, originalFileName);
            statement.setLong(5, fileSize);
            statement.setLong(6, uploadedByUserId);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not create document version");
        }
        return key.longValue();
    }

    public void updateDocumentLatestVersion(
            Long documentId,
            String filePath,
            String originalFileName,
            int version,
            long fileSize
    ) {
        jdbcTemplate.update(
                """
                UPDATE documents
                SET file_path = ?, original_file_name = ?, version = ?, file_size = ?
                WHERE document_id = ?
                """,
                filePath,
                originalFileName,
                version,
                fileSize,
                documentId
        );
    }

    public Optional<DocumentRow> findDocumentById(Long documentId) {
        List<DocumentRow> rows = jdbcTemplate.query(
                """
                SELECT document_id, title, file_path, original_file_name, visibility,
                       uploaded_by_user_id, group_id, upload_date, version, file_size
                FROM documents
                WHERE document_id = ?
                """,
                DOCUMENT_ROW_MAPPER,
                documentId
        );
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    public List<DocumentRow> findVisibleDocumentsByGroupId(Long groupId, Long userId) {
        return jdbcTemplate.query(
                """
                SELECT d.document_id, d.title, d.file_path, d.original_file_name, d.visibility,
                       d.uploaded_by_user_id, d.group_id, d.upload_date, d.version, d.file_size
                FROM documents d
                WHERE d.group_id = ?
                  AND (
                        d.visibility = 'PUBLIC'
                        OR EXISTS (
                            SELECT 1
                            FROM group_members gm
                            WHERE gm.group_id = d.group_id
                              AND gm.user_id = ?
                        )
                  )
                ORDER BY d.upload_date DESC, d.document_id DESC
                """,
                DOCUMENT_ROW_MAPPER,
                groupId,
                userId
        );
    }

    public List<DocumentVersionRow> findVersionsByDocumentId(Long documentId) {
        return jdbcTemplate.query(
                """
                SELECT version_id, document_id, version_number, file_path, original_file_name,
                       file_size, uploaded_by_user_id, uploaded_at
                FROM document_versions
                WHERE document_id = ?
                ORDER BY version_number DESC
                """,
                DOCUMENT_VERSION_ROW_MAPPER,
                documentId
        );
    }

    public Optional<DocumentVersionRow> findVersionByDocumentIdAndNumber(Long documentId, int versionNumber) {
        List<DocumentVersionRow> rows = jdbcTemplate.query(
                """
                SELECT version_id, document_id, version_number, file_path, original_file_name,
                       file_size, uploaded_by_user_id, uploaded_at
                FROM document_versions
                WHERE document_id = ? AND version_number = ?
                """,
                DOCUMENT_VERSION_ROW_MAPPER,
                documentId,
                versionNumber
        );
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    public List<CommentRow> findCommentsByDocumentId(Long documentId) {
        return jdbcTemplate.query(
                """
                SELECT comment_id, content, author_user_id, document_id, timestamp
                FROM comments
                WHERE document_id = ?
                ORDER BY timestamp ASC, comment_id ASC
                """,
                COMMENT_ROW_MAPPER,
                documentId
        );
    }

    public long createComment(String content, Long authorUserId, Long documentId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO comments (content, author_user_id, document_id)
                    VALUES (?, ?, ?)
                    """,
                    new String[]{"comment_id"}
            );
            statement.setString(1, content);
            statement.setLong(2, authorUserId);
            statement.setLong(3, documentId);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not create comment");
        }
        return key.longValue();
    }

    public record DocumentRow(
            Long documentId,
            String title,
            String filePath,
            String originalFileName,
            DocumentVisibility visibility,
            Long uploadedByUserId,
            Long groupId,
            LocalDateTime uploadDate,
            int version,
            long fileSize
    ) {
    }

    public record DocumentVersionRow(
            Long versionId,
            Long documentId,
            int versionNumber,
            String filePath,
            String originalFileName,
            long fileSize,
            Long uploadedByUserId,
            LocalDateTime uploadedAt
    ) {
    }

    public record CommentRow(
            Long commentId,
            String content,
            Long authorUserId,
            Long documentId,
            LocalDateTime timestamp
    ) {
    }
}
