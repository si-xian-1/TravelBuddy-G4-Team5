package com.TravelBuddy.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.TravelBuddy.model.Attraction;

@Repository 
public class AttractionRepository {
    private final JdbcTemplate jdbcTemplate;

    public AttractionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get all attraction in an itinerary
     * @param itineraryId
     * @return all attraction
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
            attraction.setVisitDate(result.getTimestamp("visit_date").toLocalDateTime());

            return attraction;
        }, itineraryId);
    }

    /**
     * Get a detailed specific attraction based on itinerary id and selected attraction to view
     */
    public Attraction findSelectedAttractionByItineraryId(int attractionId, int itineraryId) {
        String sql = """
                SELECT attraction_id, itinerary_id, attraction_name, latitude, longitude, visit_date
                FROM attractions WHERE attraction_id = ? AND itinerary_id = ?
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
            attraction.setVisitDate(result.getTimestamp("visit_date").toLocalDateTime());

            return attraction;
        }, itineraryId);
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

}
