package com.TravelBuddy.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.TravelBuddy.model.Flight;

@Repository 
public class FlightRepository {
    private final JdbcTemplate jdbcTemplate;

    public FlightRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get all flight in an itinerary
     * @param itineraryId
     * @return all flight
     */
    public List<Flight> findAllFlightsByItineraryId(int itineraryId) {
        String sql = """
                SELECT flight_id, itinerary_id, airline, flight_number, departure_airport, arrival_airport,
                departure_latitude, departure_longitude, arrival_latitude, arrival_longitude, departure_time, arrival_time
                FROM flights WHERE itinerary_id = ?
                """;

        return jdbcTemplate.query(
                sql,
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
        }, itineraryId);
    }

    /**
     * Find a specific flight belonging to an itinerary.
     *
     * @param flightId
     * @param itineraryId
     * @return the matching flight, or null if it does not exist
     */
    public Flight findSelectedFlightByItineraryId(int flightId, int itineraryId) {
        String sql = """
                SELECT flight_id, itinerary_id, airline, flight_number, departure_airport, arrival_airport,
                departure_latitude, departure_longitude, arrival_latitude, arrival_longitude, departure_time, arrival_time
                FROM flights WHERE itinerary_id = ? AND flight_id = ?
                """;

        try {
            return jdbcTemplate.queryForObject(
                    sql,
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
                    }, itineraryId, flightId);
        } catch (EmptyResultDataAccessException e) {
            // When no results found for flight
            return null;
        }
    }

    /**
     * Add flight into the itinerary
     *
     * @param itineraryId
     * @param flight
     * @return newly created flight details
     */
    public Flight createFlight(int itineraryId, Flight flight) {
        String sql = """
                INSERT INTO flights (
                    itinerary_id, airline, flight_number, departure_airport, arrival_airport,
                    departure_latitude, departure_longitude, arrival_latitude, arrival_longitude,
                    departure_time, arrival_time
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[] { "flight_id" });
            statement.setInt(1, itineraryId);
            statement.setString(2, flight.getAirline());
            statement.setString(3, flight.getFlightNumber());
            statement.setString(4, flight.getDepartureAirport());
            statement.setString(5, flight.getArrivalAirport());
            statement.setDouble(6, flight.getDepartureLatitude());
            statement.setDouble(7, flight.getDepartureLongitude());
            statement.setDouble(8, flight.getArrivalLatitude());
            statement.setDouble(9, flight.getArrivalLongitude());
            statement.setTimestamp(10, Timestamp.valueOf(flight.getDepartureTime()));
            statement.setTimestamp(11, Timestamp.valueOf(flight.getArrivalTime()));
            return statement;
        }, keyHolder);

        flight.setFlightId(keyHolder.getKey().intValue());
        flight.setItineraryId(itineraryId);

        return flight;
    }

    /**
     * Update a flight belonging to a specific itinerary.
     *
     * @param itineraryId
     * @param flightId
     * @param flight
     * @return the number of updated rows
     */
    public int updateFlight(int itineraryId, int flightId, Flight flight) {
        String sql = """
                UPDATE flights
                SET airline = COALESCE(?, airline),
                    flight_number = COALESCE(?, flight_number),
                    departure_airport = COALESCE(?, departure_airport),
                    arrival_airport = COALESCE(?, arrival_airport),
                    departure_latitude = COALESCE(?, departure_latitude),
                    departure_longitude = COALESCE(?, departure_longitude),
                    arrival_latitude = COALESCE(?, arrival_latitude),
                    arrival_longitude = COALESCE(?, arrival_longitude),
                    departure_time = COALESCE(?, departure_time),
                    arrival_time = COALESCE(?, arrival_time)
                WHERE flight_id = ? AND itinerary_id = ?
                """;

        return jdbcTemplate.update(
                sql,
                flight.getAirline(),
                flight.getFlightNumber(),
                flight.getDepartureAirport(),
                flight.getArrivalAirport(),
                flight.getDepartureLatitude(),
                flight.getDepartureLongitude(),
                flight.getArrivalLatitude(),
                flight.getArrivalLongitude(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flightId,
                itineraryId
        );
    }

    /**
     * Delete a flight belonging to a specific itinerary.
     *
     * @param itineraryId
     * @param flightId
     * @return the number of deleted rows
     */
    public int deleteFlight(int itineraryId, int flightId) {
        String sql = "DELETE FROM flights WHERE flight_id = ? AND itinerary_id = ?";
        return jdbcTemplate.update(sql, flightId, itineraryId);
    }
}
