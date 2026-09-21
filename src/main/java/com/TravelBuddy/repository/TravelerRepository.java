package com.TravelBuddy.repository;
import com.TravelBuddy.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TravelerRepository {
     private final JdbcTemplate jdbcTemplate;

    public TravelerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
                user.isHasConsentLocation()
        );
    }

    public User authenticateUser(String username, String password) {
    String sql = """
            SELECT user_id, username, mobile_number, password, role, has_consent_location
            FROM users
            WHERE username = ? AND password = ?
            """;

    return jdbcTemplate.query(
            sql,
            new Object[]{username, password},
            rs -> {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setMobileNumber(rs.getString("mobile_number"));
                    user.setPassword(rs.getString("password"));
                    user.setRole(rs.getString("role"));
                    user.setHasConsentLocation(rs.getBoolean("has_consent_location"));
                    return user;
                }
                return null;
            }
    );
}
}
