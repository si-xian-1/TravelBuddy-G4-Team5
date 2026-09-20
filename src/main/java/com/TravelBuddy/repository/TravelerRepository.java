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
                INSERT INTO users (username, mobileNumber, password,role, has_consent_location)
                VALUES (?, ?, ?, 'TRAVELER',?)
                """;

        return jdbcTemplate.update(
                sql,
                user.getUsername(),
                user.getMobileNumber(),
                user.getPassword(),
                user.isHasConsentLocation()
        );
    }
}
