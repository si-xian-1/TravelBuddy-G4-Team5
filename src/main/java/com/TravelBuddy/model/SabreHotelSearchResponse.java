package com.TravelBuddy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SabreHotelSearchResponse(
    @JsonProperty("hotels") List<HotelOption> hotels
) {
    public SabreHotelSearchResponse {
        // Ensure hotels list is never null to prevent Jackson NPEs
        if (hotels == null) {
            hotels = List.of();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HotelOption(
        @JsonProperty("name") String name,
        @JsonProperty("address") String address,
        @JsonProperty("price") String price,
        @JsonProperty("rating") String rating
    ) {
        public HotelOption {
            if (name == null) name = "Unknown Hotel";
            if (address == null) address = "Address Unavailable";
            if (price == null) price = "Price on Request";
            if (rating == null) rating = "N/A";
        }
    }
}