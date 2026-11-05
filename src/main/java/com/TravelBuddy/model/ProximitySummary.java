package com.TravelBuddy.model;

import com.TravelBuddy.model.Disaster.DisasterEvents;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProximitySummary {
    private boolean inDangerZone;
    private String warning;
    private Double nearestDistanceKm;
    private String nearestDisasterName;
    private DisasterEvents nearestDisaster;

    public boolean inDangerZone() {
        return inDangerZone;
    }

    public String warning() {
        return warning;
    }

    public Double nearestDistanceKm() {
        return nearestDistanceKm;
    }

    public String nearestDisasterName() {
        return nearestDisasterName;
    }

    public DisasterEvents nearestDisaster() {
        return nearestDisaster;
    }
}
