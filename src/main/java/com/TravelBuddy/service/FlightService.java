package com.TravelBuddy.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        if (flight == null) {
            throw new InvalidInputsException("Flight details cannot be empty.");
        }
        if (flight.getDepartureAirport() == null || flight.getDepartureAirport().isBlank()) {
            throw new InvalidInputsException("Departure airport is required.");
        }
        if (flight.getArrivalAirport() == null || flight.getArrivalAirport().isBlank()) {
            throw new InvalidInputsException("Arrival airport is required.");
        }
        if (flight.getDepartureTime() == null || flight.getArrivalTime() == null) {
            throw new InvalidInputsException("Departure and arrival times are required.");
        }
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

        // Disaster monitoring
        flight.setDisasterWarning(gdacsService.evaluateProximityAndDateFromDisasters(
                flight.getArrivalLatitude(), flight.getArrivalLongitude(),
                flight.getArrivalTime(), flight.getArrivalTime()));

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
        return flightRepository.findAllFlightsByItineraryId(itineraryId);
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
        return flightRepository.findSelectedFlightByItineraryId(flightId, itineraryId);
    }

    /**
     * Update an existing flight after validating the user, flight details and
     * location. Recalculate its disaster warning afterwards.
     *
     * @param itineraryId itinerary containing the flight
     * @param userId user requesting the update
     * @param flightId flight to update
     * @param flight updated flight details
     * @return updated flight including its disaster warning
     */
    public Flight updateFlight(int itineraryId, int userId, int flightId, Flight flight) {
        // Verify that the user owns the itinerary.
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);
        
        // Validate the request body.
        if (flight == null) {
            throw new InvalidInputsException("Flight details cannot be empty.");
        }
        if (flight.getDepartureAirport() == null || flight.getDepartureAirport().isBlank()) {
            throw new InvalidInputsException("Departure airport is required.");
        }
        if (flight.getArrivalAirport() == null || flight.getArrivalAirport().isBlank()) {
            throw new InvalidInputsException("Arrival airport is required.");
        }
        if (flight.getDepartureTime() == null || flight.getArrivalTime() == null) {
            throw new InvalidInputsException("Departure and arrival times are required.");
        }
        if (!flight.getArrivalTime().isAfter(flight.getDepartureTime())) {
            throw new InvalidInputsException("Arrival time must be after departure time.");
        }

        // Verify that this flight belongs to the selected itinerary.
        Flight existingFlight
                = flightRepository.findSelectedFlightByItineraryId(
                        flightId, itineraryId
                );

        if (existingFlight == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Flight not found in this itinerary."
            );
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

        // Persist the update.
        int rowsUpdated = flightRepository.updateFlight(itineraryId, flight);
        if (rowsUpdated == 0) {
            // A zero count can also mean the values were unchanged.
            // Recheck existence before treating it as a missing hotel.
            Flight currentFlight = flightRepository.findSelectedFlightByItineraryId(flightId, itineraryId);

            if (currentFlight == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Flight not found in this itinerary."
                );
            }
        }

        // Retrieve the persisted data.
        Flight updatedFlight = flightRepository.findSelectedFlightByItineraryId(flightId, itineraryId);
        if (updatedFlight == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight was not found after updating.");
        }

        // Refresh the disaster warning using the updated location and dates.
        flight.setDisasterWarning(gdacsService.evaluateProximityAndDateFromDisasters(
                flight.getArrivalLatitude(),
                flight.getArrivalLongitude(),
                flight.getDepartureTime(),
                flight.getArrivalTime()));

        return updatedFlight;
    }

    /**
     * Delete a flight only if the user owns its itinerary.
     *
     * @param itineraryId itinerary containing the flight
     * @param userId user requesting deletion
     * @param flightId flight to delete
     */
    public void deleteFlight(int itineraryId, int userId, int flightId) {
        // Verify itinerary ownership first.
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        // Verify that the flight belongs to this itinerary.
        Flight existingFlight = flightRepository.findSelectedFlightByItineraryId(flightId, itineraryId);
        if (existingFlight == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found in this itinerary.");
        }

        // Delete the selected hotel.
        int rowsDeleted = flightRepository.deleteFlight(itineraryId, flightId);

        if (rowsDeleted == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Flight could not be deleted because it no longer exists.");
        }
    }
}
