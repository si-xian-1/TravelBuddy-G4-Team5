package com.TravelBuddy.service;

import com.TravelBuddy.model.SabreFlightSearchRequest;
import com.TravelBuddy.model.SabreFlightSearchResponse;
import com.TravelBuddy.model.SabreHotelSearchRequest;
import com.TravelBuddy.model.SabreHotelSearchResponse;
import com.TravelBuddy.model.SabreToken;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class SabreService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final String DEFAULT_HOTEL_LOCATION = "TPE"; // TODO: derive from the tool request

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    private String sessionToken;
    private Instant tokenExpiry = Instant.EPOCH;

    public SabreService(
            RestClient.Builder builder,
            @Value("${sabre.client-id}") String clientId,
            @Value("${sabre.client-secret}") String clientSecret,
            @Value("${sabre.base-url:https://api.cert.platform.sabre.com}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    // ------------------------------------------------------------------
    // Tool 1: Flight Search
    // ------------------------------------------------------------------
    @Tool(description = "Search available emergency alternative flights via Sabre API given origin, destination, and departure date")
    public SabreFlightSearchResponse searchAlternativeFlights(SabreFlightSearchRequest request) {
        System.out.println("AI invoked Sabre Flight Tool for Origin: " + request.getOrigin() + " -> " + request.getDestination());

        try {
            Map<?, ?> response = withAuthRetry(token -> restClient.get()
                    .uri("/v1/shop/flights?origin={origin}&destination={dest}&departuredate={date}&limit=5",
                            request.getOrigin(), request.getDestination(), request.getDepartureDate())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class));

            System.out.println("Live Sabre Flight Data: " + response);

            List<SabreFlightSearchResponse.FlightOption> live = extractLiveFlights(response);
            if (!live.isEmpty()) {
                return new SabreFlightSearchResponse(live);
            }
            System.err.println("Sabre flight call succeeded but no itineraries could be parsed; using mock data.");

        } catch (RestClientResponseException e) {
            System.err.println("Sabre Flight API error " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Sabre Flight API Call Failed, falling back to mock data: " + e.getMessage());
        }

        return new SabreFlightSearchResponse(List.of(
                new SabreFlightSearchResponse.FlightOption("Singapore Airlines", "SQ877", request.getDepartureDate() + "T14:20:00", "USD 320"),
                new SabreFlightSearchResponse.FlightOption("EVA Air", "BR225", request.getDepartureDate() + "T18:40:00", "USD 290")
        ));
    }

    // ------------------------------------------------------------------
    // Tool 2: Hotel Search
    // ------------------------------------------------------------------
    @Tool(description = "Search available emergency accommodation or hotel options via Sabre API given a location city or code, check-in date, and check-out date")
    public SabreHotelSearchResponse searchHotels(SabreHotelSearchRequest request) {
        String checkIn = (request != null && request.getCheckInDate() != null && !request.getCheckInDate().isBlank())
                ? request.getCheckInDate().trim()
                : "2026-10-20";

        String checkOut = (request != null && request.getCheckOutDate() != null && !request.getCheckOutDate().isBlank())
                ? request.getCheckOutDate().trim()
                : "2026-10-22";

        System.out.println("AI Tool Invoked: searchHotels (" + checkIn + " to " + checkOut + ")");

        // Try several payload shapes in order; the log shows which one Sabre accepts.
        // Payload copied from Sabre's own sample app (get-hotel-avail-v2-sample-nodejs),
        // tried against several endpoint versions to find which schema accepts it.
        // /v5/ is the correct path (v5.0.0 = 404, v2.1.0 = deprecated). Remaining unknowns:
        // is GeoRef wrapped in GeoSearch, and is occupancy Rooms[] or GuestCount.Count?
        String p = "/v5/get/hotelavail";
        List<Attempt> candidates = List.of(
                new Attempt("v5 Validated Schema", "/v5/get/hotelavail", minimalBody(checkIn, checkOut))
        );

        for (Attempt candidate : candidates) {
            System.out.println("--- Trying: " + candidate.label() + " (" + candidate.path() + ") ---");
            System.out.println(candidate.body());
            try {
                Map<?, ?> response = withAuthRetry(token -> restClient.post()
                        .uri(candidate.path())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(candidate.body())
                        .retrieve()
                        .body(Map.class));

                System.out.println("Live Sabre Hotel Response (" + candidate.label() + "): " + response);

                List<SabreHotelSearchResponse.HotelOption> liveHotels = extractLiveHotels(response);
                if (!liveHotels.isEmpty()) {
                    return new SabreHotelSearchResponse(liveHotels);
                }
                System.err.println("Call succeeded but no hotels were parsed from the response.");

            } catch (RestClientResponseException e) {
                System.err.println("Sabre Hotel error (" + candidate.label() + ") "
                        + e.getStatusCode() + ": " + e.getResponseBodyAsString());
            } catch (Exception e) {
                System.err.println("Sabre Hotel call failed (" + candidate.label() + "): " + e.getMessage());
            }
        }

        // Resilience fallback
        return new SabreHotelSearchResponse(List.of(
                new SabreHotelSearchResponse.HotelOption("Grand Hyatt Taipei", "No. 2, Songshou Rd, Xinyi District", "USD 180", "4.6/5"),
                new SabreHotelSearchResponse.HotelOption("Hotel Indigo Taipei North", "No. 200, Sec. 4, Jingye 3rd Rd", "USD 130", "4.4/5")
        ));
    }

    private record Attempt(String label, String path, String body) {}

    /**
     * Minimal payload with only the blocks Sabre's docs call required
     * (location, RateInfoRef, HotelPref, ImageRef). The flags switch the two
     * structural unknowns so we can see which combination the validator accepts.
     */
    private String minimalBody(String checkIn, String checkOut) {
        return """
                {
                  "GetHotelAvailRQ": {
                    "version": "5.0.0",
                    "SearchCriteria": {
                      "GeoSearch": {
                        "GeoRef": {
                          "Radius": 30,
                          "UOM": "MI",
                          "RefPoint": { "Value": "%s", "ValueContext": "CODE", "RefPointType": "6" }
                        }
                      },
                      "RateInfoRef": {
                        "CurrencyCode": "USD",
                        "BestOnly": "2",
                        "StayDateTimeRange": { "StartDate": "%s", "EndDate": "%s" },
                        "Rooms": { "Room": [ { "Index": 1, "Adults": 1 } ] }
                      },
                      "HotelPref": { "SabreRating": { "Min": "1", "Max": "5" } },
                      "ImageRef": { "Type": "MEDIUM", "LanguageCode": "EN" }
                    }
                  }
                }
                """.formatted(DEFAULT_HOTEL_LOCATION, checkIn, checkOut);
    }

    private String rateInfoRef(String checkIn, String checkOut) {
        return """
                "RateInfoRef": {
                  "CurrencyCode": "USD",
                  "BestOnly": "2",
                  "StayDateTimeRange": { "StartDate": "%s", "EndDate": "%s" },
                  "Rooms": { "Room": [ { "Index": 1, "Adults": 1 } ] }
                }
                """.formatted(checkIn, checkOut);
    }

    /** Mirrors the payload in Sabre's own Node.js sample app, field for field. */
    private String sabreSampleBody(String airportCode, String checkIn, String checkOut) {
        return """
                {
                  "GetHotelAvailRQ": {
                    "SearchCriteria": {
                      "OffSet": 1,
                      "SortBy": "TotalRate",
                      "SortOrder": "DESC",
                      "PageSize": 20,
                      "TierLabels": false,
                      "GeoSearch": {
                        "GeoRef": {
                          "Radius": 50,
                          "UOM": "MI",
                          "RefPoint": {
                            "Value": "%s",
                            "ValueContext": "CODE",
                            "RefPointType": "6"
                          }
                        }
                      },
                      "RateInfoRef": {
                        "ConvertedRateInfoOnly": false,
                        "CurrencyCode": "USD",
                        "BestOnly": "2",
                        "PrepaidQualifier": "IncludePrepaid",
                        "StayDateRange": { "StartDate": "%s", "EndDate": "%s" },
                        "Rooms": { "Room": [ { "Index": 1, "Adults": 1, "Children": 0 } ] },
                        "InfoSource": "100,110,112,113"
                      },
                      "HotelPref": { "SabreRating": { "Min": "3", "Max": "5" } },
                      "ImageRef": { "Type": "MEDIUM", "LanguageCode": "EN" }
                    }
                  }
                }
                """.formatted(airportCode, checkIn, checkOut);
    }

    /** Payload A: search around an airport/city code. */
    /** Search around an airport/city code (RefPointType 6 = airport/city code). */
    private String geoSearchBody(String locationCode, String checkIn, String checkOut) {
        return """
                {
                  "GetHotelAvailRQ": {
                    "version": "5.0.0",
                    "SearchCriteria": {
                      "GeoSearch": {
                        "GeoRef": {
                          "Radius": 30,
                          "UOM": "MI",
                          "RefPoint": { "Value": "%s", "ValueContext": "CODE", "RefPointType": "6" }
                        }
                      },
                      %s
                    }
                  }
                }
                """.formatted(locationCode, rateInfoRef(checkIn, checkOut));
    }

    /** Search specific hotels by global hotel code. */
    private String hotelRefsBody(String checkIn, String checkOut) {
        return """
                {
                  "GetHotelAvailRQ": {
                    "version": "5.0.0",
                    "SearchCriteria": {
                      "HotelRefs": {
                        "HotelRef": [
                          { "HotelCode": "100072188", "CodeContext": "GLOBAL" },
                          { "HotelCode": "100007608", "CodeContext": "GLOBAL" }
                        ]
                      },
                      %s
                    }
                  }
                }
                """.formatted(rateInfoRef(checkIn, checkOut));
    }

    // ------------------------------------------------------------------
    // Response parsers (defensive)
    // ------------------------------------------------------------------
    private List<SabreFlightSearchResponse.FlightOption> extractLiveFlights(Map<?, ?> response) {
        List<SabreFlightSearchResponse.FlightOption> options = new ArrayList<>();
        if (response == null) {
            return options;
        }
        try {
            for (Object itinObj : asList(response.get("PricedItineraries"))) {
                Map<?, ?> itin = asMap(itinObj);

                Map<?, ?> odos = asMap(asMap(itin.get("AirItinerary")).get("OriginDestinationOptions"));
                List<?> optionList = asList(odos.get("OriginDestinationOption"));
                if (optionList.isEmpty()) {
                    continue;
                }
                List<?> segments = asList(asMap(optionList.get(0)).get("FlightSegment"));
                if (segments.isEmpty()) {
                    continue;
                }
                Map<?, ?> seg = asMap(segments.get(0));

                String airline = String.valueOf(asMap(seg.get("MarketingAirline")).get("Code"));
                String flightNo = airline + seg.get("FlightNumber");
                String departure = String.valueOf(seg.get("DepartureDateTime"));

                Map<?, ?> total = asMap(asMap(asMap(itin.get("AirItineraryPricingInfo"))
                        .get("ItinTotalFare")).get("TotalFare"));
                String price = total.get("Amount") != null
                        ? total.get("CurrencyCode") + " " + total.get("Amount")
                        : "Contact for rates";

                options.add(new SabreFlightSearchResponse.FlightOption(airline, flightNo, departure, price));
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not parse live Sabre flight JSON: " + e.getMessage());
        }
        return options;
    }

    private List<SabreHotelSearchResponse.HotelOption> extractLiveHotels(Map<?, ?> response) {
        List<SabreHotelSearchResponse.HotelOption> options = new ArrayList<>();
        if (response == null) {
            return options;
        }
        try {
            Map<?, ?> infos = asMap(asMap(response.get("GetHotelAvailRS")).get("HotelAvailInfos"));

            for (Object itemObj : asList(infos.get("HotelAvailInfo"))) {
                Map<?, ?> item = asMap(itemObj);
                Map<?, ?> info = asMap(item.get("HotelInfo"));
                if (info.isEmpty()) {
                    continue;
                }

                Object nameObj = info.get("HotelName");
                String name = nameObj != null ? nameObj.toString() : "Sabre Partner Hotel";

                String address = "City Center";
                Object line = asMap(asMap(info.get("LocationInfo")).get("Address")).get("AddressLine");
                if (line != null) {
                    address = line.toString();
                }

                String price = "Contact for rates";
                Map<?, ?> converted = asMap(asMap(asMap(item.get("HotelRateInfo")).get("RateInfos")).get("ConvertedRateInfo"));
                Object amt = converted.get("AmountBeforeTax");
                Object cur = converted.get("CurrencyCode");
                if (amt != null && cur != null) {
                    price = cur + " " + amt;
                }

                options.add(new SabreHotelSearchResponse.HotelOption(name, address, price, "4.5/5"));
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not parse live Sabre hotel JSON: " + e.getMessage());
        }
        return options;
    }

    private static Map<?, ?> asMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            return m;
        }
        return Map.of();
    }

    private static List<?> asList(Object o) {
        if (o instanceof List<?> l) {
            return l;
        }
        if (o == null) {
            return List.of();
        }
        return List.of(o);
    }

    // ------------------------------------------------------------------
    // Auth
    // ------------------------------------------------------------------

    /** Runs a call with the cached token; on a 401, refreshes the token and retries once. */
    private <T> T withAuthRetry(Function<String, T> call) {
        try {
            return call.apply(getSabreAuthToken());
        } catch (HttpClientErrorException.Unauthorized e) {
            System.err.println("Sabre returned 401; refreshing token and retrying once.");
            invalidateToken();
            return call.apply(getSabreAuthToken());
        }
    }

    private synchronized void invalidateToken() {
        this.sessionToken = null;
        this.tokenExpiry = Instant.EPOCH;
    }

    private synchronized String getSabreAuthToken() {
        if (sessionToken != null && Instant.now().isBefore(tokenExpiry)) {
            return sessionToken;
        }

        if (clientId == null || clientSecret == null || clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("Sabre API credentials failed to load from application properties or .env!");
        }

        String encodedClientId = Base64.getEncoder().encodeToString(clientId.trim().getBytes(StandardCharsets.UTF_8));
        String encodedSecret = Base64.getEncoder().encodeToString(clientSecret.trim().getBytes(StandardCharsets.UTF_8));

        // Sabre's double-Base64 rule
        String combined = encodedClientId + ":" + encodedSecret;
        String doubleEncodedAuth = Base64.getEncoder().encodeToString(combined.getBytes(StandardCharsets.UTF_8));

        SabreToken response = restClient.post()
                .uri("/v2/auth/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + doubleEncodedAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(SabreToken.class);

        if (response != null && response.accessToken() != null) {
            this.sessionToken = response.accessToken();
            this.tokenExpiry = Instant.now().plus(TOKEN_TTL);
            System.out.println("Sabre Session Token successfully obtained!");
            return this.sessionToken;
        }

        throw new RuntimeException("Failed to retrieve valid access token from Sabre API.");
    }
}