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
                SET username = COALESCE(?,username), 
                mobile_number = COALESCE(?, mobile_number), 
                password = COALESCE(?,password),
                hasConsentLocation = COALESCE(?, hasConsentLocation),
                emergency_contact_name = COALESCE(?, emergency_contact_name),
                emergency_contact_number = COALESCE(?, emergency_contact_number)
                WHERE user_id = ?
                """;
        return jdbcTemplate.update(
                sql,
                user.getUsername(),
                user.getMobileNumber(),
                user.getPassword(),
                user.isHasConsentLocation(),
                user.getEmergencyContact().getName(),
                user.getEmergencyContact().getMobileNumber(),
                userId
            );
    }
}
