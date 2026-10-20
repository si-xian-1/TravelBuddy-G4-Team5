package com.TravelBuddy.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.Hotel;
import com.TravelBuddy.model.User;
import com.TravelBuddy.service.HotelService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/itinerary/{itineraryId}/hotels")
public class HotelController {
    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    /**
     * Create a new hotel
     * @param itineraryId
     * @param currentUser
     * @param hotel
     * @return Status code whether hotel is successfully created and results or unauthorized access
     */
    @PostMapping("/")
    public ResponseEntity<Hotel> createHotel(
            @PathVariable int itineraryId, @AuthenticationPrincipal User currentUser, @Valid @RequestBody Hotel hotel) {
        
        Hotel addedHotel = hotelService.createHotel(itineraryId, currentUser.getUserId(), hotel);
        
        // If success, returns the newly created hotel
        // If invalid inputs, returns the validation message
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.CREATED).body(addedHotel);
    }

    /**
     * Get specified hotel details
     * @param itineraryId
     * @param currentUser
     * @param hotelId
     * @return Status code of user selected hotel or unauthorized access
     */
    @GetMapping("/{hotelId}")
    public ResponseEntity<Hotel> getSelectedHotel(
            @PathVariable int itineraryId, @AuthenticationPrincipal User currentUser, @PathVariable int hotelId) {

        Hotel hotel = hotelService.getSelectedHotel(itineraryId, currentUser.getUserId(), hotelId);

        if (hotel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel not found");
        }

        // If success, returns the selected hotel
        // If unauthorized access, return unauthorized access message
        return ResponseEntity.status(HttpStatus.OK).body(hotel);
    }

    /**
     * Retrieve all hotels
     * @param itineraryId
     * @param currentUser
     * @return List of all hotels belonging to the user
     */
    @GetMapping("/all")
    public ResponseEntity<List<Hotel>> getAllHotel(
            @PathVariable int itineraryId, @AuthenticationPrincipal User currentUser) {

        List<Hotel> hotels = hotelService.getAllHotel(itineraryId, currentUser.getUserId());

        // If success, returns all itinerary
        return ResponseEntity.status(HttpStatus.OK).body(hotels);
    }

     /**
     * Update hotel details
     * @param itineraryId
     * @param hotelId
     * @param hotel
     * @param currentUser
     * @return Success message for successful update of hotel or error message
     */
    @PutMapping("/{hotelId}")
    public String updateHotel(
            @PathVariable int itineraryId,
            @PathVariable int hotelId, 
            @Valid @RequestBody Hotel hotel,
            @AuthenticationPrincipal User currentUser
    ) {
        if (hotelService.updateHotel(itineraryId, currentUser.getUserId(), hotelId, hotel) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hotel not found"
            );
        }

        return "Hotel updated successfully";
    }

    /**
     * Delete a particular hotel
     * @param itineraryId
     * @param hotelId
     * @param currentUser
     * @return success message for deletion of hotel or error message
     */
    @DeleteMapping("/{hotelId}")
    public String deleteHotel(@PathVariable int itineraryId, @PathVariable int hotelId, @AuthenticationPrincipal User currentUser) {
        if (hotelService.deleteHotel(itineraryId, currentUser.getUserId(), hotelId) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hotel not found"
            );
        }

        return "Hotel deleted successfully";
    }
}
