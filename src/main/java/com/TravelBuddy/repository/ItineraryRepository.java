package com.TravelBuddy.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.TravelBuddy.model.Attraction;
import com.TravelBuddy.model.Flight;
import com.TravelBuddy.model.Hotel;
import com.TravelBuddy.model.Itinerary;

@Repository
public class ItineraryRepository {
    private final JdbcTemplate jdbcTemplate;

    public ItineraryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

        String hotel_sql = "SELECT * FROM hotels WHERE itinerary_id = ?";
        // Get hotels related to this specific itinerary
        itinerary.setHotels(jdbcTemplate.query(hotel_sql,
                (result, rowNum) -> {
                    Hotel hotel = new Hotel();
                    hotel.setHotelId(result.getInt("hotel_id"));
                    hotel.setHotelName(result.getString("hotel_name"));
                    hotel.setLatitude(result.getDouble("latitude"));
                    hotel.setLongitude(result.getDouble("longitude"));
                    hotel.setCheckIn(result.getTimestamp("check_in").toLocalDateTime());
                    hotel.setCheckOut(result.getTimestamp("check_out").toLocalDateTime());
                    return hotel;
                }, itineraryId));

        String flight_sql = "SELECT * FROM flights WHERE itinerary_id = ?";
        // Get flights related to this specific itinerary
        itinerary.setFlights(jdbcTemplate.query(flight_sql,
                (result, rowNum) -> {
                    Flight flight = new Flight();
                    flight.setFlightId(result.getInt("flight_id"));
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

        String attractions_sql = "SELECT * FROM attractions WHERE itinerary_id = ?";
        // Get attractions related to this specific itinerary
        itinerary.setAttractions(jdbcTemplate.query(attractions_sql,
                (result, rowNum) -> {
                    Attraction attraction = new Attraction();
                    attraction.setAttractionId(result.getInt("attraction_id"));
                    attraction.setAttractionName(result.getString("attraction_name"));
                    attraction.setLatitude(result.getDouble("latitude"));
                    attraction.setLongitude(result.getDouble("longitude"));
                    attraction.setVisitDate(result.getTimestamp("visit_date").toLocalDateTime());
                    return attraction;
                }, itineraryId));
                
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
}
