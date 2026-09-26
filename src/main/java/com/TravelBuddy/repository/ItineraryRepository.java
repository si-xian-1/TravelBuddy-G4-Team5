package com.TravelBuddy.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.TravelBuddy.model.*;

@Repository
public class ItineraryRepository {
    private final JdbcTemplate jdbcTemplate;
    private final HotelRepository hotelRepository;
    private final AttractionRepository attractionRepository;

    public ItineraryRepository(JdbcTemplate jdbcTemplate, HotelRepository hotelRepository, AttractionRepository attractionRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.hotelRepository = hotelRepository;
        this.attractionRepository = attractionRepository;
    }

    /**
     * Get all itinerary belonging to the user
     * @param userId
     * @return all itinerary without the detailed information
     */
    public List<Itinerary> findAllItinerariesByUserId(int userId) {
        String sql = """
                SELECT itinerary_id, itinerary_name, start_date, end_date, user_id
                FROM itineraries WHERE user_id = ?
                """;

        return jdbcTemplate.query(
                sql,
                (result, rowNum) -> {
            Itinerary itinerary = new Itinerary();

            itinerary.setItineraryId(result.getInt("itinerary_id"));
            itinerary.setItineraryName(result.getString("itinerary_name"));
            itinerary.setStartDate(result.getDate("start_date").toLocalDate());
            itinerary.setEndDate(result.getDate("end_date").toLocalDate());
            itinerary.setUserId(result.getInt("user_id"));

            return itinerary;
        }, userId);
    }

    /**
     * Get a detailed specific itinerary based on user id and selected itinerary to view
     * 
     * @param itineraryId
     * @param userId
     * @return selected itinerary
     */
    public Itinerary findSelectedItineraryByUserId(int itineraryId, int userId) {
        String itinerary_sql = """
                SELECT itinerary_id, itinerary_name, start_date, end_date, user_id
                FROM itineraries WHERE itinerary_id = ? AND user_id = ?
                """;

        // Get the specific itinerary
        Itinerary itinerary = jdbcTemplate.queryForObject(
                itinerary_sql,
                (result, rowNum) -> {
            Itinerary value = new Itinerary();
            value.setItineraryId(result.getInt("itinerary_id"));
            value.setItineraryName(result.getString("itinerary_name"));
            value.setStartDate(result.getDate("start_date").toLocalDate());
            value.setEndDate(result.getDate("end_date").toLocalDate());
            value.setUserId(result.getInt("user_id"));
            return value;
        }, itineraryId, userId);

        itinerary.setHotels(hotelRepository.findAllHotelsByItineraryId(itineraryId));

        String flight_sql = "SELECT * FROM flights WHERE itinerary_id = ?";
        // Get flights related to this specific itinerary
        itinerary.setFlights(jdbcTemplate.query(flight_sql,
                (result, rowNum) -> {
                    Flight flight = new Flight();
                    flight.setFlightId(result.getInt("flight_id"));
                    flight.setItineraryId(result.getInt("itinerary_id"));
                    flight.setAirline(result.getString("airline"));
                    flight.setFlightNumber(result.getString("flight_number"));
                    flight.setDepartureAirport(result.getString("departure_airport"));
                    flight.setArrivalAirport(result.getString("arrival_airport"));
                    flight.setDepartureLatitude(result.getDouble("departure_latitude"));
                    flight.setDepartureLongitude(result.getDouble("departure_longitude"));
                    flight.setArrivalLatitude(result.getDouble("arrival_latitude"));
                    flight.setArrivalLongitude(result.getDouble("arrival_longitude"));
                    flight.setDepartureTime(result.getTimestamp("departure_time").toLocalDateTime());
                    flight.setArrivalTime(result.getTimestamp("arrival_time").toLocalDateTime());
                    return flight;
                }, itineraryId));


        itinerary.setAttractions(attractionRepository.findAllAttractionsByItineraryId(itineraryId));
                
        return itinerary;
    }

    /**
     * Create the new itinerary record in database
     * 
     * @param itinerary
     * @return the successfully created itinerary id
     */
    public int createItinerary(Itinerary itinerary) {
        String sql = """
                INSERT INTO itineraries (itinerary_name, start_date, end_date, user_id)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[] { "itinerary_id" });
            statement.setString(1, itinerary.getItineraryName());
            statement.setDate(2, java.sql.Date.valueOf((itinerary.getStartDate())));
            statement.setDate(3, java.sql.Date.valueOf(itinerary.getEndDate()));
            statement.setInt(4, itinerary.getUserId());
            return statement;
        }, keyHolder);

        // Return the newly created itinerary id
        return keyHolder.getKey().intValue();
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
     * Update an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param itinerary
     * @return number of rows updated in itineraries
     */
    public int updateItinerary(int itineraryId, int userId, Itinerary itinerary) {
        String sql = """
                UPDATE itineraries
                SET itinerary_name = COALESCE(?, itinerary_name),
                    start_date = COALESCE(?, start_date),
                    end_date = COALESCE(?, end_date)
                WHERE itinerary_id = ? AND user_id = ?
                """;

        return jdbcTemplate.update(
                sql,
                itinerary.getItineraryName(),
                itinerary.getStartDate(),
                itinerary.getEndDate(),
                itineraryId,
                userId
        );
    }

    /**
     * Delete an itinerary of the user
     * @param itineraryId
     * @param userId
     * @return number of rows updated in itineraries
     */
    public int deleteItinerary(int itineraryId, int userId) {
        String sql = "DELETE FROM itineraries WHERE itinerary_id = ? AND user_id = ?";
        return jdbcTemplate.update(sql, itineraryId, userId);
    }
}
