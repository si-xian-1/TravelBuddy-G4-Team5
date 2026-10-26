package com.TravelBuddy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.model.Hotel;
import com.TravelBuddy.repository.HotelRepository;

@Service 
public class HotelService {
    private final HotelRepository hotelRepository;
    private final ItineraryService itineraryService;
    private final GdacsService gdacsService;

    public HotelService(HotelRepository hotelRepository, ItineraryService itineraryService, GdacsService gdacsService) {
        this.hotelRepository = hotelRepository;
        this.itineraryService = itineraryService;
        this.gdacsService = gdacsService;
    }
    
    /**
     * Validate hotel inputs before adding into database
     * @param itineraryId
     * @param userId
     * @param hotel
     * @return Hotel details created
     */
    public Hotel createHotel(int itineraryId, int userId, Hotel hotel) {
        // Check that user has the permission to add hotel for this selected itinerary
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        if (hotel.getCheckIn().isAfter(hotel.getCheckOut())) {
            throw new InvalidInputsException("Check-in time cannot be after check-out time.");
        }

        // Ensure inputted coordinate is valid
        if (hotel.getLatitude() == 0.0 && hotel.getLongitude() == 0.0) {
            throw new InvalidInputsException("Valid location coordinates must be provided from the search results.");
        }
        
        hotel.setItineraryId(itineraryId);

        // Execute sql to add new hotel
        return hotelRepository.createHotel(itineraryId, hotel);
    }

    /**
     * Get all hotels belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @return List of hotels
     */
    public List<Hotel> getAllHotel(int itineraryId, int userId) {
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        // Execute sql to get hotel
        List<Hotel> hotels = hotelRepository.findAllHotelsByItineraryId(itineraryId);

        // Disaster monitoring
        for (Hotel hotel : hotels) {
            String warning = gdacsService.evaluateProximityAndDateFromDisasters(
                    hotel.getLatitude(), hotel.getLongitude(), 
                    hotel.getCheckIn(), hotel.getCheckOut());
            
            hotel.setDisasterWarning(warning);
        }

        return hotels;
    }

    /**
     * Get selected hotel belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param hotelId
     * @return Selected hotel details
     */
    public Hotel getSelectedHotel(int itineraryId, int userId, int hotelId) {
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);
        return hotelRepository.findSelectedHotelByItineraryId(itineraryId, hotelId);
    }

    /**
     * Update selected hotel belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param hotelId
     * @param hotel
     * @return rows updated
     */
    public int updateHotel(int itineraryId, int userId, int hotelId, Hotel hotel) {
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        if (hotel.getCheckIn().isAfter(hotel.getCheckOut())) {
            throw new InvalidInputsException("Check-in time cannot be after check-out time.");
        }

        // Ensure inputted coordinate is valid
        if (hotel.getLatitude() == 0.0 && hotel.getLongitude() == 0.0) {
            throw new InvalidInputsException("Valid location coordinates must be provided from the search results.");
        }

        hotel.setItineraryId(itineraryId);
        
        return hotelRepository.updateHotel(itineraryId, hotelId, hotel);
    }

    /**
     * Deleted selected hotel belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param hotelId
     * @return 0 for unsuccessful delete, else return non-zero for successful delete
     */
    public int deleteHotel(int itineraryId, int userId, int hotelId) {
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);
        return hotelRepository.deleteHotel(itineraryId, hotelId);
    }
}