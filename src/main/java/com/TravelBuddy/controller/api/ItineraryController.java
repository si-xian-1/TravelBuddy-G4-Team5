package com.TravelBuddy.controller.api;

import java.util.List;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.TravelBuddy.model.Itinerary;
import com.TravelBuddy.model.User;
import com.TravelBuddy.service.ItineraryService;

import jakarta.validation.Valid;

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
     * @param currentUser
     * @return Status code of user selected itinerary or unauthorized access
     */
    @GetMapping("/{itineraryId}")
    public ResponseEntity<Itinerary> getSelectedItinerary(
            @PathVariable int itineraryId, @AuthenticationPrincipal User currentUser) {

        Itinerary itinerary = itineraryService.getSelectedItinerary(itineraryId, currentUser.getUserId());

        // If success, returns the selected itinerary
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.OK).body(itinerary);
    }

    /**
     * 
     * @param currentUser
     * @return List of all itineraries belonging to the user
     */
    @GetMapping("/all")
    public ResponseEntity<List<Itinerary>> getAllItinerary(
            @AuthenticationPrincipal User currentUser) {

        List<Itinerary> itineraries = itineraryService.getAllItinerary(currentUser.getUserId());

        // If success, returns all itinerary
        return ResponseEntity.status(HttpStatus.OK).body(itineraries);
    }

    /**
     * 
     * @param currentUser
     * @param itinerary
     * @return Status code whether itinerary is successfully created and id or unauthorized access
     */
    @PostMapping("/")
    public ResponseEntity<String> createItinerary(
            @AuthenticationPrincipal User currentUser, @Valid @RequestBody Itinerary itinerary) {
        
        int addedItinerary = itineraryService.createItinerary(itinerary, currentUser.getUserId());
        
        // If success, returns the newly created itinerary
        // If invalid inputs, returns the validation message
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully created itinerary id:" + addedItinerary);
    }

    /**
     * Update an existing itinerary of the user
     * @param itineraryId
     * @param currentUser
     * @param itinerary
     * @return status of the update
     */
    @PutMapping("/{itineraryId}")
    public ResponseEntity<String> updateItinerary(
            @PathVariable int itineraryId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody Itinerary itinerary
    ) {
        // Check if any update was made
        int rowsUpdated = itineraryService.updateItinerary(itineraryId, currentUser.getUserId(), itinerary);
        
        if (rowsUpdated > 0) {
            return ResponseEntity.status(HttpStatus.OK).body("Successfully updated itinerary id: " + itineraryId);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Itinerary not found or could not be updated.");
        }
    }

    /**
     * 
     * @param itineraryId
     * @param currentUser
     * @return success message for deletion of itinerary or error message
     */
    @DeleteMapping("/{itineraryId}")
    public ResponseEntity<String> deleteItinerary(@PathVariable int itineraryId, @AuthenticationPrincipal User currentUser) {
        if (itineraryService.deleteItinerary(itineraryId, currentUser.getUserId()) == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Itinerary not found or could not be updated.");
        }

        return ResponseEntity.status(HttpStatus.OK).body("Successfully deleted itinerary");
    }
}