package com.TravelBuddy.repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.TravelBuddy.model.Itinerary;

@Repository
public class ItineraryRepository {
    private final JdbcTemplate jdbcTemplate;

    public ItineraryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int createAttraction(Itinerary itinerary) {
        String sql = """
                INSERT INTO itinerary (display_name, latitude, longitude, date, type, user_id)
                VALUES (?, ?, ?, ?, 'ATTRACTION', ?)
                """;

        return jdbcTemplate.update(
                sql,
                itinerary.getDisplayName(),
                itinerary.getLatitude(),
                itinerary.getLongitude(),
                itinerary.getDate(),
                itinerary.getUserId()
        );
    }

    public int createHotel(Itinerary itinerary) {
        String sql = """
                INSERT INTO itinerary (display_name, latitude, longitude, date, type, user_id)
                VALUES (?, ?, ?, ?, 'HOTEL', ?)
                """; 

        return jdbcTemplate.update(
                sql,
                itinerary.getDisplayName(),
                itinerary.getLatitude(),
                itinerary.getLongitude(),
                itinerary.getDate(),
                itinerary.getUserId()
        );
    }
}
