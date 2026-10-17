package com.TravelBuddy.service;

import com.TravelBuddy.model.SabreHotelSearchRequest;
import com.TravelBuddy.model.SabreHotelSearchResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SabreHotelService {

    @Tool(description = "Search available emergency accommodation or hotel options via Sabre API given a location, check-in, and check-out date")
    public SabreHotelSearchResponse searchHotels(SabreHotelSearchRequest request) {
        System.out.println("AI invoked Sabre Hotel Tool for Location: " + request.getLocation());

        // Prototyping mock response (replace with live Sabre API endpoint later)
        return new SabreHotelSearchResponse(List.of(
            new SabreHotelSearchResponse.HotelOption("Grand Hyatt Taipei", "No. 2, Songshou Rd, Xinyi District", "USD 180", "4.6/5"),
            new SabreHotelSearchResponse.HotelOption("Hotel Indigo Taipei North", "No. 200, Sec. 4, Jingye 3rd Rd", "USD 130", "4.4/5")
        ));
    }
}
