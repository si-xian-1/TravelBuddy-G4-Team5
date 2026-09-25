package com.TravelBuddy.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.TravelBuddy.model.Hotel;

@Repository 
public class HotelRepository {
    private final JdbcTemplate jdbcTemplate;

    public HotelRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get all hotel in an itinerary
     * @param itineraryId
     * @return all hotel
     */
    public List<Hotel> findAllHotelsByItineraryId(int itineraryId) {
        String sql = """
                SELECT hotel_id, itinerary_id, hotel_name, latitude, longitude, check_in, check_out
                FROM hotels WHERE itinerary_id = ?
                """;
        
        return jdbcTemplate.query(
                sql,
                (result, rowNum) -> {
            Hotel hotel = new Hotel();

            hotel.setHotelId(result.getInt("hotel_id"));
            hotel.setItineraryId(result.getInt("itinerary_id"));
            hotel.setHotelName(result.getString("hotel_name"));
            hotel.setLatitude(result.getDouble("latitude"));
            hotel.setLongitude(result.getDouble("longitude"));
            hotel.setCheckIn(result.getTimestamp("check_in").toLocalDateTime());
            hotel.setCheckOut(result.getTimestamp("check_out").toLocalDateTime());

            return hotel;
        }, itineraryId);
    }

    /**
     * Get a detailed specific hotel based on itinerary id and selected hotel to view
     */
    public Hotel findSelectedHotelByItineraryId(int hotelId, int itineraryId) {
        String sql = """
                SELECT hotel_id, itinerary_id, hotel_name, latitude, longitude, check_in, check_out
                FROM hotels WHERE hotel_id = ? AND itinerary_id = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (result, rowNum) -> {
            Hotel hotel = new Hotel();
            hotel.setHotelId(result.getInt("hotel_id"));
            hotel.setItineraryId(result.getInt("itinerary_id"));
            hotel.setHotelName(result.getString("hotel_name"));
            hotel.setLatitude(result.getDouble("latitude"));
            hotel.setLongitude(result.getDouble("longitude"));
            hotel.setCheckIn(result.getTimestamp("check_in").toLocalDateTime());
            hotel.setCheckOut(result.getTimestamp("check_out").toLocalDateTime());

            return hotel;
        }, itineraryId);
    }
    
    /**
     * Add hotel stay location into the itinerary
     * 
     * @param itineraryId
     * @param hotel
     * @return newly created hotel stay details
     */
    public Hotel createHotel(int itineraryId, Hotel hotel) {
        String sql = """
                INSERT INTO hotels (itinerary_id, hotel_name, latitude, longitude, check_in, check_out)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[] { "hotel_id" });
            statement.setInt(1, itineraryId);
            statement.setString(2, hotel.getHotelName());
            statement.setDouble(3, hotel.getLatitude());
            statement.setDouble(4, hotel.getLongitude());
            statement.setTimestamp(5, Timestamp.valueOf(hotel.getCheckIn()));
            statement.setTimestamp(6, Timestamp.valueOf(hotel.getCheckOut()));
            return statement;
        }, keyHolder);

        // Retrieve hotel id of the newly created hotel from database
        hotel.setHotelId(keyHolder.getKey().intValue());
        hotel.setItineraryId(itineraryId);
        
        return hotel;

    }
    
    /**
     * verifyHotelAndUser?
     */
}
