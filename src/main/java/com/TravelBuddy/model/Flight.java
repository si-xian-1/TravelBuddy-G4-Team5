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
public class Flight {
    private Integer flightId;

    @NotNull(message = "Airline name cannot be empty")
    @NotBlank(message = "Airline name is required")
    private String airline;

    @NotNull(message = "Flght number cannot be empty")
    @NotBlank(message = "Flight number is required")
    private String flightNumber;

    @NotNull(message = "Departure airport name cannot be empty")
    @NotBlank(message = "Departure airport name is required")
    private String departureAirport;

    @NotNull(message = "Arrival airport name cannot be empty")
    @NotBlank(message = "Arrival airport name is required")
    private String arrivalAirport;
    private Double departureLatitude;
    private Double departureLongitude;
    private Double arrivalLatitude;
    private Double arrivalLongitude;

    @NotNull(message = "Departure date and time cannot be empty")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival date and time cannot be empty")
    private LocalDateTime arrivalTime;
    // Not saved in database, to show real-time disaster alerts
    private String disasterWarning;

    // Foreign key to link to respective itinerary it belongs to
    private Integer itineraryId;
}