package com.TravelBuddy.controller.api;

import com.TravelBuddy.model.LocationResponse;
import com.TravelBuddy.service.LocationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    // Listens on: GET /api/location/search?query=Paris&isHotel=true
    @GetMapping("/search")
    public List<LocationResponse> searchLocations(@RequestParam String query, @RequestParam(defaultValue = "false") boolean isHotel) {
        return locationService.searchLocations(query, isHotel);
    }
}