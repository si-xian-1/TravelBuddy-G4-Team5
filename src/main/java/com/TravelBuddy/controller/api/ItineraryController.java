package com.TravelBuddy.controller.api;

import java.util.List;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.Hotel;
import com.TravelBuddy.model.Itinerary;
import com.TravelBuddy.service.ItineraryService;

@RestController
@RequestMapping("/api/itinerary")
public class ItineraryController {
    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    /**
     * 
     * @param itineraryId
     * @param userId
     * @return Status code of user selected itinerary or unauthorized access
     */
    @GetMapping("/{itineraryId}")
    public ResponseEntity<Itinerary> getSelectedItinerary(
            @PathVariable int itineraryId, @RequestParam int userId) {

        Itinerary itinerary = itineraryService.getSelectedItinerary(itineraryId, userId);

        // If success, returns the selected itinerary
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.OK).body(itinerary);
    }

    /**
     * 
     * @param userId
     * @return List of all itineraries belonging to the user
     */
    @GetMapping("/all/{userId}")
    public ResponseEntity<List<Itinerary>> getAllItinerary(
            @PathVariable int userId) {

        List<Itinerary> itineraries = itineraryService.getAllItinerary(userId);

        // If success, returns all itinerary
        return ResponseEntity.status(HttpStatus.OK).body(itineraries);
    }

    /**
     * 
     * @param userId
     * @param itinerary
     * @return Status code whether itinerary is successfully created and id or unauthorized access
     */
    @PostMapping("/")
    public ResponseEntity<String> createItinerary(
            @RequestParam int userId, @RequestBody Itinerary itinerary) {
        
        int addedItinerary = itineraryService.createItinerary(itinerary, userId);
        
        // If success, returns the newly created itinerary
        // If invalid inputs, returns the validation message
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully created itinerary id:" + addedItinerary);
    }
}