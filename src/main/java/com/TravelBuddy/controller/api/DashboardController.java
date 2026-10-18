package com.TravelBuddy.controller.api;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.TravelBuddy.model.Disaster.DisasterEvents;
import com.TravelBuddy.service.GdacsService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final GdacsService gdacsService;

    public DashboardController(GdacsService gdacsService) {
        this.gdacsService = gdacsService;
    }

    /**
     * Retrieve active disaster events and evaluate live proximity for traveler
     * location
     * 
     * @param userLat
     * @param userLon
     * @return Status code with disaster list and live distance alert summary
     */
    @GetMapping("/disaster-overview")
    public ResponseEntity<Map<String, Object>> getDisasterOverview(
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLon) {

        Map<String, Object> results = new HashMap<>();

        // Retrieve all active disasters gathered from GDACS api
        List<DisasterEvents> activeDisasters = gdacsService.getActiveDisasters();
        results.put("disasters", activeDisasters);

        // Disaster monitoring for traveler's current location
        if (userLat != null && userLon != null) {
            LocalDateTime now = LocalDateTime.now();

            // Check if user's current GPS location is in the danger zone
            String warning = gdacsService.evaluateProximityAndDateFromDisasters(userLat, userLon, now, now);
            results.put("warning", warning);

            // Calculate distance of traveler location to the nearest disaster
            double minDistance = Double.MAX_VALUE;
            String nearestDisasterName = "None";

            for (DisasterEvents disaster : activeDisasters) {
                double[] disasterCoordinates = disaster.getLocationCoordinates().getCoordinates();
                double longtitudeDiff = userLon - disasterCoordinates[0];
                double latitudeDiff = userLat - disasterCoordinates[1];
                double proximityDistance = Math.sqrt((longtitudeDiff * longtitudeDiff) + (latitudeDiff * latitudeDiff))
                        * 111.0;

                if (proximityDistance < minDistance) {
                    minDistance = proximityDistance;
                    nearestDisasterName = disaster.getDisasterDetails().getDisasterType().getDisasterName()
                            + " (" + disaster.getDisasterDetails().getDisasterCountryLocation() + ")";
                }
            }

            results.put("nearestDistanceKm", minDistance == Double.MAX_VALUE ? null : Math.round(minDistance));
            results.put("nearestDisasterName", nearestDisasterName);
        }

        // If success, returns status 200 OK with disaster overview data
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }
}
