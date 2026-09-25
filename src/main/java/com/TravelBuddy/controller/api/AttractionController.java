package com.TravelBuddy.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
