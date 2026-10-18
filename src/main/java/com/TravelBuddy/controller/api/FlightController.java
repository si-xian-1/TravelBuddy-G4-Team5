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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.TravelBuddy.model.Flight;
import com.TravelBuddy.model.User;
import com.TravelBuddy.service.FlightService;

import jakarta.validation.Valid;

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
            @PathVariable int itineraryId, @AuthenticationPrincipal User currentUser, @Valid @RequestBody Flight flight) {

        Flight addedFlight = flightService.createFlight(itineraryId, currentUser.getUserId(), flight);

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
            @PathVariable int itineraryId, @AuthenticationPrincipal User currentUser, @PathVariable int flightId) {

        Flight flight = flightService.getSelectedFlight(itineraryId, currentUser.getUserId(), flightId);

        if (flight == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found");
        }

        // If success, returns the selected flight
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.OK).body(flight);
    }

    /**
     * Get all flights belonging to the user.
     * 
     * @param itineraryId
     * @param userId
     * @return List of all flights belonging to the user
     */
    @GetMapping("/all")
    public ResponseEntity<List<Flight>> getAllFlights(
            @PathVariable int itineraryId, @AuthenticationPrincipal User currentUser) {

        List<Flight> flights = flightService.getAllFlights(itineraryId, currentUser.getUserId());

        // If success, returns all flights
        return ResponseEntity.status(HttpStatus.OK).body(flights);
    }

    /**
     * Update a flight belonging to the specified itinerary.
     */
    @PutMapping("/{flightId}")
    public String updateFlight(
            @PathVariable int itineraryId, @PathVariable int flightId, @AuthenticationPrincipal User currentUser, @Valid @RequestBody Flight flight) {

        if (flightService.updateFlight(itineraryId, currentUser.getUserId(), flightId, flight) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Flight not found"
            );
        }

        return "Flight details updated successfully";
    }

    /**
     * Delete a flight belonging to the specified itinerary.
     */
    @DeleteMapping("/{flightId}")
    public ResponseEntity<String> deleteFlight(
            @PathVariable int itineraryId, @PathVariable int flightId, @AuthenticationPrincipal User currentUser) {

        if (flightService.deleteFlight(itineraryId, currentUser.getUserId(), flightId) == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Flight not found or could not be deleted.");
        }
        
        return ResponseEntity.status(HttpStatus.OK).body("Successfully deleted flight");
    }
}
