package com.TravelBuddy.model;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Hotel {
    private Integer hotelId;
    private String hotelName;
    private double latitude;
    private double longitude;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    // Not saved in database, to show real-time disaster alerts
    private String disasterWarning;

    // Foreign key to link to respective itinerary it belongs to
    private Integer itineraryId;
}