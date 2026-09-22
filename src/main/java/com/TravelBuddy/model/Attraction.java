package com.TravelBuddy.model;

import java.time.LocalDate;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Attraction {
    private int attractionId;
    private String attractionName;
    private double latitude;
    private double longitude;
    private LocalDate visitDate;

    // Foreign key to link to respective itinerary it belongs to
    private int itineraryId;
}