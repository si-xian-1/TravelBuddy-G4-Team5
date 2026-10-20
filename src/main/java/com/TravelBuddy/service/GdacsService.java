package com.TravelBuddy.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.TravelBuddy.model.Disaster;
import com.TravelBuddy.model.Disaster.DisasterDetails;
import com.TravelBuddy.model.Disaster.DisasterEvents;
import com.TravelBuddy.model.Disaster.LocationCoordinates;

@Service
public class GdacsService {
    // External api connecting to GDACS api
    private final RestClient restClient;
    private final String base_url = "https://www.gdacs.org/gdacsapi/api";
    // If disaster is 120km away from the itienarary plan, user should be alerted
    private final double danger_distance_Km = 120.0;
    // Stores all the disastes gathered
    private List<DisasterEvents> activeDisasters = List.of();

    public GdacsService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    /**
     * Connect to GDACS API to retrieve all disasters location and details
     * @return list of disasters
     */
    public Disaster getDisasterEvents() {
        return restClient.get()
                .uri(base_url + "/Events/geteventlist/events4app")
                .retrieve()
                .body(Disaster.class);
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

                    System.out.printf("Disaster Info %d: Specific coordinates: %s, Location: %s, Type: %s, Alert Level: %s, Start: %s, End: %s, Affected countries: %s\n",
                        disasterDetails.getDisasterId(), Arrays.toString(disasterSpecifiedLocation.getCoordinates()), disasterDetails.getDisasterCountryLocation(), disasterDetails.getDisasterType().getDisasterName(),
                        disasterDetails.getAlertLevel(), disasterDetails.getDisasterStartDate(), disasterDetails.getDisasterEndDate(), disasterDetails.getAffectedCountries());
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
     * Get user itinerary plan to check against disaster location
     * @param latitude
     * @param longtitude
     * @param startDate
     * @param endDate
     * @return Null - itinerary plans are not in danger zone. Warning message to alert user that their plans could be affected
     */
    public String evaluateProximityAndDateFromDisasters(
            double latitude, double longtitude, LocalDateTime startDate, LocalDateTime endDate) {
        for (DisasterEvents disaster : activeDisasters) {
            // Pythagoras theorem to calculate distance of itinerary locations to each disasters location
            double[] disasterCoordinates = disaster.getLocationCoordinates().getCoordinates();
            double longtitudeDiff = longtitude - disasterCoordinates[0];
            double latitudeDiff = latitude - disasterCoordinates[1];
            double proximityDistance = Math.sqrt((longtitudeDiff * longtitudeDiff) + (latitudeDiff * latitudeDiff)) * 111.0;
            
            // Check if the itinerary location is in the danger zone
            if (proximityDistance > danger_distance_Km) {
                continue; 
            }

            // Ignore time in comparing trip and disaster date
            LocalDate tripStart = startDate.toLocalDate();
            LocalDate tripEnd = endDate.toLocalDate();
            LocalDate disasterStartDate = disaster.getDisasterDetails().getDisasterStartDate().toLocalDate();
            LocalDate disasterEndDate = disaster.getDisasterDetails().getDisasterEndDate().toLocalDate();

            // Set 1 month away from disaster to still send warning to user, since there could be potential damages to infrastructures
            LocalDate recoveryEndDate = disasterEndDate.plusMonths(1);

            // Check if itinerary dates concide with disaster occured date
            boolean isOverlapping = !tripStart.isAfter(recoveryEndDate) && !tripEnd.isBefore(disasterStartDate);

            if (isOverlapping) {
                if (!tripStart.isAfter(disasterEndDate)) {
                    return String.format("WARNING - Active disaster type: %s affecting %s (Alert Level: %s). Please review it or let our travelbuddy AI agent help you with it.", 
                            disaster.getDisasterDetails().getDisasterType().getDisasterName(),
                            disaster.getDisasterDetails().getAffectedCountries(),
                            disaster.getDisasterDetails().getAlertLevel());
                } else {
                    return String.format("NOTICE - Disaster type: %s recently occurred at %s. You can still proceed with this itinerary item but due to potential infrastructure damage, you may wish to review it.", 
                            disaster.getDisasterDetails().getDisasterType().getDisasterName(),
                            disaster.getDisasterDetails().getAffectedCountries());
                }
            }
        }

        // When itinerary is not affected by any disaster
        return null;
    }
}
