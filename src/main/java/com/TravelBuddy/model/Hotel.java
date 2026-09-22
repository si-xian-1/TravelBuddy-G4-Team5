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
    private int hotelId;
    private String hotelName;
    private double latitude;
    private double longitude;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;

    // Foreign key to link to respective itinerary it belongs to
    private int itineraryId;
}