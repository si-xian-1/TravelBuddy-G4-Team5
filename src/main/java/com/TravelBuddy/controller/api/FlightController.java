package com.TravelBuddy.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.Flight;
import com.TravelBuddy.service.FlightService;

@RestController
@RequestMapping("/api/itinerary/{itineraryId}/flights")
public class FlightController {
    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    /**
     * Create a flight in the specified itinerary.
     * 
     * @param itineraryId
     * @param userId
     * @param flight
     * @return Status code whether flight is successfully created and results or
     * unauthorized access
     */
    @PostMapping("/")
    public ResponseEntity<Flight> createFlight(
            @PathVariable int itineraryId, @RequestParam int userId, @RequestBody Flight flight) {

        Flight addedFlight = flightService.createFlight(itineraryId, userId, flight);

        // If success, returns the newly created flight
        // If invalid inputs, returns the validation message
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.CREATED).body(addedFlight);
    }

    /**
     * Get a selected flight from the specified itinerary.
     * 
     * @param itineraryId
     * @param userId
     * @param flightId
     * @return Status code of user selected flight or unauthorized access
     */
    @GetMapping("/{flightId}")
    public ResponseEntity<Flight> getSelectedFlight(
            @PathVariable int itineraryId, @RequestParam int userId, @PathVariable int flightId) {

        Flight flight = flightService.getSelectedFlight(itineraryId, userId, flightId);

        if (flight == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found");
        }

        // If success, returns the selected flight
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.ok(flight);
    }

    /**
     * Get all flights belonging to the user.
     * 
     * @param itineraryId
     * @param userId
     * @return List of all flights belonging to the user
     */
    @GetMapping("/all/{userId}")
    public ResponseEntity<List<Flight>> getAllFlights(
            @PathVariable int itineraryId, @PathVariable int userId) {

        List<Flight> flights = flightService.getAllFlights(itineraryId, userId);

        // If success, returns all flights
        return ResponseEntity.ok(flights);
    }

    /**
     * Update a flight belonging to the specified itinerary.
     */
    @PutMapping("/{flightId}")
    public ResponseEntity<Flight> updateFlight(
            @PathVariable int itineraryId, @PathVariable int flightId, @RequestParam int userId, @RequestBody Flight flight) {

        Flight updatedFlight = flightService.updateFlight(itineraryId, userId, flightId, flight);

        return ResponseEntity.ok(updatedFlight);
    }

    /**
     * Delete a flight belonging to the specified itinerary.
     */
    @DeleteMapping("/{flightId}")
    public ResponseEntity<Void> deleteFlight(
            @PathVariable int itineraryId, @PathVariable int flightId, @RequestParam int userId) {

        flightService.deleteFlight(itineraryId, userId, flightId);
        
        return ResponseEntity.noContent().build();
    }
}
