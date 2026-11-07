package com.TravelBuddy.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Captues user's prompt, itinerary and disaster context for AI to process alternative itinerary recommendation
 */

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

    // Optional request identifier for AI task cancellation
    private String requestId;

}