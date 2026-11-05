package com.TravelBuddy.model;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.TravelBuddy.model.enums.EventType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * For processing JSON data returned from GDACS api
 * Not saved in database as data is updated whenever new disasters happens
 */
public class Disaster {
    /**
     * API json response returns a list of all disasters using features array
     */
    @JsonProperty("features")
    private ArrayList<DisasterEvents> disasterEvents;

    public ArrayList<DisasterEvents> getAllDisastersEvents() {
        return disasterEvents;
    }

    /**
     * In features array, each disaster events has its own properties
     */
    public static class DisasterEvents {
        @JsonProperty("geometry")
        private LocationCoordinates locationCoordinates;

        @JsonProperty("properties")
        private DisasterDetails disasterDetails;

        @JsonProperty("distanceKm")
        private Double distanceKm;

        @JsonProperty("inDangerZone")
        private Boolean inDangerZone;

        public LocationCoordinates getLocationCoordinates() {
            return locationCoordinates;
        }

        public DisasterDetails getDisasterDetails() {
            return disasterDetails;
        }

        public Double getDistanceKm() {
            return distanceKm;
        }

        public void setDistanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
        }

        public Boolean getInDangerZone() {
            return inDangerZone;
        }

        public void setInDangerZone(Boolean inDangerZone) {
            this.inDangerZone = inDangerZone;
        }
    }

    /**
     * Access geometry dictionary to get the longtitude and latitude coordinates
     */
    public static class LocationCoordinates {
        @JsonProperty("coordinates")
        private double[] coordinates;

        public double[] getCoordinates() {
            return coordinates;
        }
    }

    /**
     * Access properties dictionary to get disaster details
     */
    public static class DisasterDetails {
        @JsonProperty("eventtype")
        private EventType disasterType;

        @JsonProperty("eventid")
        private long disasterId;

        @JsonProperty("alertlevel")
        private String alertLevel;

        @JsonProperty("country")
        private String disasterCountryLocation;

        @JsonProperty("fromdate")
        private LocalDateTime disasterStartDate;

        @JsonProperty("todate")
        private LocalDateTime disasterEndDate;

        @JsonProperty("affectedcountries")
        private ArrayList<AffectedCountries> affectedCountries;

        public EventType getDisasterType() {
            return disasterType;
        }

        public long getDisasterId() {
            return disasterId;
        }

        public String getAlertLevel() {
            return alertLevel;
        }

        public String getDisasterCountryLocation() {
            return disasterCountryLocation;
        }

        public LocalDateTime getDisasterStartDate() {
            return disasterStartDate;
        }

        public LocalDateTime getDisasterEndDate() {
            return disasterEndDate;
        }

        public ArrayList<AffectedCountries> getAffectedCountries() {
            if (affectedCountries == null) {
                return new ArrayList<>();
            }
            
            return affectedCountries;
        }
    }

    /**
     * Get list of all the affected countries
     */
    public static class AffectedCountries {
        @JsonProperty("iso3")
        private String countryCode;

        @JsonProperty("countryname")
        private String countryName;

        public String getCountryCode() {
            return countryCode;
        }

        public String getCountryName() {
            return countryName;
        }

        public String toString() {
            return getCountryName();
        }
    }
}
