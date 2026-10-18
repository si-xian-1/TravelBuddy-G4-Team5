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
public class Attraction {
    private Integer attractionId;

    @NotNull(message = "Attraction name cannot be empty")
    @NotBlank(message = "Attraction name is required")
    private String attractionName;
    private Double latitude;
    private Double longitude;

    @NotNull(message = "Visit date is required")
    private LocalDateTime visitDate;
    // Not saved in database, to show real-time disaster alerts
    private String disasterWarning;

    // Foreign key to link to respective itinerary it belongs to
    private Integer itineraryId;
}