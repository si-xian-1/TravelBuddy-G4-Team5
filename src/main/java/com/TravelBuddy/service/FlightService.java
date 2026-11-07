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

        // Ensure inputted coordinate values are valid
        validateCoordinates(flight.getDepartureLatitude(), flight.getDepartureLongitude(), "departure");
        validateCoordinates(flight.getArrivalLatitude(), flight.getArrivalLongitude(), "arrival");

        flight.setItineraryId(itineraryId);

        // Execute sql to add new flight
        return flightRepository.createFlight(itineraryId, flight);
    }

    private void validateCoordinates(double latitude, double longitude, String airport) {
        if (latitude == 0.0 && longitude == 0.0) {
            throw new InvalidInputsException("Valid coordinates must be provided for the " + airport + " airport.");
        }

        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new InvalidInputsException("Invalid " + airport
                    + " airport coordinates: latitude must be between -90 and 90, and longitude between -180 and 180.");
        }
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
            try {
                flight.setDisasterWarning(gdacsService.evaluateProximityAndDateFromDisasters(
                        flight.getArrivalLatitude(), flight.getArrivalLongitude(),
                        flight.getDepartureTime(), flight.getArrivalTime()));
            } catch (IllegalStateException exception) {
                flight.setDisasterWarning("Disaster monitoring is currently unavailable. We are doing our best to resolve this.");
            }
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

        // Ensure inputted coordinate is valid
        validateCoordinates(flight.getDepartureLatitude(), flight.getDepartureLongitude(), "departure");
        validateCoordinates(flight.getArrivalLatitude(), flight.getArrivalLongitude(), "arrival");

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