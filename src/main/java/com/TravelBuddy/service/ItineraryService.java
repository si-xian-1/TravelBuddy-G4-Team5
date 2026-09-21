package com.TravelBuddy.service;

import org.springframework.stereotype.Service;
import com.TravelBuddy.model.Itinerary;
import com.TravelBuddy.repository.ItineraryRepository;
import com.TravelBuddy.model.LocationResponse;
import com.TravelBuddy.repository.TravelerRepository;

@Service
public class ItineraryService {
    private final ItineraryRepository itineraryRepository;
    private final LocationService locationService;
    private final TravelerRepository travelerRepository;

    public ItineraryService(ItineraryRepository itineraryRepository, LocationService locationService, TravelerRepository travelerRepository) {
        this.itineraryRepository = itineraryRepository;
        this.locationService = locationService;
        this.travelerRepository = travelerRepository;
    }

    public String createAttraction(Itinerary itinerary, int userId) {
        LocationResponse location = locationService.getCoordinates(itinerary.getDisplayName());
        itinerary.setUserId(userId);

        if (location != null) {
            itinerary.setLatitude(Double.parseDouble(location.getLat()));
            itinerary.setLongitude(Double.parseDouble(location.getLon()));
            itineraryRepository.createAttraction(itinerary);
            return "Success";
        }

        return "Invalid location";
        
    }

    public int createHotel(Itinerary itinerary) {
        return itineraryRepository.createHotel(itinerary);
    }
}
