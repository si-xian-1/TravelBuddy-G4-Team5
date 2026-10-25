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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.TravelBuddy.model.ProximitySummary;

@Service
public class GdacsService {
    private final RestClient restClient;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JavaTimeModule());
    private final ResourceLoader resourceLoader;
    private final String base_url = "https://www.gdacs.org/gdacsapi/api";
    // If disaster is 120km away from the itienarary plan, user should be alerted
    private final double danger_distance_Km = 120.0;
    // Stores all the disastes gathered
    private List<DisasterEvents> activeDisasters = List.of();

    // For demo purpose to stimulate scenario
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
     * @return list of disasters
     */
    public Disaster getDisasterEvents() {
        // Can be set in env
        if (isDemoMode) {
            System.out.println("DEMO MODE ACTIVE: Loading mock disaster data from " + demoDisasterData);
            return loadMockDisasters();
        }

        try {
            // Attempt to connect to gdacs api to retrieve results before using mock as fallback
            Disaster response = restClient.get()
                    .uri(base_url + "/Events/geteventlist/events4app")
                    .header("Accept", "application/json")
                    .header("User-Agent", "TravelBuddy/1.0 (student-lab-project)")
                    .retrieve()
                    .body(Disaster.class);

            // Check if retrival was successful
            if (response != null && response.getAllDisastersEvents() != null && !response.getAllDisastersEvents().isEmpty()) {
                System.out.println("Successfully retrieved " + response.getAllDisastersEvents().size() + " active disasters from GDACS API.");
                return response;
            }
        } catch (Exception ex) {
            System.out.println("Error calling GDACS API" + ex.getMessage());
            System.out.println("Falling back to mock disaster data: " + demoDisasterData);
            return loadMockDisasters();
        }

        return loadMockDisasters();
    }

    /**
     * Reads mock disaster data from demo_disasters.json
     */
    private Disaster loadMockDisasters() {
        try {
            Resource resource = resourceLoader.getResource(demoDisasterData);
            if (!resource.exists()) {
                throw new IllegalStateException("Mock disaster resource does not exist: " + demoDisasterData);
            }

            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readValue(is, Disaster.class);
            }
        } catch (Exception ex) {
            System.out.println("Failed to read mock disaster JSON file '" + demoDisasterData
                    + "': " + ex.getMessage());
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
            if (response != null && response.getAllDisastersEvents() != null && !response.getAllDisastersEvents().isEmpty()) {
                this.activeDisasters = response.getAllDisastersEvents();

                for (DisasterEvents d : activeDisasters) {
                    DisasterDetails disasterDetails = d.getDisasterDetails();
                    LocationCoordinates disasterSpecifiedLocation = d.getLocationCoordinates();

                    if (disasterDetails != null && disasterSpecifiedLocation != null) {
                        String typeName = disasterDetails.getDisasterType() != null
                                ? disasterDetails.getDisasterType().getDisasterName()
                                : "Disaster";
                        System.out.printf(
                                "Disaster Info %d: Specific coordinates: %s, Location: %s, Type: %s, Alert Level: %s, Start: %s, End: %s, Affected countries: %s\n",
                                disasterDetails.getDisasterId(),
                                Arrays.toString(disasterSpecifiedLocation.getCoordinates()),
                                disasterDetails.getDisasterCountryLocation(),
                                typeName,
                                disasterDetails.getAlertLevel(), 
                                disasterDetails.getDisasterStartDate(),
                                disasterDetails.getDisasterEndDate(), 
                                disasterDetails.getAffectedCountries());
                    }
                }

                System.out.println("Retrieval completed: " + activeDisasters.size() + " disasters detected.");
            } else {
                System.out.println("No active disaster events detected from GDACS api.");
                this.activeDisasters = List.of();
            }
        } catch (Exception e) {
            // Prevents application from crashing in the event no disaster is retrieved
            System.out.println("Error retrieving disasters information from GDACS api: " + e.getMessage());
            this.activeDisasters = List.of();
        }
    }

    /**
     * Retrieve stored active disaster events without calling GDACS API again
     * @return list of active disasters
     */
    public List<DisasterEvents> getActiveDisasters() {
        if (this.activeDisasters == null || this.activeDisasters.isEmpty()) {
            loadDisastersOnStartup();
        }
        return this.activeDisasters;
    }

    /**
     * Calculates distance and danger status for a traveler location relative to all active disasters.
     * @param userLat Traveler latitude
     * @param userLong Traveler longitude
     * @return ProximitySummary containing danger status, warning string, and nearest disaster metrics
     */
    public ProximitySummary calculateProximitySummary(Double userLat, Double userLong) {
        // Invalid coordinates
        if (userLat == null || userLong == null) {
            for (DisasterEvents disaster : activeDisasters) {
                disaster.setDistanceKm(null);
                disaster.setInDangerZone(null);
            }
            return new ProximitySummary(false, null, null, "None", null);
        }

        LocalDateTime now = LocalDateTime.now();
        String warning = evaluateProximityAndDateFromDisasters(userLat, userLong, now, now);
        double minDistance = Double.MAX_VALUE;
        String nearestName = "None";
        DisasterEvents nearestEvent = null;

        for (DisasterEvents disaster : activeDisasters) {
            // Check through if data is valid
            if (disaster.getLocationCoordinates() == null
                    || disaster.getLocationCoordinates().getCoordinates() == null) {
                continue;
            }

            double[] coords = disaster.getLocationCoordinates().getCoordinates();
            if (coords.length < 2) {
                continue;
            }

            // Calculate user proximity from the disaster
            double longDiff = userLong - coords[0];
            double latDiff = userLat - coords[1];
            double dist = Math.sqrt((longDiff * longDiff) + (latDiff * latDiff)) * 111.0;
            double roundedDist = (double) Math.round(dist);
            disaster.setDistanceKm(roundedDist);
            disaster.setInDangerZone(roundedDist <= danger_distance_Km);

            // Compute which disaster zone user is closest to
            if (dist < minDistance) {
                minDistance = dist;
                nearestEvent = disaster;

                String disasterType = (disaster.getDisasterDetails() != null
                        && disaster.getDisasterDetails().getDisasterType() != null)
                                ? disaster.getDisasterDetails().getDisasterType().getDisasterName()
                                : "Disaster";

                String disasterCountry = (disaster.getDisasterDetails() != null
                        && disaster.getDisasterDetails().getDisasterCountryLocation() != null)
                                ? disaster.getDisasterDetails().getDisasterCountryLocation()
                                : "";

                nearestName = disasterType + (disasterCountry.isEmpty() ? "" : " (" + disasterCountry + ")");
            }
        }

        // Compute selected disaster closest to user
        Double nearestDist = (minDistance == Double.MAX_VALUE) ? null : (double) Math.round(minDistance);
        boolean isDanger = (warning != null && !warning.isEmpty())
                || (nearestDist != null && nearestDist <= danger_distance_Km);

        return new ProximitySummary(isDanger, warning, nearestDist, nearestName, nearestEvent);
    }

    /**
     * Get user itinerary plan to check against disaster location
     * @param latitude
     * @param longtitude
     * @param startDate
     * @param endDate
     * @return Null - itinerary plans are not in danger zone. Warning message to alert user that their plans could be affected
     */
    public String evaluateProximityAndDateFromDisasters(
            double latitude, double longtitude, LocalDateTime startDate, LocalDateTime endDate) {
        //List<DisasterEvents> disasters = getActiveDisasters();
        for (DisasterEvents disaster : activeDisasters) {
            // Check valid coordinates
            if (disaster.getLocationCoordinates() == null
                    || disaster.getLocationCoordinates().getCoordinates() == null) {
                continue;
            }

            // Pythagoras theorem to calculate distance of itinerary locations to each disasters location
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

            // Retrieve trip duration to check overlap with disaster happening duration
            LocalDate tripStart = startDate.toLocalDate();
            LocalDate tripEnd = endDate.toLocalDate();
            LocalDate disasterStartDate = disaster.getDisasterDetails().getDisasterStartDate().toLocalDate();
            LocalDate disasterEndDate = disaster.getDisasterDetails().getDisasterEndDate().toLocalDate();
            // Assume trip takes at least 1 month to recovers from disaster happenings
            LocalDate recoveryEndDate = disasterEndDate.plusMonths(1);

            // Check if itinerary dates concide with disaster occured date
            boolean isOverlapping = !tripStart.isAfter(recoveryEndDate) && !tripEnd.isBefore(disasterStartDate);

            if (isOverlapping) {
                String disasterType = disaster.getDisasterDetails().getDisasterType() != null
                        ? disaster.getDisasterDetails().getDisasterType().getDisasterName()
                        : "Disaster";
                        
                if (!tripStart.isAfter(disasterEndDate)) {
                    return String.format(
                            "WARNING - Active disaster type: %s affecting %s (Alert Level: %s). Please review it or let our travelbuddy AI agent help you with it.",
                            disasterType,
                            disaster.getDisasterDetails().getAffectedCountries(),
                            disaster.getDisasterDetails().getAlertLevel());
                } else {
                    return String.format(
                            "NOTICE - Disaster type: %s recently occurred at %s. You can still proceed with this itinerary item but due to potential infrastructure damage, you may wish to review it.",
                            disasterType,
                            disaster.getDisasterDetails().getAffectedCountries());
                }
            }
        }

        // When itinerary is not affected by any disaster
        return null;
    }
}
