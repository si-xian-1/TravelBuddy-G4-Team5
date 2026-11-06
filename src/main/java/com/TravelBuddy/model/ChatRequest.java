package com.TravelBuddy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ChatRequest {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationRequest {
        private double userLat;
        private double userLon;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteRequest {
        private double userLat;
        private double userLon;
        private String transportMode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteToShelterRequest {
        private double userLat;
        private double userLon;
        private double destLat;
        private double destLon;
        private String shelterName;
        private String transportMode;
    }
}
