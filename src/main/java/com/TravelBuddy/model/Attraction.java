package com.TravelBuddy.model;

import java.time.LocalDateTime;

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
    private LocalDateTime visitDate;
    // Not saved in database, to show real-time disaster alerts
    private String disasterWarning;

    // Foreign key to link to respective itinerary it belongs to
    private int itineraryId;
}