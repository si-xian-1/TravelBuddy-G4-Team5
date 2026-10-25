package com.TravelBuddy.service;

import com.TravelBuddy.repository.ShelterRepository;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;

import com.TravelBuddy.model.ChatRequest;
import com.TravelBuddy.model.ChatResult.ForecastResult;
import com.TravelBuddy.model.ChatResult.ProximityResult;
import com.TravelBuddy.model.EvacuationRouteOption;
import com.TravelBuddy.model.OsrmResponse;
import com.TravelBuddy.model.ProximitySummary;
import com.TravelBuddy.model.Shelter;

/**
 * Create set of tools that AI can use like agentic AI workflow
 */
@Component
public class GeminiAgentTools {
    private final ShelterRepository shelterRepository;
    private final GdacsService gdacsService;
    private final OsrmRoutingService osrmRoutingService;

    public GeminiAgentTools(GdacsService gdacsService, OsrmRoutingService osrmRoutingService, ShelterRepository shelterRepository) {
        this.gdacsService = gdacsService;
        this.osrmRoutingService = osrmRoutingService;
        this.shelterRepository = shelterRepository;
    }

    /**
     * Retreive user's proximity to disaster
     * @param request
     * @return
     */
    @Tool(description = "Checks if the traveler's coordinates are within the 120km danger zone of an active natural disaster.")
    public ProximityResult checkProximity(ChatRequest.LocationRequest request) {
        if (request == null) {
            return new ProximityResult(false, "None", null, "MODERATE");
        }

        ProximitySummary summary = getProximitySummary(request);
        String threat = summary.getNearestDistanceKm() != null
                && summary.getNearestDistanceKm() <= 30.0
                        ? "CRITICAL"
                        : "MODERATE";
        
        return new ProximityResult(summary.inDangerZone(), summary.getNearestDisasterName(), summary.getNearestDistanceKm(), threat);
    }

    /**
     * Predicts if the disaster will improve or worsen
     * @param request
     * @return
     */
    @Tool(description = "Retrieves the official meteorological trajectory and severity forecast for the nearest active disaster.")
    public ForecastResult getDisasterForecast(ChatRequest.LocationRequest request) {
        if (request == null) {
            return new ForecastResult("None", "Stable", "N/A", "No active hazards in proximity.");
        }

        try {
            ProximitySummary summary = getProximitySummary(request);
            
            if (!summary.inDangerZone()) {
                return new ForecastResult("None", "Stable", "N/A", "No active hazards in proximity.");
            }

            return new ForecastResult(
                    summary.getNearestDisasterName(),
                    "Active/Volatile", 
                    "Next 24-48 Hours",
                    "Official hazard perimeter remains active. Refer to local meteorological authorities for minute-by-minute trajectory changes.");
        } catch (Exception e) {
            return new ForecastResult("Unknown", "Unpredictable", "Current", "Forecast data currently unavailable.");
        }
    }

    @Tool(description = """
            Finds registered shelters in the country of the traveler's nearest
            active disaster. Use the returned shelters only; an empty list means
            that no local shelter is available and routing must not be attempted.
            """)
    public List<Shelter> getSafeShelters(ChatRequest.LocationRequest request) {
        if (request == null) {
            return List.of();
        }

        ProximitySummary summary = getProximitySummary(request);
        if (summary.nearestDisaster() == null
                || summary.nearestDisaster().getDisasterDetails() == null) {
            return List.of();
        }

        String country = summary.nearestDisaster()
                .getDisasterDetails()
                .getDisasterCountryLocation();

        if (country == null || country.isBlank()) {
            return List.of();
        }

        String expectedCountry = country.trim().toLowerCase(Locale.ROOT);
        return shelterRepository.getAllShelters().stream()
                .filter(shelter -> shelter.getCountry() != null
                        && shelter.getCountry().trim().toLowerCase(Locale.ROOT)
                                .equals(expectedCountry))
                .toList();
    }

    // Get route from OSRM api
    @Tool(description = "Gets OSRM route alternatives to a specific safe shelter destination. Mode must be 'foot' or 'vehicle'.")
    public String getEvacuationRoute(ChatRequest.RouteToShelterRequest request) {
        if (request == null) {
            return "Route unavailable: invalid request parameters.";
        }

        String profile = "foot".equalsIgnoreCase(request.getTransportMode()) ? "foot" : "driving";

        return osrmRoutingService.getRouteAlternatives(
                request.getUserLat(),
                request.getUserLon(),
                request.getDestLat(),
                request.getDestLon(),
                profile);
    }

    private ProximitySummary getProximitySummary(ChatRequest.LocationRequest request) {
        return gdacsService.calculateProximitySummary(request.getUserLat(), request.getUserLon());
    }
}
