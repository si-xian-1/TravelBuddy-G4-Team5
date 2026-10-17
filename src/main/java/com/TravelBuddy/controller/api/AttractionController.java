package com.TravelBuddy.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.Attraction;
import com.TravelBuddy.model.User;
import com.TravelBuddy.service.AttractionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/itinerary/{itineraryId}/attractions")
public class AttractionController {
    private final AttractionService attractionService;

    public AttractionController(AttractionService attractionService) {
        this.attractionService = attractionService;
    }

    /**
     * Create a new attraction
     * @param itineraryId
     * @param currentUser
     * @param attraction
     * @return Status code whether attraction is successfully created and results or unauthorized access
     */
    @PostMapping("/")
    public ResponseEntity<Attraction> createAttraction(
            @PathVariable int itineraryId, @AuthenticationPrincipal User currentUser, @Valid @RequestBody Attraction attraction) {        

        //after user chooses recommendation then html sets display name of itinerary and calls this function
        Attraction addedAttraction = attractionService.createAttraction(itineraryId, currentUser.getUserId(), attraction);

        // If success, returns the newly created attraction
        // If invalid inputs, returns the validation message
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.CREATED).body(addedAttraction);
    }

    /**
     * Get specified attraction to view
     * @param itineraryId
     * @param currentUser
     * @param attractionId
     * @return Status code of user selected attraction or unauthorized access
     */
    @GetMapping("/{attractionId}")
    public ResponseEntity<Attraction> getSelectedAttraction(
            @PathVariable int itineraryId, @AuthenticationPrincipal User currentUser, @PathVariable int attractionId) {

        Attraction attraction = attractionService.getSelectedAttraction(itineraryId, currentUser.getUserId(), attractionId);

        if (attraction == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attraction not found");
        }

        // If success, returns the selected itinerary
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.OK).body(attraction);
    }

    /**
     * Retrieve all attractions
     * @param itineraryId
     * @param currentUser
     * @return List of all attractions belonging to the user
     */
    @GetMapping("/all")
    public ResponseEntity<List<Attraction>> getAllAttraction(
            @PathVariable int itineraryId, @AuthenticationPrincipal User currentUser) {

        List<Attraction> attractions = attractionService.getAllAttraction(itineraryId, currentUser.getUserId());

        // If success, returns all itinerary
        return ResponseEntity.status(HttpStatus.OK).body(attractions);
    }

    /**
     * Update attraction details
     * @param itineraryId
     * @param attractionId
     * @param attraction
     * @param currentUser
     * @return Success message for successful update of attraction or error message
     */
    @PutMapping("/{attractionId}")
    public String updateAttraction(
            @PathVariable int itineraryId,
            @PathVariable int attractionId, 
            @Valid @RequestBody Attraction attraction,
            @AuthenticationPrincipal User currentUser
    ) {
        if (attractionService.updateAttraction(itineraryId, currentUser.getUserId(), attractionId, attraction) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Attraction not found"
            );
        }

        return "Attraction updated successfully";
    }

    /**
     * Delete a particular attraction
     * @param itineraryId
     * @param attractionId
     * @param currentUser
     * @return success message for deletion of attraction or error message
     */
    @DeleteMapping("/{attractionId}")
    public String deleteAttraction(@PathVariable int itineraryId, @PathVariable int attractionId, @AuthenticationPrincipal User currentUser) {
        if (attractionService.deleteAttraction(itineraryId, currentUser.getUserId(), attractionId) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Attraction not found"
            );
        }

        return "Attraction deleted";
    }
}