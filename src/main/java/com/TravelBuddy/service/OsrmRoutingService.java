package com.TravelBuddy.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.TravelBuddy.model.OsrmResponse;

@Service
public class OsrmRoutingService {

    private final RestClient restClient;
    private final String baseUrl = "https://router.project-osrm.org";

    public OsrmRoutingService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Requests 3 OSRM route alternatives between the user and the AI-selected destination.
     */
    public String getRouteAlternatives(double userLat, double userLon, double destLat, double destLon, String profile) {
        
        // 1. Format coordinates for OSRM (User -> Destination)
        String coordString = String.format(Locale.US, "%.5f,%.5f;%.5f,%.5f", userLon, userLat, destLon, destLat);
        
        try {
            // 2. Request 3 alternatives and turn-by-turn steps
            OsrmResponse response = restClient.get()
                    .uri(baseUrl + "/route/v1/" + profile + "/{coordinates}?overview=full&geometries=geojson&steps=true&alternatives=3", coordString)
                    .header("Accept-Encoding", "identity")
                    .header("User-Agent", "TravelBuddy/1.0")
                    .retrieve()
                    .body(OsrmResponse.class);

            if (response == null || !"Ok".equalsIgnoreCase(response.getCode()) || response.getRoutes().isEmpty()) {
                return "No valid " + profile + " routes found to the destination.";
            }

            // 3. Package the 3 routes into a clean string for Gemini to review
            StringBuilder routeSummaries = new StringBuilder("Available " + profile + " routes:\n");
            int optionNum = 1;
            
            for (OsrmResponse.Route route : response.getRoutes()) {
                double distKm = route.getDistance() != null ? route.getDistance() / 1000.0 : 0;
                long mins = route.getDuration() != null ? Math.round(route.getDuration() / 60.0) : 0;
                
                routeSummaries.append(String.format("Route %d: %.1f km (%d mins). ", optionNum++, distKm, mins));
                
                // Add key maneuver checkpoints
                if (route.getLegs() != null && !route.getLegs().isEmpty() && route.getLegs().get(0).getSteps() != null) {
                    routeSummaries.append("Key maneuvers: ");
                    route.getLegs().get(0).getSteps().stream().limit(3).forEach(step -> {
                        if (step.getManeuver() != null && step.getName() != null && !step.getName().isEmpty()) {
                            routeSummaries.append(step.getManeuver().getType()).append(" onto ").append(step.getName()).append("; ");
                        }
                    });
                }
                routeSummaries.append("\n");
            }
            
            return routeSummaries.toString();

        } catch (Exception e) {
            System.err.println("[OSRM Error] Routing call failed: " + e.getMessage());
            return "OSRM API unavailable.";
        }
    }
}