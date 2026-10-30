package com.TravelBuddy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ChatResult {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProximityResult {
        private boolean inDanger;
        private String nearestHazardName;
        private Double distanceKm;
        private String threatLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavigationResult {
        private String destination;
        private String distance;
        private String duration;
        private boolean routeVerified;
        private String routingAdvice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastResult {
        private String hazardName;
        private String projectedTrend; // e.g., "Worsening", "Improving", "Stable"
        private String timeline; // e.g., "Next 48 hours"
        private String officialAdvisory;
    }
}
