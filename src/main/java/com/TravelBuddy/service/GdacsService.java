package com.TravelBuddy.service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.TravelBuddy.model.Disaster;
import com.TravelBuddy.model.Disaster.DisasterDetails;
import com.TravelBuddy.model.Disaster.DisasterEvents;
import com.TravelBuddy.model.Disaster.LocationCoordinates;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.TravelBuddy.model.ProximitySummary;

@Service
public class GdacsService {
    // External api connecting to GDACS api
    private final RestClient restClient;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final ResourceLoader resourceLoader;
    private final String base_url = "https://www.gdacs.org/gdacsapi/api";
    // If disaster is 120km away from the itienarary plan, user should be alerted
    private final double danger_distance_Km = 120.0;
    // Stores all the disastes gathered
    private List<DisasterEvents> activeDisasters = List.of();

    @Value("${travelbuddy.gdacs.mock-mode}")
    private boolean isDemoMode;

    @Value("${travelbuddy.gdacs.mock-file-path}")
    private String demoDisasterData;

    public GdacsService(RestClient.Builder builder, ResourceLoader resourceLoader) {
        this.restClient = builder.build();
        this.resourceLoader = resourceLoader;
    }

    /**
     * Connect to GDACS API to retrieve all disasters location and details
     *
     * @return list of disasters
     */
    public Disaster getDisasterEvents() {
        if (isDemoMode) {
            System.out.println("DEMO MODE ACTIVE: Loading mock disaster data from " + demoDisasterData);
            return loadMockDisasters();
        }

        return restClient.get()
                .uri(base_url + "/Events/geteventlist/events4app")
                .retrieve()
                .body(Disaster.class);
    }

    /**
     * Reads mock disaster data safely from classpath JSON
     */
    private Disaster loadMockDisasters() {
        try {
            Resource resource = resourceLoader.getResource(demoDisasterData);
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readValue(is, Disaster.class);
            }
        } catch (Exception ex) {
            System.out.println("Failed to read mock disaster JSON file: " + ex.getMessage());
            return new Disaster();
        }
    }

    /**
     * Retrieve all disaster information when application start up, reduce lag time
     */
    public void loadDisastersOnStartup() {
        try {
            Disaster response = getDisasterEvents();

            // Check if there is any disaster data to retrieve
            if (response != null && response.getAllDisastersEvents() != null) {
                this.activeDisasters = response.getAllDisastersEvents();

                for (DisasterEvents d : activeDisasters) {
                    DisasterDetails disasterDetails = d.getDisasterDetails();
                    LocationCoordinates disasterSpecifiedLocation = d.getLocationCoordinates();

                    System.out.printf(
                            "Disaster Info %d: Specific coordinates: %s, Location: %s, Type: %s, Alert Level: %s, Start: %s, End: %s, Affected countries: %s\n",
                            disasterDetails.getDisasterId(),
                            Arrays.toString(disasterSpecifiedLocation.getCoordinates()),
                            disasterDetails.getDisasterCountryLocation(),
                            disasterDetails.getDisasterType().getDisasterName(),
                            disasterDetails.getAlertLevel(), disasterDetails.getDisasterStartDate(),
                            disasterDetails.getDisasterEndDate(), disasterDetails.getAffectedCountries());
                }

                System.out.println("Retrieval completed: " + activeDisasters.size() + " disasters detected.");
            }
        } catch (Exception e) {
            System.out.println("Error retrieving disasters information from GDACS api: " + e.getMessage());
            // Prevents application from crashing in the event no disaster is retrieved
            this.activeDisasters = List.of();
        }
    }

    /**
     * Retrieve active disaster events stored in memory without calling GDACS API
     * again
     * 
     * @return in-memory list of active disasters
     */
    public List<DisasterEvents> getActiveDisasters() {
        if (this.activeDisasters == null || this.activeDisasters.isEmpty()) {
            loadDisastersOnStartup();
        }
        return this.activeDisasters;
    }

    /**
     * Calculates distance and danger status for a traveler location relative to all
     * active disasters.
     * Consolidates proximity logic into a single method to eliminate code
     * duplication across controllers.
     *
     * @param userLat Traveler latitude
     * @param userLon Traveler longitude
     * @return ProximitySummary containing danger status, warning string, and
     *         nearest disaster metrics
     */
    public ProximitySummary calculateProximitySummary(Double userLat, Double userLon) {
        if (userLat == null || userLon == null) {
            return new ProximitySummary(false, null, null, "None", null);
        }

        LocalDateTime now = LocalDateTime.now();
        String warning = evaluateProximityAndDateFromDisasters(userLat, userLon, now, now);
        List<DisasterEvents> disasters = getActiveDisasters();

        double minDistance = Double.MAX_VALUE;
        String nearestName = "None";
        DisasterEvents nearestEvent = null;

        for (DisasterEvents disaster : disasters) {
            if (disaster.getLocationCoordinates() == null
                    || disaster.getLocationCoordinates().getCoordinates() == null) {
                continue;
            }
            double[] coords = disaster.getLocationCoordinates().getCoordinates();
            if (coords.length < 2) {
                continue;
            }
            double lonDiff = userLon - coords[0];
            double latDiff = userLat - coords[1];
            double dist = Math.sqrt((lonDiff * lonDiff) + (latDiff * latDiff)) * 111.0;

            if (dist < minDistance) {
                minDistance = dist;
                nearestEvent = disaster;
                String dType = (disaster.getDisasterDetails() != null
                        && disaster.getDisasterDetails().getDisasterType() != null)
                                ? disaster.getDisasterDetails().getDisasterType().getDisasterName()
                                : "Disaster";
                String dCountry = (disaster.getDisasterDetails() != null
                        && disaster.getDisasterDetails().getDisasterCountryLocation() != null)
                                ? disaster.getDisasterDetails().getDisasterCountryLocation()
                                : "";
                nearestName = dType + (dCountry.isEmpty() ? "" : " (" + dCountry + ")");
            }
        }

        Double nearestDist = (minDistance == Double.MAX_VALUE) ? null : (double) Math.round(minDistance);
        boolean isDanger = (warning != null && !warning.isEmpty())
                || (nearestDist != null && nearestDist <= danger_distance_Km);

        return new ProximitySummary(isDanger, warning, nearestDist, nearestName, nearestEvent);
    }

    /**
     * Get user itinerary plan to check against disaster location
     * 
     * @param latitude
     * @param longtitude
     * @param startDate
     * @param endDate
     * @return Null - itinerary plans are not in danger zone. Warning message to
     *         alert user that their plans could be affected
     */
    public String evaluateProximityAndDateFromDisasters(
            double latitude, double longtitude, LocalDateTime startDate, LocalDateTime endDate) {
        List<DisasterEvents> disasters = getActiveDisasters();
        for (DisasterEvents disaster : disasters) {
            if (disaster.getLocationCoordinates() == null
                    || disaster.getLocationCoordinates().getCoordinates() == null) {
                continue;
            }
            // Pythagoras theorem to calculate distance of itinerary locations to each
            // disasters location
            double[] disasterCoordinates = disaster.getLocationCoordinates().getCoordinates();
            if (disasterCoordinates.length < 2) {
                continue;
            }
            double longtitudeDiff = longtitude - disasterCoordinates[0];
            double latitudeDiff = latitude - disasterCoordinates[1];
            double proximityDistance = Math.sqrt((longtitudeDiff * longtitudeDiff) + (latitudeDiff * latitudeDiff))
                    * 111.0;

            // Check if the itinerary location is in the danger zone
            if (proximityDistance > danger_distance_Km) {
                continue;
            }

            if (disaster.getDisasterDetails() == null ||
                    disaster.getDisasterDetails().getDisasterStartDate() == null ||
                    disaster.getDisasterDetails().getDisasterEndDate() == null) {
                continue;
            }

            // Ignore time in comparing trip and disaster date
            LocalDate tripStart = startDate.toLocalDate();
            LocalDate tripEnd = endDate.toLocalDate();
            LocalDate disasterStartDate = disaster.getDisasterDetails().getDisasterStartDate().toLocalDate();
            LocalDate disasterEndDate = disaster.getDisasterDetails().getDisasterEndDate().toLocalDate();

            // Set 1 month away from disaster to still send warning to user, since there
            // could be potential damages to infrastructures
            LocalDate recoveryEndDate = disasterEndDate.plusMonths(1);

            // Check if itinerary dates concide with disaster occured date
            boolean isOverlapping = !tripStart.isAfter(recoveryEndDate) && !tripEnd.isBefore(disasterStartDate);

            if (isOverlapping) {
                String dType = disaster.getDisasterDetails().getDisasterType() != null
                        ? disaster.getDisasterDetails().getDisasterType().getDisasterName()
                        : "Disaster";
                if (!tripStart.isAfter(disasterEndDate)) {
                    return String.format(
                            "WARNING - Active disaster type: %s affecting %s (Alert Level: %s). Please review it or let our travelbuddy AI agent help you with it.",
                            dType,
                            disaster.getDisasterDetails().getAffectedCountries(),
                            disaster.getDisasterDetails().getAlertLevel());
                } else {
                    return String.format(
                            "NOTICE - Disaster type: %s recently occurred at %s. You can still proceed with this itinerary item but due to potential infrastructure damage, you may wish to review it.",
                            dType,
                            disaster.getDisasterDetails().getAffectedCountries());
                }
            }
        }

        // When itinerary is not affected by any disaster
        return null;
    }
}
