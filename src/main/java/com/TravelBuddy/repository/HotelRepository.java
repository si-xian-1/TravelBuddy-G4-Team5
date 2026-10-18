package com.TravelBuddy.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
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
     *
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
     * Get a detailed specific hotel based on itinerary id and selected hotel to
     * view
     */
    public Hotel findSelectedHotelByItineraryId(int hotelId, int itineraryId) {
        String sql = """
                SELECT hotel_id, itinerary_id, hotel_name, latitude, longitude, check_in, check_out
                FROM hotels WHERE hotel_id = ? AND itinerary_id = ?
                """;

        try {
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
                    }, hotelId, itineraryId);
        } catch (EmptyResultDataAccessException e) {
            // When no results found for hotel
            return null;
        }
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
            var statement = connection.prepareStatement(sql, new String[]{"hotel_id"});
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
<<<<<<< HEAD
     * Update a hotel belonging to a specific itinerary.
     *
     * @param itineraryId the itinerary containing the hotel
     * @param hotel the updated hotel details
     * @return number of rows affected
     */
    public int updateHotel(int itineraryId, Hotel hotel) {
        String sql = """
            UPDATE hotels
            SET hotel_name = ?,
                latitude = ?,
                longitude = ?,
                check_in = ?,
                check_out = ?
            WHERE hotel_id = ? AND itinerary_id = ?
            """;
=======
     * Update hotel in an itinerary of the user
     * @param itineraryId
     * @param hotelId
     * @param hotel
     * @return number of updated rows in hotels
     */
    public int updateHotel(int itineraryId, int hotelId, Hotel hotel) {
        String sql = """
                UPDATE hotels
                SET hotel_name = COALESCE(?, hotel_name),
                    latitude = COALESCE(?, latitude),
                    longitude = COALESCE(?, longitude),
                    check_in = COALESCE(?, check_in),
                    check_out = COALESCE(?, check_out)
                WHERE hotel_id = ? AND itinerary_id = ?
                """;
>>>>>>> main

        return jdbcTemplate.update(
                sql,
                hotel.getHotelName(),
                hotel.getLatitude(),
                hotel.getLongitude(),
<<<<<<< HEAD
                Timestamp.valueOf(hotel.getCheckIn()),
                Timestamp.valueOf(hotel.getCheckOut()),
                hotel.getHotelId(),
=======
                hotel.getCheckIn(),
                hotel.getCheckOut(),
                hotelId,
>>>>>>> main
                itineraryId
        );
    }

    /**
<<<<<<< HEAD
     * Delete a hotel belonging to a specific itinerary.
     *
     * @param hotelId the hotel to delete
     * @param itineraryId the itinerary containing the hotel
     * @return number of rows deleted
     */
    public int deleteHotel(int hotelId, int itineraryId) {
        String sql = """
            DELETE FROM hotels
            WHERE hotel_id = ? AND itinerary_id = ?
            """;

=======
     * Delete hotel in an itinerary of the user
     * @param itineraryId
     * @param hotelId
     * @return number of updated rows in hotels
     */
    public int deleteHotel(int itineraryId, int hotelId) {
        String sql = "DELETE FROM hotels WHERE hotel_id = ? AND itinerary_id = ?";
>>>>>>> main
        return jdbcTemplate.update(sql, hotelId, itineraryId);
    }
}
