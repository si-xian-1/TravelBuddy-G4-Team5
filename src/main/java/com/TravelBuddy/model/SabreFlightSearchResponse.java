package com.TravelBuddy.model;

import java.util.List;

public record SabreFlightSearchResponse(List<FlightOption> flights) {
    public record FlightOption(String airline, String flightNumber, String departureTime, String price) {}
}
