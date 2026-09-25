package com.TravelBuddy.repository;

import com.TravelBuddy.model.User;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TravelerRepository {

    private final JdbcTemplate jdbcTemplate;

    public TravelerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User getUser(int userId) {
        // Never select the password for this endpoint.
        String sql = """
                SELECT user_id, username, mobile_number, role, has_consent_location,
                       emergency_contact_name, emergency_contact_number
                FROM users
                WHERE user_id = ? AND role = 'TRAVELER'
                """;

        List<User> users = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setMobileNumber(rs.getString("mobile_number"));
                    user.setRole(rs.getString("role"));
                    user.setHasConsentLocation(rs.getBoolean("has_consent_location"));
                    user.setEmergencyContactName(rs.getString("emergency_contact_name"));
                    user.setEmergencyContactMobileNumber(rs.getString("emergency_contact_number"));

                    return user;
                },
                userId
        );

        return users.isEmpty() ? null : users.get(0);
    }

    public User findCredentialsByUsername(String username) {
        String sql = """
                SELECT user_id, username, password
                FROM users
                WHERE username = ? AND role = 'TRAVELER'
                """;

        List<User> users = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    return user;
                },
                username
        );

        return users.isEmpty() ? null : users.get(0);
    }

    public int createUser(User user) {
        String sql = """
                INSERT INTO users (username, mobile_number, password, role, has_consent_location)
                VALUES (?, ?, ?, 'TRAVELER', ?)
                """;

        return jdbcTemplate.update(
                sql,
                user.getUsername(),
                user.getMobileNumber(),
                user.getPassword(),
                user.getHasConsentLocation()
        );
    }

    public boolean isUsernameTakenByOtherUser(String username, int userId) {
        String sql = """
                SELECT COUNT(*) FROM users
                WHERE username = ? AND user_id <> ?
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username, userId);
        return count != null && count > 0;
    }

    public int updateUser(int userId, User user) {
        String sql = """
                UPDATE users
                SET username = COALESCE(?, username),
                    mobile_number = COALESCE(?, mobile_number),
                    password = COALESCE(?, password),
                    has_consent_location = COALESCE(?, has_consent_location),
                    emergency_contact_name = ?,
                    emergency_contact_number = ?
                WHERE user_id = ? AND role = 'TRAVELER'
                """;

        return jdbcTemplate.update(
                sql,
                user.getUsername(),
                user.getMobileNumber(),
                user.getPassword(),
                user.getHasConsentLocation(),
                user.getEmergencyContactName(),
                user.getEmergencyContactMobileNumber(),
                userId
        );
    }

    public int deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ? AND role = 'TRAVELER'";
        return jdbcTemplate.update(sql, userId);
    }
}