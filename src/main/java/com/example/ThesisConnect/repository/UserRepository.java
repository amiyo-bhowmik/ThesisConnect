package com.example.ThesisConnect.repository;

import com.example.ThesisConnect.domain.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final RowMapper<User> USER_ROW_MAPPER = (resultSet, rowNum) -> {
        User user = new User();
        user.setUserId(resultSet.getLong("user_id"));
        user.setName(resultSet.getString("name"));
        user.setEmail(resultSet.getString("email"));
        user.setPassword(resultSet.getString("password"));
        user.setDepartment(resultSet.getString("department"));
        user.setUniversity(resultSet.getString("university"));
        user.setAcademicDetails(resultSet.getString("academic_details"));
        user.setBio(resultSet.getString("bio"));
        user.setProfilePicture(resultSet.getString("profile_picture"));
        user.setLookingForGroup(resultSet.getBoolean("is_looking_for_group"));
        return user;
    };

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByEmail(String email) {
        List<User> users = jdbcTemplate.query(
                """
                SELECT user_id, name, email, password, department, university,
                       academic_details, bio, profile_picture, is_looking_for_group
                FROM users
                WHERE email = ?
                """,
                USER_ROW_MAPPER,
                email
        );

        if (users.isEmpty()) {
            return Optional.empty();
        }

        User user = users.getFirst();
        loadCollections(user);
        return Optional.of(user);
    }

    public Optional<User> findById(Long userId) {
        List<User> users = jdbcTemplate.query(
                """
                SELECT user_id, name, email, password, department, university,
                       academic_details, bio, profile_picture, is_looking_for_group
                FROM users
                WHERE user_id = ?
                """,
                USER_ROW_MAPPER,
                userId
        );

        if (users.isEmpty()) {
            return Optional.empty();
        }

        User user = users.getFirst();
        loadCollections(user);
        return Optional.of(user);
    }

    @Transactional
    public User save(User user) {
        if (user.getUserId() == null) {
            insertUser(user);
        } else {
            updateUser(user);
        }

        replaceCollection(
                "DELETE FROM user_research_interests WHERE user_id = ?",
                "INSERT INTO user_research_interests (user_id, interest) VALUES (?, ?)",
                user.getUserId(),
                user.getResearchInterests()
        );
        replaceCollection(
                "DELETE FROM user_skills WHERE user_id = ?",
                "INSERT INTO user_skills (user_id, skill) VALUES (?, ?)",
                user.getUserId(),
                user.getSkills()
        );

        return findByEmail(user.getEmail())
                .orElseThrow(() -> new IllegalStateException("Saved user could not be reloaded"));
    }

    private void insertUser(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO users (
                        name, email, password, department, university,
                        academic_details, bio, profile_picture, is_looking_for_group
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getDepartment());
            statement.setString(5, user.getUniversity());
            statement.setString(6, user.getAcademicDetails());
            statement.setString(7, user.getBio());
            statement.setString(8, user.getProfilePicture());
            statement.setBoolean(9, user.isLookingForGroup());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not generate user id");
        }
        user.setUserId(key.longValue());
    }

    private void updateUser(User user) {
        jdbcTemplate.update(
                """
                UPDATE users
                SET name = ?, email = ?, password = ?, department = ?, university = ?,
                    academic_details = ?, bio = ?, profile_picture = ?, is_looking_for_group = ?
                WHERE user_id = ?
                """,
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getDepartment(),
                user.getUniversity(),
                user.getAcademicDetails(),
                user.getBio(),
                user.getProfilePicture(),
                user.isLookingForGroup(),
                user.getUserId()
        );
    }

    private void loadCollections(User user) {
        user.setResearchInterests(readCollection(
                "SELECT interest FROM user_research_interests WHERE user_id = ? ORDER BY id",
                user.getUserId()
        ));
        user.setSkills(readCollection(
                "SELECT skill FROM user_skills WHERE user_id = ? ORDER BY id",
                user.getUserId()
        ));
    }

    private List<String> readCollection(String sql, Long userId) {
        return new ArrayList<>(jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> resultSet.getString(1),
                userId
        ));
    }

    private void replaceCollection(String deleteSql, String insertSql, Long userId, List<String> values) {
        jdbcTemplate.update(deleteSql, userId);
        List<String> safeValues = values == null ? List.of() : values;
        if (safeValues.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                insertSql,
                safeValues,
                safeValues.size(),
                (preparedStatement, value) -> {
                    preparedStatement.setLong(1, userId);
                    preparedStatement.setString(2, value);
                }
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String toContainsQuery(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
