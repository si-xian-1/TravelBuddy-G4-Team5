package com.TravelBuddy.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.Attraction;
import com.TravelBuddy.service.AttractionService;

@RestController
@RequestMapping("/api/itinerary/{itineraryId}/attractions")
public class AttractionController {
    private final AttractionService attractionService;

    public AttractionController(AttractionService attractionService) {
        this.attractionService = attractionService;
    }

    /**
     * 
     * @param itineraryId
     * @param userId
     * @param attraction
     * @return Status code whether attraction is successfully created and results or unauthorized access
     */
    @PostMapping("/")
    public ResponseEntity<Attraction> createAttraction(
            @PathVariable int itineraryId, @RequestParam int userId, @RequestBody Attraction attraction) {        

        //after user chooses recommendation then html sets display name of itinerary and calls this function
        Attraction addedAttraction = attractionService.createAttraction(itineraryId, userId, attraction);

        // If success, returns the newly created attraction
        // If invalid inputs, returns the validation message
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.CREATED).body(addedAttraction);
    }

    /**
     * 
     * @param itineraryId
     * @param userId
     * @param attractionId
     * @return Status code of user selected attraction or unauthorized access
     */
    @GetMapping("/{attractionId}")
    public ResponseEntity<Attraction> getSelectedAttraction(
            @PathVariable int itineraryId, @RequestParam int userId, @PathVariable int attractionId) {

        Attraction attraction = attractionService.getSelectedAttraction(itineraryId, userId, attractionId);

        if (attraction == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attraction not found");
        }

        // If success, returns the selected itinerary
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.OK).body(attraction);
    }

    /**
     * 
     * @param itineraryId
     * @param userId
     * @return List of all attractions belonging to the user
     */
    @GetMapping("/all/{userId}")
    public ResponseEntity<List<Attraction>> getAllAttraction(
            @PathVariable int itineraryId, @PathVariable int userId) {

        List<Attraction> attractions = attractionService.getAllAttraction(itineraryId, userId);

        // If success, returns all itinerary
        return ResponseEntity.status(HttpStatus.OK).body(attractions);
    }

    /**
     * 
     * @param itineraryId
     * @param attractionId
     * @param attraction
     * @param userId
     * @return Success message for successful update of attraction or error message
     */
    @PutMapping("/{attractionId}")
    public String updateAttraction(
            @PathVariable int itineraryId,
            @PathVariable int attractionId, 
            @RequestBody Attraction attraction,
            @RequestParam int userId
    ) {
        if (attractionService.updateAttraction(itineraryId, userId, attractionId, attraction) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Attraction not found"
            );
        }

        return "Attraction updated successfully";
    }

    /**
     * 
     * @param itineraryId
     * @param attractionId
     * @param userId
     * @return success message for deletion of attraction or error message
     */
    @DeleteMapping("/{attractionId}")
    public String deleteAttraction(@PathVariable int itineraryId, @PathVariable int attractionId, @RequestParam int userId) {
        if (attractionService.deleteAttraction(itineraryId, userId, attractionId) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Attraction not found"
            );
        }

        return "Attraction deleted";
    }
}