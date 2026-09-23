package com.TravelBuddy.service;

import com.TravelBuddy.model.LocationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class LocationService {

    private final RestClient restClient;

    public LocationService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader("User-Agent", "TravelBuddyApp/1.0 (student-lab-project)")
                .build();
    }

    public List<LocationResponse> searchLocations(String query) {
        LocationResponse[] response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("accept-language", "en")
                        .build())
                .retrieve()
                .body(LocationResponse[].class);

        return response != null ? Arrays.asList(response) : Collections.emptyList();
    }

    // Returns the single best matching location (first result)
    // Need to edit in future to change it to when user click recommendation, return index number to get Coordinates or something like that
    // getCoordinates(int index, List<LocationResponse> results)
    public LocationResponse getCoordinates(String locationName) {
        List<LocationResponse> results = searchLocations(locationName);
        return results.isEmpty() ? null : results.get(0);
    }
}