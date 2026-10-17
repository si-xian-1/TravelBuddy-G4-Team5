package com.TravelBuddy.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class SabreHotelSearchRequest {

    @JsonPropertyDescription("City name or location code, e.g., Taipei or TPE")
    private String location;

    @JsonPropertyDescription("Check-in date in YYYY-MM-DD format")
    private String checkInDate;

    @JsonPropertyDescription("Check-out date in YYYY-MM-DD format")
    private String checkOutDate;

    // Getters and Setters
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public String getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }
}
