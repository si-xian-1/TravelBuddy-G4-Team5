package com.TravelBuddy;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;

/**
 * Integration tests verifying Spring Boot application context,
 * in-memory H2 database connectivity, Flyway schema migrations,
 * and repository data operations
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TravelBuddyApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TravelerRepository travelerRepository;

    @Test
    @DisplayName("Verify connection with H2 database")
    void contextLoads_withH2Database() throws SQLException {
        // Arrange (Given)
        // Spring Boot test context initialized with DataSource bean

        // Act (When)
        Connection connection = dataSource.getConnection();

        // Assert (Then)
        assertNotNull(connection, "Database connection should not be null");
        assertTrue(connection.getMetaData().getDatabaseProductName().toLowerCase().contains("h2"),
                "Connection should be backed by H2 in-memory database");
        connection.close();
    }

    @Test
    @DisplayName("Verify seeded user data exists in H2 database")
    void flywayMigration_seedsInitialTraveler() {
        // Arrange (Given)
        int seededUserId = 1;

        // Act (When)
        User user = travelerRepository.getUser(seededUserId);

        // Assert (Then)
        assertNotNull(user, "Seeded traveler should exist in database");
        assertEquals("albert", user.getUsername());
        assertEquals("TRAVELER", user.getRole());
    }

    @Test
    @DisplayName("Verify seeded shelter data exists in H2 database")
    void flywayMigration_seedsSafeShelters() {
        // Arrange (Given)
        String countSql = "SELECT COUNT(*) FROM safe_shelter";

        // Act (When)
        Integer shelterCount = jdbcTemplate.queryForObject(countSql, Integer.class);

        // Assert (Then)
        assertNotNull(shelterCount, "Shelter count should not be null");
        assertTrue(shelterCount > 0, "Safe shelters should be pre-seeded by Flyway");
    }
}
