package com.TravelBuddy.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlternativeRequest {

    @NotNull(message = "Itinerary ID is required")
    private Integer itineraryId;

    @NotNull(message = "Item type is required")
    @NotBlank(message = "Item type must be FLIGHT, HOTEL, or ATTRACTION")
    private String itemType; // "FLIGHT", "HOTEL", "ATTRACTION"

    private Integer itemId;

    // Item-specific location info
    private String location;           // City/Airport, e.g., "Taipei" or "TPE"
    private String destinationLocation; // Optional: e.g., "SIN" for flights

    // Dates
    private String startDate; // "YYYY-MM-DD" (departureTime / checkIn / visitDate)
    private String endDate;   // "YYYY-MM-DD" (arrivalTime / checkOut)

    // Disaster Context attached to the item
    private String disasterWarning; // e.g. "Red Alert: Typhoon active near Taipei"

    // Optional user prompt refinement
    private String userPrompt;

    // JSON containing the affected itinerary items and their current booking details
    private String context;

    public Integer getItineraryId() {
        return itineraryId;
    }

    public void setItineraryId(Integer itineraryId) {
        this.itineraryId = itineraryId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(String destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getDisasterWarning() {
        return disasterWarning;
    }

    public void setDisasterWarning(String disasterWarning) {
        this.disasterWarning = disasterWarning;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

}