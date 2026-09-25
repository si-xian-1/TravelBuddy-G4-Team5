package com.TravelBuddy.service;

import org.springframework.stereotype.Service;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.model.Hotel;
import com.TravelBuddy.repository.HotelRepository;
import com.TravelBuddy.repository.TravelerRepository;

@Service 
public class HotelService {
    private final HotelRepository hotelRepository;
    private final ItineraryService itineraryService;
    private final LocationService locationService;
    private final GdacsService gdacsService;

    public HotelService(HotelRepository hotelRepository, ItineraryService itineraryService, LocationService locationService, GdacsService gdacsService) {
        this.hotelRepository = hotelRepository;
        this.itineraryService = itineraryService;
        this.locationService = locationService;
        this.gdacsService = gdacsService;
    }
    
    /**
     * Validate hotel inputs before adding into database
     * @param itineraryId
     * @param userId
     * @param hotel
     * @return Validation messages or the newly created hotel if successful
     */
    public Hotel createHotel(int itineraryId, int userId, Hotel hotel) {
        // Check that user has the permission to add hotel for this selected itinerary
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        // Validation checks and return meaningful response to user
        if (hotel == null) {
            throw new InvalidInputsException("Hotel cannot be empty.");
        }
        if (hotel.getHotelName() == null || hotel.getHotelName().trim().isEmpty()) {
            throw new InvalidInputsException("Hotel name is required.");
        }
        if (hotel.getCheckIn() == null || hotel.getCheckOut() == null) {
            throw new InvalidInputsException("Check-in and check-out dates are required.");
        }
        if (hotel.getCheckIn().isAfter(hotel.getCheckOut())) {
            throw new InvalidInputsException("Check-in time cannot be after check-out time.");
        }

        // Execute sql to add new hotel
        Hotel savedHotel = hotelRepository.createHotel(itineraryId, hotel);

        // Disaster monitoring
        savedHotel.setDisasterWarning(gdacsService.evaluateProximityAndDateFromDisasters(
                savedHotel.getLatitude(), savedHotel.getLongitude(), 
                savedHotel.getCheckIn(), savedHotel.getCheckOut()));

        // If hotel creation is successful
        return savedHotel;
    }

}
