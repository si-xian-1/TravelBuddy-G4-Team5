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
        // Requires the emergency contact columns in the users table.
        // Never select the password for this endpoint.
        String sql = """
                SELECT user_id, username, mobileNumber, role, hasConsentLocation,
                       emergency_contact_name, emergency_contact_number
                FROM users
                WHERE user_id = ? AND role = 'TRAVELER'
                """;
        List<User> users = jdbcTemplate.query(
                sql, (rs, rowNum) -> {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setMobileNumber(rs.getString("mobileNumber"));
                user.setRole(rs.getString("role"));
                user.setHasConsentLocation(rs.getBoolean("hasConsentLocation"));
                String contactName = rs.getString("emergency_contact_name");
                String contactNumber = rs.getString("emergency_contact_number");
                if (contactName != null || contactNumber != null) {
                    user.setEmergencyContact(contactName, contactNumber);
                }
                return user;
            }, userId);
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
                WHERE user_id = ? AND role = 'TRAVELER'
                """;
        return jdbcTemplate.update(
                sql,
                user.getUsername(),
                user.getMobileNumber(),
                user.getPassword(),
                user.isHasConsentLocation(),
                user.getEmergencyContact() == null ? null : user.getEmergencyContact().getName(),
                user.getEmergencyContact() == null ? null : user.getEmergencyContact().getMobileNumber(),
                userId
            );
    }
}
