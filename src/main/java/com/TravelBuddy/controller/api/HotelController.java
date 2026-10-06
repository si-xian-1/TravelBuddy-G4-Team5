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
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.Hotel;
import com.TravelBuddy.service.HotelService;

@RestController
@RequestMapping("/api/itinerary/{itineraryId}/hotels")
public class HotelController {
    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    /**
     * 
     * @param itineraryId
     * @param userId
     * @param hotel
     * @return Status code whether hotel is successfully created and results or unauthorized access
     */
    @PostMapping("/")
    public ResponseEntity<Hotel> createHotel(
            @PathVariable int itineraryId, @RequestParam int userId, @RequestBody Hotel hotel) {
        
        Hotel addedHotel = hotelService.createHotel(itineraryId, userId, hotel);
        
        // If success, returns the newly created hotel
        // If invalid inputs, returns the validation message
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.CREATED).body(addedHotel);
    }

    /**
     * 
     * @param itineraryId
     * @param userId
     * @param hotelId
     * @return Status code of user selected hotel or unauthorized access
     */
    @GetMapping("/{hotelId}")
    public ResponseEntity<Hotel> getSelectedHotel(
            @PathVariable int itineraryId, @RequestParam int userId, @PathVariable int hotelId) {

        Hotel hotel = hotelService.getSelectedHotel(itineraryId, userId, hotelId);

        if (hotel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel not found");
        }

        // If success, returns the selected hotel
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.OK).body(hotel);
    }

    /**
     * 
     * @param itineraryId
     * @param userId
     * @return List of all hotels belonging to the user
     */
    @GetMapping("/all/{userId}")
    public ResponseEntity<List<Hotel>> getAllHotel(
            @PathVariable int itineraryId, @PathVariable int userId) {

        List<Hotel> hotels = hotelService.getAllHotel(itineraryId, userId);

        // If success, returns all itinerary
        return ResponseEntity.status(HttpStatus.OK).body(hotels);
    }
}
