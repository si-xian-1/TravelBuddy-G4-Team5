package com.TravelBuddy.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Itinerary {
    private Integer itineraryId;

    @NotNull(message = "Itinerary cannot be empty")
    @NotBlank(message = "Itinerary name is required")
    private String itineraryName;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
    private Integer userId;

    // Mapped all the related hotels, flights and attractions to this itinerary
    // Prevent fields from showing in request body of itinerary creation
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<Hotel> hotels = new ArrayList<>();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<Flight> flights = new ArrayList<>();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<Attraction> attractions = new ArrayList<>();
}
