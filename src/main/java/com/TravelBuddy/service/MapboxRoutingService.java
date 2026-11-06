package com.TravelBuddy.service;

import java.util.Locale;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.TravelBuddy.model.MapboxResponse;

@Service
public class MapboxRoutingService {
    private static final String DEFAULT_PROFILE = "mapbox/driving";
    private static final double MAX_ROUTE_COORDINATE_DELTA = 5.0;

    private final RestClient restClient;
    private final String mapboxApiKey;
    private final String baseUrl = "https://api.mapbox.com/directions/v5";

    public MapboxRoutingService(
            RestClient.Builder restClientBuilder,
            @Value("${mapbox.api.key}") String mapboxApiKey) {
        this.restClient = restClientBuilder.build();
        this.mapboxApiKey = mapboxApiKey;
    }

    /**
     * Requests route alternatives between the user and the AI-selected
     * destination.
     */
    public String getRouteAlternatives(double userLat, double userLon, double destLat, double destLon, String profile) {
        if (!isWithinRoutingRange(userLat, userLon, destLat, destLon)) {
            return "Destination shelter is outside terrestrial navigation range (>500km).";
        }

        String effectiveProfile = normalizeProfile(profile);

        try {
            MapboxResponse response = requestRouteResponse(userLat, userLon, destLat, destLon, effectiveProfile);
            if (response == null || !"Ok".equalsIgnoreCase(response.getCode())
                    || response.getRoutes() == null || response.getRoutes().isEmpty()) {
                return "No valid " + profile + " routes found to the destination.";
            }

            StringBuilder routeSummaries = new StringBuilder("Available " + profile + " routes:\n");
            int optionNum = 1;
            for (MapboxResponse.Route route : response.getRoutes()) {
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
            System.err.println("[Mapbox Error] Routing call failed: " + e.getMessage());
            return "Mapbox API temporarily unavailable.";
        }
    }

    /**
     * Gets the route geometry for the first available route without involving
     * Gemini.
     * The browser uses these longitude/latitude pairs to draw the actual road path.
     *
     * @return Mapbox GeoJSON coordinates, or an empty list when no route is available
     */
    public List<List<Double>> getRouteGeometry(
            double userLat, double userLon, double destLat, double destLon, String profile) {
        if (!isWithinRoutingRange(userLat, userLon, destLat, destLon)) {
            return List.of();
        }

        try {
            MapboxResponse response = requestRouteResponse(
                    userLat, userLon, destLat, destLon, normalizeProfile(profile));
            if (response == null || !"Ok".equalsIgnoreCase(response.getCode())
                    || response.getRoutes() == null || response.getRoutes().isEmpty()) {
                return List.of();
            }

            MapboxResponse.Geometry geometry = response.getRoutes().get(0).getGeometry();
            return geometry == null || geometry.getCoordinates() == null
                    ? List.of()
                    : geometry.getCoordinates();
        } catch (Exception e) {
            System.err.println("[Mapbox Error] Route geometry call failed: " + e.getMessage());
            return List.of();
        }
    }

    private MapboxResponse requestRouteResponse(
            double userLat, double userLon, double destLat, double destLon, String profile) {
        String coordString = String.format(Locale.US, "%.5f,%.5f;%.5f,%.5f", userLon, userLat, destLon, destLat);
        return restClient.get()
                .uri(baseUrl + "/" + profile + "/" + coordString
                        + "?overview=full&geometries=geojson&steps=true&alternatives=true"
                        + "&access_token=" + mapboxApiKey)
                .header("Accept-Encoding", "identity")
                .header("User-Agent", "TravelBuddy/1.0")
                .retrieve()
                .body(MapboxResponse.class);
    }

    private boolean isWithinRoutingRange(double userLat, double userLon, double destLat, double destLon) {
        double latDiff = Math.abs(userLat - destLat);
        double lonDiff = Math.abs(userLon - destLon);
        return latDiff <= MAX_ROUTE_COORDINATE_DELTA && lonDiff <= MAX_ROUTE_COORDINATE_DELTA;
    }

    /**
     * Keeps unsupported or blank profiles from producing an invalid Mapbox URL.
     */
    private String normalizeProfile(String profile) {
        if ("foot".equalsIgnoreCase(profile)) {
            return "mapbox/walking";
        }

        if ("vehicle".equalsIgnoreCase(profile)
                || "driving".equalsIgnoreCase(profile)) {
            return "mapbox/driving";
        }

        return DEFAULT_PROFILE;
    }
}
