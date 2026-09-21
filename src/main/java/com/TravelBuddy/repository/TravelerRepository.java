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
    public int updateUser(int userId, User user){
        String sql = """
                UPDATE users 
                SET username = ?, 
                mobile_number = ?, 
                hasConsentLocation = ? 
                WHERE user_id = ?
                """;
        return jdbcTemplate.update(
                sql,
                user.getUsername(),
                user.getMobileNumber(),
                user.isHasConsentLocation(),
                userId
            );
    }
}
