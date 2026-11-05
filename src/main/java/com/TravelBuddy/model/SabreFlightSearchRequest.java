package com.TravelBuddy.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class SabreFlightSearchRequest {

    @JsonPropertyDescription("3-letter IATA origin airport code, e.g., TPE for Taipei")
    private String origin;

    @JsonPropertyDescription("3-letter IATA destination airport code, e.g., SIN for Singapore")
    private String destination;

    @JsonPropertyDescription("Departure date in YYYY-MM-DD format")
    private String departureDate;

    public SabreFlightSearchRequest() {};

    public SabreFlightSearchRequest(String origin, String destination, String departureDate) {
        this.origin = origin;
        this.destination = destination;
        this.departureDate = departureDate;
    }
    // Getters and Setters
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getDepartureDate() { return departureDate; }
    public void setDepartureDate(String departureDate) { this.departureDate = departureDate; }
}
