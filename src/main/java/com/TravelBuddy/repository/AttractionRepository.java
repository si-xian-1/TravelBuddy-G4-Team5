package com.TravelBuddy.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.TravelBuddy.model.Attraction;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class AttractionRepository {
    private final JdbcTemplate jdbcTemplate;

    public AttractionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get all attractions belonging to an itinerary of the user
     * @param itineraryId
     * @return all attractions without the detailed information
     */
    public List<Attraction> findAllAttractionsByItineraryId(int itineraryId) {
        String sql = """
                SELECT attraction_id, itinerary_id, attraction_name, latitude, longitude, visit_date
                FROM attractions WHERE itinerary_id = ?
                """;

        return jdbcTemplate.query(
                sql,
                (result, rowNum) -> {
            Attraction attraction = new Attraction();

            attraction.setAttractionId(result.getInt("attraction_id"));
            attraction.setItineraryId(result.getInt("itinerary_id"));
            attraction.setAttractionName(result.getString("attraction_name"));
            attraction.setLatitude(result.getDouble("latitude"));
            attraction.setLongitude(result.getDouble("longitude"));
            attraction.setVisitDate(result.getObject("visit_date", LocalDateTime.class));

            return attraction;
        }, itineraryId);
    }

    /**
     * Find specfic attraction belonging to an itinerary of the user
     * @param itineraryId
     * @param attractionId
     * @return attraction matching attractionId
     */
    public Attraction findSelectedAttractionByAttractionId(int itineraryId, int attractionId) {
        String sql = """
                SELECT attraction_id, itinerary_id, attraction_name, latitude, longitude, visit_date
                FROM attractions WHERE itinerary_id = ?, attraction_id = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (result, rowNum) -> {
            Attraction attraction = new Attraction();

            attraction.setAttractionId(result.getInt("attraction_id"));
            attraction.setItineraryId(result.getInt("itinerary_id"));
            attraction.setAttractionName(result.getString("attraction_name"));
            attraction.setLatitude(result.getDouble("latitude"));
            attraction.setLongitude(result.getDouble("longitude"));
            attraction.setVisitDate(result.getObject("visit_date", LocalDateTime.class));

            return attraction;
        }, itineraryId, attractionId);
    }

    /**
     * Add attractions to visit into the itinerary
     * 
     * @param itineraryId
     * @param attraction
     * @return newly created attraction details
     */
    public Attraction createAttraction(int itineraryId, Attraction attraction) {
        String sql = """
                INSERT INTO attractions (itinerary_id, attraction_name, latitude, longitude, visit_date)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[] { "attraction_id" });
            statement.setInt(1, itineraryId);
            statement.setString(2, attraction.getAttractionName());
            statement.setDouble(3, attraction.getLatitude());
            statement.setDouble(4, attraction.getLongitude());
            statement.setTimestamp(5, Timestamp.valueOf(attraction.getVisitDate()));
            return statement;
        }, keyHolder);

        // Retrieve attraction id of the newly created attraction from database
        attraction.setAttractionId(keyHolder.getKey().intValue());
        attraction.setItineraryId(itineraryId);
        
        return attraction;
    }

    /**
     * Ensure only user can edit their own itinerary
     * @param itineraryId
     * @param userId
     * @return true or false
     */
    public boolean verifyItineraryAndUser(int itineraryId, int userId) {
        String sql = "SELECT COUNT(*) FROM itineraries WHERE itinerary_id = ? AND user_id = ?";

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                itineraryId,
                userId);
        
        return count != null && count > 0;
    }

    /**
     * Update attraction in an itinerary of the user
     * @param itineraryId
     * @param attractionId
     * @param attraction
     * @return number of updated rows in attractions
     */
    public int updateAttraction(int itineraryId, int attractionId, Attraction attraction) {
        String sql = """
                UPDATE attractions
                SET attraction_name = COALESCE(?, attraction_name),
                    latitude = COALESCE(?, latitude),
                    longitude = COALESCE(?, longitude),
                    visit_date = COALESCE(?, visit_date),
                WHERE attraction_id = ? AND itinerary_id = ?
                """;

        return jdbcTemplate.update(
                sql,
                attraction.getAttractionName(),
                attraction.getLatitude(),
                attraction.getLongitude(),
                attraction.getVisitDate(),
                attractionId,
                itineraryId
        );
    }

    /**
     * Delete attraction in an itinerary of the user
     * @param itineraryId
     * @param attractionId
     * @return number of updated rows in attractions
     */
    public int deleteAttraction(int itineraryId, int attractionId) {
        String sql = "DELETE FROM attractions WHERE attraction_id = ? AND itinerary_id = ?";
        return jdbcTemplate.update(sql, attractionId, itineraryId);
    }
}
