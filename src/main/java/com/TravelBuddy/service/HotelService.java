package com.TravelBuddy.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.model.Hotel;
import com.TravelBuddy.model.LocationResponse;
import com.TravelBuddy.repository.HotelRepository;

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
     *
     * @param itineraryId
     * @param userId
     * @param hotel
     * @return Validation messages or the newly created hotel if successful
     */
    public Hotel createHotel(int itineraryId, int userId, Hotel hotel) {
        // Check that user has the permission to add hotel for this selected itinerary
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        if (hotel.getCheckIn().isAfter(hotel.getCheckOut())) {
            throw new InvalidInputsException("Check-in time cannot be after check-out time.");
        }

        // Get the coordinates of the hotel
        LocationResponse location = locationService.getCoordinates(hotel.getHotelName());

        // When API is unable to find coordinates of the user selected location
        if (location == null || location.getLat() == null || location.getLon() == null) {
            throw new InvalidInputsException("Could not find coordinates for location: " + hotel.getHotelName());
        }

        hotel.setItineraryId(itineraryId);
        hotel.setLatitude(Double.parseDouble(location.getLat()));
        hotel.setLongitude(Double.parseDouble(location.getLon()));

        // Execute sql to add new hotel
        Hotel savedHotel = hotelRepository.createHotel(itineraryId, hotel);

        // Disaster monitoring
        savedHotel.setDisasterWarning(gdacsService.evaluateProximityAndDateFromDisasters(
                savedHotel.getLatitude(), savedHotel.getLongitude(),
                savedHotel.getCheckIn(), savedHotel.getCheckOut()));

        // If hotel creation is successful
        return savedHotel;
    }

    /**
     * Get all hotels belonging to an itinerary of the user
     *
     * @param userId
     * @return all hotels belonging to user
     */
    public List<Hotel> getAllHotel(int itineraryId, int userId) {
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);
        return hotelRepository.findAllHotelsByItineraryId(itineraryId);
    }

    /**
     * Get selected hotel belonging to an itinerary of the user
     *
     * @param itineraryId
     * @param userId
     * @param hotelId
     * @return selected
     */
    public Hotel getSelectedHotel(int itineraryId, int userId, int hotelId) {
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);
        return hotelRepository.findSelectedHotelByItineraryId(hotelId, itineraryId);
    }

    /**
     * Update an existing hotel after validating the user, hotel details and
     * location. Recalculate its disaster warning afterwards.
     *
     * @param itineraryId itinerary containing the hotel
     * @param userId user requesting the update
     * @param hotelId hotel to update
     * @param hotel updated hotel details
     * @return updated hotel including its disaster warning
     */
    public Hotel updateHotel(
            int itineraryId,
            int userId,
            int hotelId,
            Hotel hotel) {

        // Verify that the user owns the itinerary.
        itineraryService.checkItineraryBelongToUser(
                itineraryId, userId
        );

        // Validate the request body.
        if (hotel == null) {
            throw new InvalidInputsException(
                    "Hotel details cannot be empty."
            );
        }

        if (hotel.getHotelName() == null
                || hotel.getHotelName().trim().isEmpty()) {
            throw new InvalidInputsException(
                    "Hotel name is required."
            );
        }

        if (hotel.getCheckIn() == null
                || hotel.getCheckOut() == null) {
            throw new InvalidInputsException(
                    "Check-in and check-out dates are required."
            );
        }

        if (!hotel.getCheckOut().isAfter(hotel.getCheckIn())) {
            throw new InvalidInputsException(
                    "Check-out time must be after check-in time."
            );
        }

        // Verify that this hotel belongs to the selected itinerary.
        Hotel existingHotel
                = hotelRepository.findSelectedHotelByItineraryId(
                        hotelId, itineraryId
                );

        if (existingHotel == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hotel not found in this itinerary."
            );
        }

        // Resolve the hotel location again if its name has changed.
        LocationResponse location
                = locationService.getCoordinates(hotel.getHotelName().trim());

        if (location == null
                || location.getLat() == null
                || location.getLon() == null) {
            throw new InvalidInputsException(
                    "Could not find coordinates for location: "
                    + hotel.getHotelName()
            );
        }

        double latitude;
        double longitude;

        try {
            latitude = Double.parseDouble(location.getLat());
            longitude = Double.parseDouble(location.getLon());
        } catch (NumberFormatException e) {
            throw new InvalidInputsException(
                    "The location service returned invalid coordinates."
            );
        }

        // Set server-controlled values instead of trusting the request body.
        hotel.setHotelId(hotelId);
        hotel.setItineraryId(itineraryId);
        hotel.setHotelName(hotel.getHotelName().trim());
        hotel.setLatitude(latitude);
        hotel.setLongitude(longitude);

        // Persist the update.
        int rowsUpdated
                = hotelRepository.updateHotel(itineraryId, hotel);

        if (rowsUpdated == 0) {
            // A zero count can also mean the values were unchanged.
            // Recheck existence before treating it as a missing hotel.
            Hotel currentHotel
                    = hotelRepository.findSelectedHotelByItineraryId(
                            hotelId, itineraryId
                    );

            if (currentHotel == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hotel not found in this itinerary."
                );
            }
        }

        // Retrieve the persisted data.
        Hotel updatedHotel
                = hotelRepository.findSelectedHotelByItineraryId(
                        hotelId, itineraryId
                );

        if (updatedHotel == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hotel was not found after updating."
            );
        }

        // Refresh the disaster warning using the updated location and dates.
        updatedHotel.setDisasterWarning(
                gdacsService.evaluateProximityAndDateFromDisasters(
                        updatedHotel.getLatitude(),
                        updatedHotel.getLongitude(),
                        updatedHotel.getCheckIn(),
                        updatedHotel.getCheckOut()
                )
        );

        return updatedHotel;
    }

    /**
     * Delete a hotel only if the requesting user owns its itinerary.
     *
     * @param itineraryId itinerary containing the hotel
     * @param userId user requesting deletion
     * @param hotelId hotel to delete
     */
    public void deleteHotel(
            int itineraryId,
            int userId,
            int hotelId) {

        // Verify itinerary ownership first.
        itineraryService.checkItineraryBelongToUser(
                itineraryId, userId
        );

        // Verify that the hotel belongs to this itinerary.
        Hotel existingHotel
                = hotelRepository.findSelectedHotelByItineraryId(
                        hotelId, itineraryId
                );

        if (existingHotel == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hotel not found in this itinerary."
            );
        }

        // Delete the selected hotel.
        int rowsDeleted
                = hotelRepository.deleteHotel(hotelId, itineraryId);

        if (rowsDeleted == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hotel could not be deleted because it no longer exists."
            );
        }
    }

    /**
     * Update selected hotel belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param hotelId
     * @param hotel
     * @return number of rows updated in hotels
     */
    public int updateHotel(int itineraryId, int userId, int hotelId, Hotel hotel) {
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

        if (hotel.getCheckIn().isAfter(hotel.getCheckOut())) {
            throw new InvalidInputsException("Check-in time cannot be after check-out time.");
        }

        // Get the coordinates of the hotel
        LocationResponse location = locationService.getCoordinates(hotel.getHotelName());
        
        // When API is unable to find coordinates of the user selected location
        if (location == null || location.getLat() == null || location.getLon() == null) {
            throw new InvalidInputsException("Could not find coordinates for location: " + hotel.getHotelName());
        }

        hotel.setItineraryId(itineraryId);
        hotel.setLatitude(Double.parseDouble(location.getLat()));
        hotel.setLongitude(Double.parseDouble(location.getLon()));
        
        return hotelRepository.updateHotel(itineraryId, hotelId, hotel);
    }

    /**
     * Deleted selected hotel belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param hotelId
     * @return number of rows updated in hotels
     */
    public int deleteHotel(int itineraryId, int userId, int hotelId) {
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);
        return hotelRepository.deleteHotel(itineraryId, hotelId);
    }
}
