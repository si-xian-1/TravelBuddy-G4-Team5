package com.TravelBuddy.model;

import java.util.List;

public record SabreFlightSearchResponse(List<FlightOption> flights) {
    public record FlightOption(
            String airline,
            String flightNumber,
            String departureTime,
            String price,
            String departureAirport,
            String arrivalAirport) {
        public FlightOption(String airline, String flightNumber, String departureTime, String price) {
            this(airline, flightNumber, departureTime, price, null, null);
        }
    }
}
