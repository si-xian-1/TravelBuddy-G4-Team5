package com.TravelBuddy.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Itinerary {
    private int itineraryId;
    private String itineraryName;
    private LocalDate startDate;
    private LocalDate endDate;
    private int userId;

    // Mapped all the related hotels, flights and attractions to this itinerary
    // Prevent fields from showing in request body of itinerary creation
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<Hotel> hotels = new ArrayList<>();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<Flight> flights = new ArrayList<>();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<Attraction> attractions = new ArrayList<>();
}
