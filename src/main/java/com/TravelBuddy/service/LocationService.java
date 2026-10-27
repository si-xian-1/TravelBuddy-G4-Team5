package com.TravelBuddy.service;

import com.TravelBuddy.model.LocationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class LocationService {

    private static final Set<String> ACCOMMODATION_TYPES = Set.of(
            "hotel", "motel", "hostel", "guest_house", "apartment",
            "apartments", "alpine_hut", "chalet", "resort", "camp_site",
            "caravan_site"
    );

    private final RestClient restClient;

    public LocationService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader("User-Agent", "TravelBuddyApp/1.0 (student-lab-project)")
                .build();
    }

    public List<LocationResponse> searchLocations(String query, boolean isHotel) {
        LocationResponse[] response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("accept-language", "en")
                        .queryParam("limit", 10)
                        .build())
                .retrieve()
                .body(LocationResponse[].class);

        if (response == null || response.length == 0) {
            return Collections.emptyList();
        }

        List<LocationResponse> results = Arrays.asList(response);

        // Bubble accommodations to the top if the frontend requested a hotel search
        if (isHotel) {
            return results.stream()
                    .sorted(Comparator.comparing(location -> !isAccommodation(location)))
                    .toList();
        }

        return results;
    }

    private boolean isAccommodation(LocationResponse location) {
        return location.getType() != null
                && ACCOMMODATION_TYPES.contains(location.getType())
                && ("tourism".equals(location.getCategory())
                        || "amenity".equals(location.getCategory()));
    }
}