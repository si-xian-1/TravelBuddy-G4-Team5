package com.TravelBuddy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.model.Flight;
import com.TravelBuddy.repository.FlightRepository;

@Service
public class FlightService {
    private final FlightRepository flightRepository;
    private final ItineraryService itineraryService;
    private final GdacsService gdacsService;

    public FlightService(FlightRepository flightRepository, ItineraryService itineraryService, GdacsService gdacsService) {
        this.flightRepository = flightRepository;
        this.itineraryService = itineraryService;
        this.gdacsService = gdacsService;
    }

    /**
     * Validate flight inputs before adding into database
     * @param itineraryId
     * @param userId
     * @param flight
     * @return Flight details created
     */
    public Flight createFlight(int itineraryId, int userId, Flight flight) {
        // Check that user has the permission to add flight for this selected itinerary
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);
        
        // Validation checks
        if (!flight.getArrivalTime().isAfter(flight.getDepartureTime())) {
            throw new InvalidInputsException("Arrival time must be after departure time.");
        }

        // Ensure inputted coordinate values are valid
        if (flight.getDepartureLatitude() == 0.0 && flight.getDepartureLongitude() == 0.0) {
            throw new InvalidInputsException("Valid coordinates must be provided for the departure airport.");
        }
        if (flight.getArrivalLatitude() == 0.0 && flight.getArrivalLongitude() == 0.0) {
            throw new InvalidInputsException("Valid coordinates must be provided for the arrival airport.");
        }

        flight.setItineraryId(itineraryId);

        // Execute sql to add new flight
        return flightRepository.createFlight(itineraryId, flight);
    }

    /**
     * Get all flights belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @return lists of flight
     */
    public List<Flight> getAllFlights(int itineraryId, int userId) {
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        List<Flight> flights = flightRepository.findAllFlightsByItineraryId(itineraryId);

        // Disaster warning
        for (Flight flight : flights) {
            flight.setDisasterWarning(gdacsService.evaluateProximityAndDateFromDisasters(
                    flight.getArrivalLatitude(), flight.getArrivalLongitude(),
                    flight.getDepartureTime(), flight.getArrivalTime()));
        }

        return flights;
    }

    /**
     * Get a selected flight belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param flightId
     * @return Selected flight details
     */
    public Flight getSelectedFlight(int itineraryId, int userId, int flightId) {
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        Flight flight = flightRepository.findSelectedFlightByItineraryId(flightId, itineraryId);
        
        if (flight == null) {
            throw new InvalidInputsException("Flight not found in this itinerary.");
        }

        return flight;
    }

    /**
     * Update an existing flight after validating the user, flight details and location.
     * @param itineraryId
     * @param userId
     * @param flightId
     * @param flight
     * @return rows updated
     */
    public int updateFlight(int itineraryId, int userId, int flightId, Flight flight) {
        // Verify that the user owns the itinerary.
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        if (!flight.getArrivalTime().isAfter(flight.getDepartureTime())) {
            throw new InvalidInputsException("Arrival time must be after departure time.");
        }

        // Ensure inputted coordinate is valid
        if (flight.getDepartureLatitude() == 0.0 && flight.getDepartureLongitude() == 0.0) {
            throw new InvalidInputsException("Valid coordinates must be provided for the departure airport.");
        }
        if (flight.getArrivalLatitude() == 0.0 && flight.getArrivalLongitude() == 0.0) {
            throw new InvalidInputsException("Valid coordinates must be provided for the arrival airport.");
        }

        flight.setItineraryId(itineraryId);

        return flightRepository.updateFlight(itineraryId, flightId, flight);
    }

    /**
     * Delete a flight only if the user owns its itinerary.
     * @param itineraryId
     * @param userId
     * @param flightId
     * @return 0 for unsuccessful delete, else return non-zero for successful delete
     */
    public int deleteFlight(int itineraryId, int userId, int flightId) {
        // Verify itinerary ownership first.
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        return flightRepository.deleteFlight(itineraryId, flightId);
    }
}