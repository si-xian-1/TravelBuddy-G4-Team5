package com.TravelBuddy.model;

import java.util.List;

public record SabreHotelSearchResponse(List<HotelOption> hotels) {
    public record HotelOption(String hotelName, String address, String pricePerNight, String rating) {}
}
