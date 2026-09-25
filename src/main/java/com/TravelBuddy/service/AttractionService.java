package com.TravelBuddy.service;

import org.springframework.stereotype.Service;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.model.Attraction;
import com.TravelBuddy.model.LocationResponse;
import com.TravelBuddy.repository.AttractionRepository;
import com.TravelBuddy.repository.TravelerRepository;

@Service 
public class AttractionService {
    private final AttractionRepository attractionRepository;
    private final ItineraryService itineraryService;
    private final LocationService locationService;
    private final GdacsService gdacsService;

    public AttractionService(AttractionRepository attractionRepository, GdacsService gdacsService, ItineraryService itineraryService, LocationService locationService) {
        this.attractionRepository = attractionRepository;
        this.gdacsService = gdacsService;
        this.itineraryService = itineraryService;
        this.locationService = locationService;
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
        itineraryService.checkItineraryBelongToUser(itineraryId, userId);

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

        // Disaster monitoring
        savedAttraction.setDisasterWarning(gdacsService.evaluateProximityAndDateFromDisasters(
                savedAttraction.getLatitude(), savedAttraction.getLongitude(), 
                savedAttraction.getVisitDate(), savedAttraction.getVisitDate()));

        // If attraction creation is successful
        return savedAttraction;
    }

}
