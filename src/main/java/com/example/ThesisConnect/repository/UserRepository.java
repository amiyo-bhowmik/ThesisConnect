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

    public List<User> searchStudents(
            Long currentUserId,
            String name,
            String email,
            String researchInterest,
            String department,
            String university,
            Boolean lookingForGroupOnly
    ) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT user_id, name, email, password, department, university,
                       academic_details, bio, profile_picture, is_looking_for_group
                FROM users u
                WHERE u.user_id <> ?
                """
        );
        List<Object> params = new ArrayList<>();
        params.add(currentUserId);

        if (hasText(name)) {
            sql.append(" AND LOWER(COALESCE(u.name, '')) LIKE ? ");
            params.add(toContainsQuery(name));
        }

        if (hasText(email)) {
            sql.append(" AND LOWER(COALESCE(u.email, '')) LIKE ? ");
            params.add(toContainsQuery(email));
        }

        if (hasText(researchInterest)) {
            sql.append(
                    """
                     AND EXISTS (
                        SELECT 1
                        FROM user_research_interests uri
                        WHERE uri.user_id = u.user_id
                          AND LOWER(uri.interest) LIKE ?
                    )
                    """
            );
            params.add(toContainsQuery(researchInterest));
        }

        if (hasText(department)) {
            sql.append(" AND LOWER(COALESCE(u.department, '')) LIKE ? ");
            params.add(toContainsQuery(department));
        }

        if (hasText(university)) {
            sql.append(" AND LOWER(COALESCE(u.university, '')) LIKE ? ");
            params.add(toContainsQuery(university));
        }

        if (Boolean.TRUE.equals(lookingForGroupOnly)) {
            sql.append(" AND u.is_looking_for_group = ? ");
            params.add(true);
        }

        sql.append(" ORDER BY u.is_looking_for_group DESC, LOWER(u.name) ASC ");

        List<User> users = jdbcTemplate.query(sql.toString(), USER_ROW_MAPPER, params.toArray());
        users.forEach(this::loadCollections);
        users.sort(Comparator.comparing(User::isLookingForGroup).reversed().thenComparing(User::getName, String.CASE_INSENSITIVE_ORDER));
        return users;
    }

    public boolean existsByEmailAndUserIdNot(String email, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM users
                WHERE email = ? AND user_id <> ?
                """,
                Integer.class,
                email,
                userId
        );
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM users
                WHERE email = ?
                """,
                Integer.class,
                email
        );
        return count != null && count > 0;
    }

    public void deleteById(Long userId) {
        jdbcTemplate.update(
                """
                DELETE FROM users
                WHERE user_id = ?
                """,
                userId
        );
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
