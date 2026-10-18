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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
     * <<<<<<< HEAD
     *
     * =======
     * Create a new hotel
     * >>>>>>> main
     *
     * @param itineraryId
     * @param currentUser
     * @param hotel
     * @return Status code whether hotel is successfully created and results or
     * unauthorized access
     */
    @PostMapping("/")
    public ResponseEntity<Hotel> createHotel(
            
    <<<<<<< HEAD
            @PathVariable int itineraryId, @RequestParam
    int userId, @RequestBody Hotel hotel
        

    ) {

        Hotel addedHotel = hotelService.createHotel(itineraryId, userId, hotel);

         == == ==
                = @PathVariable int itineraryId
        , @AuthenticationPrincipal
        User currentUser, @Valid @RequestBody Hotel hotel
            
        ) {
        
        Hotel addedHotel = hotelService.createHotel(itineraryId, currentUser.getUserId(), hotel);
             >>> >>> > main // If success, returns the newly created hotel
                    // If invalid inputs, returns the validation message
                    // If unauthorized access, return unauthorized access message
            return ResponseEntity.status(HttpStatus.CREATED).body(addedHotel);
        }

        /**
         * <<<<<<< HEAD
         *
         * =======
         * Get specified hotel details
         * >>>>>>> main
         *
         * @param itineraryId
         * @param currentUser
         * @param hotelId
         * @return Status code of user selected hotel or unauthorized access
         */
        @GetMapping("/{hotelId}")
        public ResponseEntity<Hotel> getSelectedHotel
        (
            @PathVariable
        int itineraryId, @AuthenticationPrincipal User currentUser, @PathVariable int hotelId
            
        ) {

        Hotel hotel = hotelService.getSelectedHotel(itineraryId, currentUser.getUserId(), hotelId);

            if (hotel == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel not found");
            }

            // If success, returns the selected hotel
            // If unauthorized access, return unauthorized access message
            return ResponseEntity.status(HttpStatus.OK).body(hotel);
        }

        /**
         * <<<<<<< HEAD
         *
         * =======
         * Retrieve all hotels
         * >>>>>>> main
         *
         * @param itineraryId
         * @param currentUser
         * @return List of all hotels belonging to the user
         */
        @GetMapping("/all/{userId}")
        public ResponseEntity<List<Hotel>> getAllHotel
        (
            @PathVariable
        int itineraryId, @AuthenticationPrincipal User currentUser
            
        ) {

        List<Hotel> hotels = hotelService.getAllHotel(itineraryId, currentUser.getUserId());

            // If success, returns all itinerary
            return ResponseEntity.status(HttpStatus.OK).body(hotels);
        }

        /**
         * Update a hotel belonging to the selected itinerary.
         */
        @PutMapping("/{hotelId}")
        public ResponseEntity<Hotel> updateHotel
        (
        @PathVariable
        int itineraryId,
        @PathVariable int hotelId,
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody Hotel hotel
            
        ) {

    Hotel updatedHotel = hotelService.updateHotel(
                    itineraryId, currentUser.getUserId(), hotelId, hotel);

            return ResponseEntity.ok(updatedHotel);
        }

        /**
         * Delete a hotel belonging to the selected itinerary.
         */
        @DeleteMapping("/{hotelId}")
        public ResponseEntity<Void> deleteHotel
        (
        @PathVariable
        int itineraryId,
        @PathVariable int hotelId,
        @AuthenticationPrincipal User currentUser
            
        ) {

    hotelService.deleteHotel(
                    itineraryId, currentUser.getUserId(), hotelId);

            return ResponseEntity.noContent().build();
        }

    }
