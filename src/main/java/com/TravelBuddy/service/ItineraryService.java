package com.TravelBuddy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.TravelBuddy.exception.*;
import com.TravelBuddy.model.*;
import com.TravelBuddy.repository.*;

@Service
public class ItineraryService {
    private final TravelerRepository travelerRepository;
    private final ItineraryRepository itineraryRepository;
    private  final GdacsService gdacsService;

    public ItineraryService(ItineraryRepository itineraryRepository, TravelerRepository travelerRepository, GdacsService gdacsService) {
        this.itineraryRepository = itineraryRepository;
        this.travelerRepository = travelerRepository;
        this.gdacsService = gdacsService;
    }

    /**
     * 
     * @param itineraryId
     * @param userId
     * @return user selected itinerary with potential disaster alerts or unauthorized access message
     */
    public Itinerary getSelectedItinerary(int itineraryId, int userId) {
        // Check that user has the permission to access this selected itinerary
        checkItineraryBelongToUser(itineraryId, userId);
        
        // If authorized, retrieve the selected itinerary
        Itinerary itinerary = itineraryRepository.findSelectedItineraryByUserId(itineraryId, userId);
        
        // Disaster monitoring
        if (itinerary != null) {
            // Check Hotels
            if (itinerary.getHotels() != null) {
                for (Hotel hotel : itinerary.getHotels()) {
                    hotel.setDisasterWarning(gdacsService.evaluateProximityAndDateFromDisasters(
                            hotel.getLatitude(), hotel.getLongitude(), hotel.getCheckIn(), hotel.getCheckOut()));
                }
            }

            // Check Attractions
            if (itinerary.getAttractions() != null) {
                for (Attraction attraction : itinerary.getAttractions()) {
                    attraction.setDisasterWarning(gdacsService.evaluateProximityAndDateFromDisasters(
                            attraction.getLatitude(), attraction.getLongitude(), attraction.getVisitDate(), attraction.getVisitDate()));
                }
            }

            // Check Flights (only check arrival location)
            if (itinerary.getFlights() != null) {
                for (Flight flight : itinerary.getFlights()) {
                    flight.setDisasterWarning(gdacsService.evaluateProximityAndDateFromDisasters(
                            flight.getArrivalLatitude(), flight.getArrivalLongitude(),
                            flight.getDepartureTime(), flight.getArrivalTime()));
                }
            }
        }

        return itinerary;
    }

    /**
     *
     * @param userId
     * @return all itineraries belonging to user
     */
    public List<Itinerary> getAllItinerary(int userId) {
        return itineraryRepository.findAllItinerariesByUserId(userId);
    }

    /**
     * Validate itinerary fields
     * @param itinerary
     * @param userId
     * @return Validation messages or the newly created itinerary id
     */
    public int createItinerary(Itinerary itinerary, int userId) {
        // Validation checks and return meaningful response to user
        if (travelerRepository.getUser(userId) == null) {
            throw new InvalidInputsException("User does not exist.");
        }
        if (itinerary == null) {
            throw new InvalidInputsException("Itinerary cannot be empty.");
        }
        if (itinerary.getItineraryName() == null || itinerary.getItineraryName().trim().isEmpty()) {
            throw new InvalidInputsException("Itinerary name is required.");
        }
        if (itinerary.getStartDate() == null || itinerary.getEndDate() == null) {
            throw new InvalidInputsException("Start and end dates are required.");
        }
        if (itinerary.getStartDate().isAfter(itinerary.getEndDate())) {
            throw new InvalidInputsException("Start date cannot be after end date.");
        }

        itinerary.setUserId(userId);

        // If itinerary creation is successful
        return itineraryRepository.createItinerary(itinerary);
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
        checkItineraryBelongToUser(itineraryId, userId);

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
        Hotel savedHotel = itineraryRepository.createHotel(itineraryId, hotel);

        // Disaster monitoring
        savedHotel.setDisasterWarning(gdacsService.evaluateProximityAndDateFromDisasters(
                savedHotel.getLatitude(), savedHotel.getLongitude(), 
                savedHotel.getCheckIn(), savedHotel.getCheckOut()));

        // If hotel creation is successful
        return savedHotel;
    }

    /**
     * Verify that user is able to edit the specific itinerary
     * @param itineraryId
     * @param userId
     */
    private void checkItineraryBelongToUser(int itineraryId, int userId) {
        if (!itineraryRepository.verifyItineraryAndUser(itineraryId, userId)) {
            // Throw error that user is unauthorized to edit itinerary info
            throw new UnauthorizedUserException("You are not allowed to access this itinerary!");
        }
    }

    /**
     * Update an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param itinerary
     * @return number of rows updated in itineraries
     */
    public int updateItinerary(int itineraryId, int userId, Itinerary itinerary) {
        checkItineraryBelongToUser(itineraryId, userId);
        return itineraryRepository.updateItinerary(itineraryId, userId, itinerary);
    }

    /**
     * Delete an itinerary of the user
     * @param itineraryId
     * @param userId
     * @return number of rows updated in itineraries
     */
    public int deleteItinerary(int itineraryId, int userId) {
        checkItineraryBelongToUser(itineraryId, userId);
        return itineraryRepository.deleteItinerary(itineraryId, userId);
    }
}
