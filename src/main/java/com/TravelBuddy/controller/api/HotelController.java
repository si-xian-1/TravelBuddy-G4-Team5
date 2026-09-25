package com.TravelBuddy.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
