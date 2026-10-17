package com.TravelBuddy.model;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Hotel {
    private Integer hotelId;

    @NotNull(message = "Hotel name cannot be empty")
    @NotBlank(message = "Hotel name is required")
    private String hotelName;
    private double latitude;
    private double longitude;

    @NotNull(message = "Check in date is required")
    private LocalDateTime checkIn;

    @NotNull(message = "Check out date is required")
    private LocalDateTime checkOut;
    // Not saved in database, to show real-time disaster alerts
    private String disasterWarning;

    // Foreign key to link to respective itinerary it belongs to
    private Integer itineraryId;
}