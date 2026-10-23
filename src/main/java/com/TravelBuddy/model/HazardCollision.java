package com.TravelBuddy.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HazardCollision {
    private boolean roadBlocked;
    private String hazardName;
    private Double waypointLat;
    private Double waypointLon;
}
