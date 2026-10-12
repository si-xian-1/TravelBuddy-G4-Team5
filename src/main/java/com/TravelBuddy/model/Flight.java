package com.TravelBuddy.model;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Flight {
    private Integer flightId;
    private String airline;
    private String flightNumber;
    private String departureAirport;
    private String arrivalAirport;
    private double departureLatitude;
    private double departureLongitude;
    private double arrivalLatitude;
    private double arrivalLongitude;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    // Not saved in database, to show real-time disaster alerts
    private String disasterWarning;

    // Foreign key to link to respective itinerary it belongs to
    private Integer itineraryId;
}