package com.TravelBuddy.controller.api;

import com.TravelBuddy.model.Itinerary;
import com.TravelBuddy.service.ItineraryService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/itinerary")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @PostMapping("/attraction")
    public ResponseEntity<String> createAttraction(
            @RequestBody Itinerary itinerary, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date dateOfItinerary) {        

        int userId = 1;                         //havent thought of how to get userId of current user
        //after user chooses recommendation then html sets display name of itinerary and calls this function
        itinerary.setDate(dateOfItinerary);
        String status = itineraryService.createAttraction(itinerary, userId);
        if (status.equals("Success")) {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Attraction created successfully for location: " + itinerary.getDisplayName());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Attraction creation failed: location not found");
    }

    @PostMapping("/hotel")
    public ResponseEntity<String> createHotel(@RequestBody Itinerary itinerary) {
        itineraryService.createHotel(itinerary);
        return ResponseEntity.status(HttpStatus.CREATED).body("Hotel created successfully");
    }
}