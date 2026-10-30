package com.TravelBuddy.model.enums;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransportMode {
    WALKING("foot", 1.0),
    DRIVING("driving", 8.0);

    private final String mapboxProfile;
    private final double detourOffsetKm;
}
