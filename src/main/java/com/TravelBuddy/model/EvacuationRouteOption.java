package com.TravelBuddy.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Normal DTO class representing an evacuation route recommendation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvacuationRouteOption {
    private String title;
    private String destination;
    private String estimatedDistance;
    private String estimatedDuration;
    private Double destinationLat;
    private Double destinationLon;
    private String routeAdvice;
    private String safetyTips;
    private boolean routeVerified;
    private boolean roadBlocked;
    private String blockedHazardName;
    private List<List<Double>> coordinates;
}
