package com.TravelBuddy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.TravelBuddy.repository.*;
import com.TravelBuddy.exception.*;
import com.TravelBuddy.model.*;

@Service
public class AttractionService {
    private final ItineraryRepository itineraryRepository;
    private final LocationService locationService;
    private  final GdacsService gdacsService;
    private final AttractionRepository attractionRepository;

    public AttractionService(ItineraryRepository itineraryRepository, LocationService locationService, GdacsService gdacsService, AttractionRepository attractionRepository) {
        this.itineraryRepository = itineraryRepository;
        this.locationService = locationService;
        this.gdacsService = gdacsService;
        this.attractionRepository = attractionRepository;
    }

    /**
     * Validate attraction before adding into database
     * @param itineraryId
     * @param userId
     * @param attraction
     * @return Validation messages or the newly created attraction if successful
     */
    public Attraction createAttraction(int itineraryId, int userId, Attraction attraction) {
        // Check that user has the permission to add attraction for this selected itinerary
        checkItineraryBelongToUser(itineraryId, userId);

        // Validation checks and return meaningful response to user
        if (attraction == null) {
            throw new InvalidInputsException("Attraction cannot be empty.");
        }
        if (attraction.getAttractionName() == null || attraction.getAttractionName().trim().isEmpty()) {
            throw new InvalidInputsException("Attraction name is required.");
        }
        if (attraction.getVisitDate() == null) {
            throw new InvalidInputsException("Visit date is required.");
        }

        // Get the coordinates of the attraction
        LocationResponse location = locationService.getCoordinates(attraction.getAttractionName());
        
        // When API is unable to find coordinates of the user selected location
        if (location == null || location.getLat() == null || location.getLon() == null) {
            throw new InvalidInputsException("Could not find coordinates for location: " + attraction.getAttractionName());
        }

        attraction.setItineraryId(itineraryId);
        attraction.setLatitude(Double.parseDouble(location.getLat()));
        attraction.setLongitude(Double.parseDouble(location.getLon()));

        // Execute sql to add new attraction
        Attraction savedAttraction = attractionRepository.createAttraction(itineraryId, attraction);

        // If attraction creation is successful
        return savedAttraction;
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
     * Get all attractions belonging to an itinerary of the user
     * @param userId
     * @return all itineraries belonging to user
     */
    public List<Attraction> getAllAttraction(int itineraryId, int userId) {
        checkItineraryBelongToUser(itineraryId, userId);

        // Execute sql to add get attraction
        List<Attraction> attractions = attractionRepository.findAllAttractionsByItineraryId(itineraryId);

        // Disaster monitoring
        for (Attraction attraction : attractions) {
            String warning = gdacsService.evaluateProximityAndDateFromDisasters(
                    attraction.getLatitude(), attraction.getLongitude(), 
                    attraction.getVisitDate(), attraction.getVisitDate());
            
            attraction.setDisasterWarning(warning);
        }

        return attractions;
    }

    /**
     * Get selected attraction belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param attractionId
     * @return selected 
     */
    public Attraction getSelectedAttraction(int itineraryId, int userId, int attractionId) {
        checkItineraryBelongToUser(itineraryId, userId);
        return attractionRepository.findSelectedAttractionByAttractionId(itineraryId, attractionId);
    }


    /**
     * Update selected attraction belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param attractionId
     * @param attraction
     * @return number of rows updated in attractions
     */
    public int updateAttraction(int itineraryId, int userId, int attractionId, Attraction attraction) {
        checkItineraryBelongToUser(itineraryId, userId);
         // Validation checks and return meaningful response to user
        if (attraction == null) {
            throw new InvalidInputsException("Attraction cannot be empty.");
        }
        if (attraction.getAttractionName() == null || attraction.getAttractionName().trim().isEmpty()) {
            throw new InvalidInputsException("Attraction name is required.");
        }
        if (attraction.getVisitDate() == null) {
            throw new InvalidInputsException("Visit date is required.");
        }

        // Get the coordinates of the attraction
        LocationResponse location = locationService.getCoordinates(attraction.getAttractionName());
        
        // When API is unable to find coordinates of the user selected location
        if (location == null || location.getLat() == null || location.getLon() == null) {
            throw new InvalidInputsException("Could not find coordinates for location: " + attraction.getAttractionName());
        }

        attraction.setItineraryId(itineraryId);
        attraction.setLatitude(Double.parseDouble(location.getLat()));
        attraction.setLongitude(Double.parseDouble(location.getLon()));
        
        return attractionRepository.updateAttraction(itineraryId, attractionId, attraction);
    }

    /**
     * Deleted selected attraction belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param attractionId
     * @return number of rows updated in attractions
     */
    public int deleteAttraction(int itineraryId, int userId, int attractionId) {
        checkItineraryBelongToUser(itineraryId, userId);
        return attractionRepository.deleteAttraction(itineraryId, attractionId);
    }
}
