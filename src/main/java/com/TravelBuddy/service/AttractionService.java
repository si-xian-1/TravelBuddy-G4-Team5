package com.TravelBuddy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.TravelBuddy.repository.*;
import com.TravelBuddy.exception.*;
import com.TravelBuddy.model.*;

@Service
public class AttractionService {
    private final ItineraryRepository itineraryRepository;
    private final GdacsService gdacsService;
    private final AttractionRepository attractionRepository;

    public AttractionService(ItineraryRepository itineraryRepository, GdacsService gdacsService, AttractionRepository attractionRepository) {
        this.itineraryRepository = itineraryRepository;
        this.gdacsService = gdacsService;
        this.attractionRepository = attractionRepository;
    }

    /**
     * Validate attraction before adding into database
     * @param itineraryId
     * @param userId
     * @param attraction
     * @return attraction details created
     */
    public Attraction createAttraction(int itineraryId, int userId, Attraction attraction) {
        // Check that user has the permission to add attraction for this selected itinerary
        checkItineraryBelongToUser(itineraryId, userId);

        validateCoordinates(attraction);

        attraction.setItineraryId(itineraryId);

        // Execute sql to add new attraction
        return attractionRepository.createAttraction(itineraryId, attraction);
    }

    private void validateCoordinates(Attraction attraction) {
        double latitude = attraction.getLatitude();
        double longitude = attraction.getLongitude();

        if (latitude == 0.0 && longitude == 0.0) {
            throw new InvalidInputsException("Valid location coordinates must be provided from the search results.");
        }

        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new InvalidInputsException(
                    "Latitude must be between -90 and 90, and longitude between -180 and 180.");
        }
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
     * @param itineraryId
     * @param userId
     * @return list of attractions
     */
    public List<Attraction> getAllAttraction(int itineraryId, int userId) {
        checkItineraryBelongToUser(itineraryId, userId);

        List<Attraction> attractions = attractionRepository.findAllAttractionsByItineraryId(itineraryId);

        // Disaster monitoring
        for (Attraction attraction : attractions) {
            try {
                String warning = gdacsService.evaluateProximityAndDateFromDisasters(
                        attraction.getLatitude(), attraction.getLongitude(),
                        attraction.getVisitDate(), attraction.getVisitDate());

                attraction.setDisasterWarning(warning);
            } catch (IllegalStateException exception) {
                attraction.setDisasterWarning("disaster-service failure");
            }
        }

        return attractions;
    }

    /**
     * Get selected attraction belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param attractionId
     * @return attraction details of the requested attraction id
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
     * @return rows of edited record
     */
    public int updateAttraction(int itineraryId, int userId, int attractionId, Attraction attraction) {
        checkItineraryBelongToUser(itineraryId, userId);

        validateCoordinates(attraction);

        attraction.setItineraryId(itineraryId);
        
        return attractionRepository.updateAttraction(itineraryId, attractionId, attraction);
    }

    /**
     * Deleted selected attraction belonging to an itinerary of the user
     * @param itineraryId
     * @param userId
     * @param attractionId
     * @return 0 for unsuccessful delete, else return non-zero for successful delete
     */
    public int deleteAttraction(int itineraryId, int userId, int attractionId) {
        checkItineraryBelongToUser(itineraryId, userId);
        return attractionRepository.deleteAttraction(itineraryId, attractionId);
    }
}