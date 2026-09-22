package com.TravelBuddy.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.TravelBuddy.model.Attraction;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(itinerary);
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

    /**
     * 
     * @param itineraryId
     * @param userId
     * @param attraction
     * @return Status code whether attraction is successfully created and results or unauthorized access
     */
    @PostMapping("/{itineraryId}/attractions")
    public ResponseEntity<Attraction> createAttraction(
            @PathVariable int itineraryId, @RequestParam int userId, @RequestBody Attraction attraction) {        

        //after user chooses recommendation then html sets display name of itinerary and calls this function
        Attraction addedAttraction = itineraryService.createAttraction(itineraryId, userId, attraction);

        // If success, returns the newly created attraction
        // If invalid inputs, returns the validation message
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.CREATED).body(addedAttraction);
    }

    /**
     * 
     * @param itineraryId
     * @param userId
     * @param hotel
     * @return Status code whether hotel is successfully created and results or unauthorized access
     */
    @PostMapping("/{itineraryId}/hotels")
    public ResponseEntity<Hotel> createHotel(
            @PathVariable int itineraryId, @RequestParam int userId, @RequestBody Hotel hotel) {
        
        Hotel addedHotel = itineraryService.createHotel(itineraryId, userId, hotel);
        
        // If success, returns the newly created hotel
        // If invalid inputs, returns the validation message
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.CREATED).body(addedHotel);
    }
}