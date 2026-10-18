package com.TravelBuddy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.model.Flight;
import com.TravelBuddy.model.LocationResponse;
import com.TravelBuddy.repository.FlightRepository;

@Service
public class FlightService {
    private final FlightRepository flightRepository;
    private final ItineraryService itineraryService;
    private final LocationService locationService;
    private final GdacsService gdacsService;

    public FlightService(FlightRepository flightRepository, ItineraryService itineraryService, LocationService locationService, GdacsService gdacsService) {
        this.flightRepository = flightRepository;
        this.itineraryService = itineraryService;
        this.locationService = locationService;
        this.gdacsService = gdacsService;
    }

    /**
     * Validate flight inputs before adding into database
     *
     * @param itineraryId
     * @param userId
     * @param flight
     * @return Validation messages or the newly created flight if successful
     */
    public Flight createFlight(int itineraryId, int userId, Flight flight) {
        // Check that user has the permission to add hotel for this selected itinerary
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);
        
        // Validation checks and return meaningful response to user
        if (!flight.getArrivalTime().isAfter(flight.getDepartureTime())) {
            throw new InvalidInputsException("Arrival time must be after departure time.");
        }

        // Get the coordinates of the airports
        LocationResponse departureLocation = locationService.getCoordinates(flight.getDepartureAirport().trim());
        LocationResponse arrivalLocation = locationService.getCoordinates(flight.getArrivalAirport().trim());

        // When API is unable to find coordinates of the user selected location
        if (departureLocation == null || departureLocation.getLat() == null || departureLocation.getLon() == null) {
            throw new InvalidInputsException(
                    "Could not find coordinates for departure airport: " + flight.getDepartureAirport());
        }
        if (arrivalLocation == null || arrivalLocation.getLat() == null || arrivalLocation.getLon() == null) {
            throw new InvalidInputsException(
                    "Could not find coordinates for arrival airport: " + flight.getArrivalAirport());
        }

        flight.setItineraryId(itineraryId);
        flight.setDepartureLatitude(Double.parseDouble(departureLocation.getLat()));
        flight.setDepartureLongitude(Double.parseDouble(departureLocation.getLon()));
        flight.setArrivalLatitude(Double.parseDouble(arrivalLocation.getLat()));
        flight.setArrivalLongitude(Double.parseDouble(arrivalLocation.getLon()));

        // Execute sql to add new flight
        Flight savedFlight = flightRepository.createFlight(itineraryId, flight);

        // If flight creation is successful
        return savedFlight;
    }

    /**
     * Get all flights belonging to an itinerary of the user
     *
     * @param itineraryId
     * @param userId
     * @return all flights in the itinerary
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
     *
     * @param itineraryId
     * @param userId
     * @param flightId
     * @return elected
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
     * Update an existing flight after validating the user, flight details and
     * location.
     *
     * @param itineraryId itinerary containing the flight
     * @param userId user requesting the update
     * @param flightId flight to update
     * @param flight updated flight details
     * @return rows updated
     */
    public int updateFlight(int itineraryId, int userId, int flightId, Flight flight) {
        // Verify that the user owns the itinerary.
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        if (!flight.getArrivalTime().isAfter(flight.getDepartureTime())) {
            throw new InvalidInputsException("Arrival time must be after departure time.");
        }

        // Resolve the location again if its airports have changed.
        LocationResponse departureLocation = locationService.getCoordinates(flight.getDepartureAirport().trim());
        LocationResponse arrivalLocation = locationService.getCoordinates(flight.getArrivalAirport().trim());

        // When API is unable to find coordinates of the user selected location
        if (departureLocation == null || departureLocation.getLat() == null || departureLocation.getLon() == null) {
            throw new InvalidInputsException(
                    "Could not find coordinates for departure airport: " + flight.getDepartureAirport());
        }
        if (arrivalLocation == null || arrivalLocation.getLat() == null || arrivalLocation.getLon() == null) {
            throw new InvalidInputsException(
                    "Could not find coordinates for arrival airport: " + flight.getArrivalAirport());
        }

        flight.setItineraryId(itineraryId);
        flight.setDepartureLatitude(Double.parseDouble(departureLocation.getLat()));
        flight.setDepartureLongitude(Double.parseDouble(departureLocation.getLon()));
        flight.setArrivalLatitude(Double.parseDouble(arrivalLocation.getLat()));
        flight.setArrivalLongitude(Double.parseDouble(arrivalLocation.getLon()));

        return flightRepository.updateFlight(itineraryId, flightId, flight);
    }

    /**
     * Delete a flight only if the user owns its itinerary.
     *
     * @param itineraryId itinerary containing the flight
     * @param userId user requesting deletion
     * @param flightId flight to delete
     */
    public int deleteFlight(int itineraryId, int userId, int flightId) {
        // Verify itinerary ownership first.
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        return flightRepository.deleteFlight(itineraryId, flightId);
    }
}
