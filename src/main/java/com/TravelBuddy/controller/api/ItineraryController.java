package com.TravelBuddy.controller.api;

import com.TravelBuddy.model.Itinerary;
import com.TravelBuddy.service.ItineraryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/itinerary")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    // Standard POST with explicit coordinates
    @PostMapping("/attraction")
    public ResponseEntity<String> createAttraction(@RequestBody Itinerary itinerary) {
        int userId = 1;                         //havent thought of how to get userId of current user
        itineraryService.createAttraction(itinerary, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body("Attraction created successfully");
    }

    // New POST using place name parameter: POST /api/itinerary/attraction/by-place?placeName=Eiffel+Tower
    @PostMapping("/attraction/by-place")
    public ResponseEntity<String> createAttractionWithPlaceName(
            @RequestBody Itinerary itinerary,
            @RequestParam String placeName) {

        int userId = 1;                         //havent thought of how to get userId of current user
        itineraryService.createAttractionWithPlaceName(itinerary, placeName, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Attraction created successfully for location: " + placeName);
    }

    @PostMapping("/hotel")
    public ResponseEntity<String> createHotel(@RequestBody Itinerary itinerary) {
        itineraryService.createHotel(itinerary);
        return ResponseEntity.status(HttpStatus.CREATED).body("Hotel created successfully");
    }
}