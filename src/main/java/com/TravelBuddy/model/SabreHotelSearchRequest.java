package com.TravelBuddy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class SabreHotelSearchRequest {

    @JsonProperty("location")
    @JsonPropertyDescription("The city name, airport code, or location for hotel search (e.g., Taipei, TPE, Singapore)")
    private String location;

    @JsonProperty("checkInDate")
    @JsonPropertyDescription("Check-in date in YYYY-MM-DD format")
    private String checkInDate;

    @JsonProperty("checkOutDate")
    @JsonPropertyDescription("Check-out date in YYYY-MM-DD format")
    private String checkOutDate;

    @JsonProperty("excludedHotelName")
    @JsonPropertyDescription("The user's current hotel name; never return this hotel unless the requested dates change")
    private String excludedHotelName;

    public SabreHotelSearchRequest() {}

    public SabreHotelSearchRequest(String location, String checkInDate, String checkOutDate) {
        this.location = location;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public String getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }

    public String getExcludedHotelName() { return excludedHotelName; }
    public void setExcludedHotelName(String excludedHotelName) { this.excludedHotelName = excludedHotelName; }
}