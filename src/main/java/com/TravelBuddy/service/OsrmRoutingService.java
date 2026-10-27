package com.TravelBuddy.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.TravelBuddy.model.OsrmResponse;

@Service
public class OsrmRoutingService {
    private static final String DEFAULT_PROFILE = "driving";
    private static final double MAX_ROUTE_COORDINATE_DELTA = 5.0;

    private final RestClient restClient;
    private final String baseUrl = "https://router.project-osrm.org";

    public OsrmRoutingService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Requests 3 OSRM route alternatives between the user and the AI-selected destination.
     */
    public String getRouteAlternatives(double userLat, double userLon, double destLat, double destLon, String profile) {
        double latDiff = Math.abs(userLat - destLat);
        double lonDiff = Math.abs(userLon - destLon);
        if (latDiff > MAX_ROUTE_COORDINATE_DELTA || lonDiff > MAX_ROUTE_COORDINATE_DELTA) {
            return "Destination shelter is outside terrestrial navigation range (>500km).";
        }

        // The public route contract accepts walking/foot, but OSRM is queried
        // through the application's supported driving profile for both options.
        String effectiveProfile = normalizeProfile(profile);

        try {
            String coordString = String.format(Locale.US, "%.5f,%.5f;%.5f,%.5f", userLon, userLat, destLon, destLat);
            OsrmResponse response = restClient.get()
                    .uri(baseUrl + "/route/v1/" + effectiveProfile + "/" + coordString
                            + "?overview=full&geometries=geojson&steps=true&alternatives=3")
                    .header("Accept-Encoding", "identity")
                    .header("User-Agent", "TravelBuddy/1.0")
                    .retrieve()
                    .body(OsrmResponse.class);
            if (response == null || !"Ok".equalsIgnoreCase(response.getCode())
                    || response.getRoutes() == null || response.getRoutes().isEmpty()) {
                return "No valid " + profile + " routes found to the destination.";
            }

            StringBuilder routeSummaries = new StringBuilder("Available " + profile + " routes:\n");
            int optionNum = 1;
            for (OsrmResponse.Route route : response.getRoutes()) {
                double distKm = route.getDistance() != null ? route.getDistance() / 1000.0 : 0;
                long mins = route.getDuration() != null ? Math.round(route.getDuration() / 60.0) : 0;
                routeSummaries.append(String.format("Route %d: %.1f km (%d mins). ", optionNum++, distKm, mins));
                if (route.getLegs() != null && !route.getLegs().isEmpty()
                        && route.getLegs().get(0).getSteps() != null) {
                    routeSummaries.append("Key maneuvers: ");
                    route.getLegs().get(0).getSteps().stream().limit(3).forEach(step -> {
                        if (step.getManeuver() != null && step.getName() != null && !step.getName().isEmpty()) {
                            routeSummaries.append(step.getManeuver().getType()).append(" onto ")
                                    .append(step.getName()).append("; ");
                        }
                    });
                }
                routeSummaries.append("\n");
            }
            return routeSummaries.toString();
        } catch (Exception e) {
            System.err.println("[OSRM Error] Routing call failed: " + e.getMessage());
            return "OSRM API temporarily unavailable.";
        }
    }

    /**
     * Keeps unsupported or blank profiles from producing an invalid OSRM URL.
     * Walking requests intentionally retain the existing driving fallback.
     */
    private String normalizeProfile(String profile) {
        return DEFAULT_PROFILE;
    }
}
