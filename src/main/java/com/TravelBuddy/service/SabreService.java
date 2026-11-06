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
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Service
public class SabreService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final String FLIGHT_SHOP_PATH = "/v1/offers/flightShop";
    private static final String HOTEL_AVAILABILITY_PATH = "/v5/get/hotelavail";
    private static final String DEFAULT_HOTEL_LOCATION = "TPE";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final boolean flightDemoFallbackEnabled;
    private final boolean hotelDemoFallbackEnabled;

    private String sessionToken;
    private Instant tokenExpiry = Instant.EPOCH;

    /**
     * Creates the Sabre client and loads API credentials and demo settings.
     */
    public SabreService(
            RestClient.Builder builder,
            @Value("${sabre.client-id}") String clientId,
            @Value("${sabre.client-secret}") String clientSecret,
            @Value("${sabre.base-url}") String baseUrl,
            @Value("${sabre.flight-mock}") boolean flightDemoFallbackEnabled,
            @Value("${sabre.hotel-mock}") boolean hotelDemoFallbackEnabled) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.flightDemoFallbackEnabled = flightDemoFallbackEnabled;
        this.hotelDemoFallbackEnabled = hotelDemoFallbackEnabled;
    }

    // ------------------------------------------------------------------
    // Tool 1: Flight Search
    // ------------------------------------------------------------------
    /**
     * Searches Sabre for alternative flights and returns demo options when enabled
     * and no live offers can be used.
     */
    @Tool(description = "Search available emergency alternative flights via Sabre API given origin, destination, and departure date")
    public SabreFlightSearchResponse searchAlternativeFlights(SabreFlightSearchRequest request) {
        if (request == null) {
            System.err.println("Sabre flight search rejected: request is missing.");
            return new SabreFlightSearchResponse(List.of());
        }

        String origin = normalizeIataAirportCode(request.getOrigin());
        String destination = normalizeIataAirportCode(request.getDestination());
        if (origin == null || destination == null) {
            System.err.println("Sabre flight search rejected: origin and destination must be three-letter IATA airport codes. "
                    + "Received origin='" + request.getOrigin() + "', destination='" + request.getDestination() + "'.");
            return new SabreFlightSearchResponse(List.of());
        }

        System.out.println("AI invoked Sabre Flight Tool for Origin: " + origin + " -> " + destination);

        try {
            Map<?, ?> response = requestFlightOffers(origin, destination, request.getDepartureDate());

            System.out.println("Live Sabre Flight Data: " + response);

            List<SabreFlightSearchResponse.FlightOption> live = parseFlightShopResponse(response);
            if (!live.isEmpty()) {
                return new SabreFlightSearchResponse(live);
            }
            System.err.println("Sabre flight call succeeded but returned no usable offers.");

        } catch (RestClientResponseException e) {
            System.err.println("Sabre Flight API error " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Sabre Flight API call failed: " + e.getMessage());
        }

        return flightDemoFallbackEnabled
                ? demoFlightOptions(origin, destination, request.getDepartureDate())
                : new SabreFlightSearchResponse(List.of());
    }

    /** Creates route-compatible flight options for the demonstration flow. */
    private SabreFlightSearchResponse demoFlightOptions(
            String origin, String destination, String departureDate) {
        System.err.println("Sabre returned no flights; using configured demo flight options.");
        return new SabreFlightSearchResponse(List.of(
                new SabreFlightSearchResponse.FlightOption(
                        "Demo Airways", "DM101", departureDate + "T09:00:00",
                        "USD 320", origin, destination),
                new SabreFlightSearchResponse.FlightOption(
                        "Demo Airways", "DM202", departureDate + "T15:30:00",
                        "USD 290", origin, destination)
        ));
    }

    /** Normalizes and validates a three-letter IATA airport code. */
    private String normalizeIataAirportCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z]{3}") ? normalized : null;
    }

    // ------------------------------------------------------------------
    // Tool 2: Hotel Search
    // ------------------------------------------------------------------
    /**
     * Searches Sabre for alternative hotels, removes the current hotel, and
     * optionally returns configured demo hotels when live results are unavailable.
     */
    @Tool(description = "Search available emergency accommodation or hotel options via Sabre API given a location city or code, check-in date, and check-out date. Exclude the user's current hotel using excludedHotelName unless the dates are changed.")
    public SabreHotelSearchResponse searchHotels(SabreHotelSearchRequest request) {
        String checkIn = requestDateOrDefault(request == null ? null : request.getCheckInDate(), "2026-10-20");
        String checkOut = requestDateOrDefault(request == null ? null : request.getCheckOutDate(), "2026-10-22");

        System.out.println("AI Tool Invoked: searchHotels (" + checkIn + " to " + checkOut + ")");

        String body = buildHotelAvailabilityRequestBody(checkIn, checkOut);
        System.out.println("--- Trying: " + HOTEL_AVAILABILITY_PATH + " ---");
        System.out.println(body);
        try {
            Map<?, ?> response = requestHotelAvailability(body);

            System.out.println("Live Sabre Hotel Response: " + response);

            List<SabreHotelSearchResponse.HotelOption> liveHotels = excludeCurrentHotel(
                    parseHotelAvailabilityResponse(response), request == null ? null : request.getExcludedHotelName());
            if (!liveHotels.isEmpty()) {
                return new SabreHotelSearchResponse(liveHotels);
            }
            System.err.println("Call succeeded but no hotels were parsed from the response.");

        } catch (RestClientResponseException e) {
            System.err.println("Sabre Hotel error (" + HOTEL_AVAILABILITY_PATH + ") "
                    + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Sabre Hotel call failed (" + HOTEL_AVAILABILITY_PATH + "): " + e.getMessage());
        }

        return hotelDemoFallbackEnabled
                ? demoHotelOptions(request == null ? null : request.getExcludedHotelName())
                : new SabreHotelSearchResponse(List.of());
    }

    /** Creates hotel options for the demonstration flow. */
    private SabreHotelSearchResponse demoHotelOptions(String excludedHotelName) {
        System.err.println("Sabre returned no hotels; using configured demo hotel options.");
        List<SabreHotelSearchResponse.HotelOption> hotels = List.of(
                new SabreHotelSearchResponse.HotelOption(
                        "Demo Safe Harbor Hotel", "TPE Demo District", "USD 180", "4.5/5"),
                new SabreHotelSearchResponse.HotelOption(
                        "Demo Central Plaza", "TPE City Center", "USD 220", "4.3/5")
        );
        return new SabreHotelSearchResponse(excludeCurrentHotel(hotels, excludedHotelName));
    }

    /** Uses a supplied date when present, otherwise returns the tool default. */
    private String requestDateOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    /** Sends the Flight Shop request with one adult traveler and the requested route. */
    private Map<?, ?> requestFlightOffers(String origin, String destination, String departureDate) {
        Map<String, Object> journey = new LinkedHashMap<>();
        journey.put("departureLocation", Map.of("airportCode", origin));
        journey.put("arrivalLocation", Map.of("airportCode", destination));
        journey.put("departureDate", departureDate);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("journeys", List.of(journey));
        requestBody.put("travelers", List.of(Map.of("passengerTypeCode", "ADT")));
        requestBody.put("processingOptions", Map.of("limitNumberOfOffers", 1));

        return withAuthRetry(token -> restClient.post()
                .uri(FLIGHT_SHOP_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class));
    }

    /** Sends a hotel availability request using the cached Sabre token. */
    private Map<?, ?> requestHotelAvailability(String requestBody) {
        return withAuthRetry(token -> restClient.post()
                .uri(HOTEL_AVAILABILITY_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class));
    }

    /** Removes hotels whose names match the user's current hotel. */
    private List<SabreHotelSearchResponse.HotelOption> excludeCurrentHotel(
            List<SabreHotelSearchResponse.HotelOption> hotels, String excludedHotelName) {
        if (excludedHotelName == null || excludedHotelName.isBlank()) {
            return hotels;
        }

        String excluded = excludedHotelName.trim().toLowerCase();
        return hotels.stream()
                .filter(hotel -> !hotel.name().trim().toLowerCase().equals(excluded))
                .toList();
    }

    /** Builds the nested JSON body required by Sabre Hotel Availability. */
    private String buildHotelAvailabilityRequestBody(String checkIn, String checkOut) {
        // Sabre's hotel endpoint currently requires this nested request shape.
        // The location remains TPE until the hotel tool accepts an airport code.
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

    // ------------------------------------------------------------------
    // Response parsers (defensive)
    // ------------------------------------------------------------------
    /** Converts the nested Flight Shop response into the app's flight model. */
    private List<SabreFlightSearchResponse.FlightOption> parseFlightShopResponse(Map<?, ?> response) {
        List<SabreFlightSearchResponse.FlightOption> options = new ArrayList<>();
        collectFlightOffersFromNestedData(response, options, null);
        return options.stream().limit(5).toList();
    }

    /** Walks nested response data to find flight offers at any nesting level. */
    private void collectFlightOffersFromNestedData(
            Object value,
            List<SabreFlightSearchResponse.FlightOption> options,
            String inheritedPrice) {
        if (value instanceof Map<?, ?> map) {
            String price = findOfferPrice(map);
            if (price == null) {
                price = inheritedPrice;
            }
            String departureAirport = findFirstNonBlankValue(map,
                    "departureAirportCode", "departureAirport", "originAirportCode");
            if (departureAirport == null) {
                departureAirport = findNestedValue(map, "departureLocation", "airportCode");
            }
            String arrivalAirport = findFirstNonBlankValue(map,
                    "arrivalAirportCode", "arrivalAirport", "destinationAirportCode");
            if (arrivalAirport == null) {
                arrivalAirport = findNestedValue(map, "arrivalLocation", "airportCode");
            }

            if (departureAirport != null && arrivalAirport != null) {
                String airline = findFirstNonBlankValue(map,
                        "marketingAirlineCode", "airlineCode", "carrierCode", "airline");
                String flightNumber = findFirstNonBlankValue(map, "marketingFlightNumber", "flightNumber");
                String departureDate = findFirstNonBlankValue(map, "departureDate");
                String departureTime = findFirstNonBlankValue(map, "departureTime");
                if (airline == null) {
                    airline = findNestedValue(map, "marketingCarrier", "airlineCode");
                }
                if (flightNumber == null) {
                    flightNumber = findNestedValue(map, "marketingCarrier", "flightNumber");
                }
                if (departureDate == null) {
                    departureDate = findNestedValue(map, "departureDateTime", "date");
                }
                if (departureTime == null) {
                    departureTime = findNestedValue(map, "departureDateTime", "time");
                }

                if (airline != null && flightNumber != null && price != null) {
                    options.add(new SabreFlightSearchResponse.FlightOption(
                            airline,
                            flightNumber,
                            combineDepartureDateAndTime(departureDate, departureTime),
                            price,
                            departureAirport,
                            arrivalAirport));
                }
            }

            for (Object child : map.values()) {
                collectFlightOffersFromNestedData(child, options, price);
            }
        } else if (value instanceof List<?> list) {
            for (Object child : list) {
                collectFlightOffersFromNestedData(child, options, inheritedPrice);
            }
        }
    }

    /** Returns the first non-empty value for the supplied field names. */
    private String findFirstNonBlankValue(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }

    /** Reads one value from a nested object in a Sabre response. */
    private String findNestedValue(Map<?, ?> map, String objectKey, String valueKey) {
        Object nested = map.get(objectKey);
        return nested instanceof Map<?, ?> nestedMap ? findFirstNonBlankValue(nestedMap, valueKey) : null;
    }

    /** Extracts and formats the first price found on a flight offer. */
    private String findOfferPrice(Map<?, ?> map) {
        String amount = findFirstNonBlankValue(map, "totalAmount", "amount", "totalPrice");
        if (amount == null && map.get("price") instanceof Map<?, ?> priceMap) {
            amount = findFirstNonBlankValue(priceMap, "totalAmount", "amount", "totalPrice");
            if (amount == null) {
                amount = findNestedValue(priceMap, "total", "amount");
            }
        }
        if (amount == null) {
            return null;
        }
        String currency = findFirstNonBlankValue(map, "currencyCode", "currency");
        if (currency == null && map.get("price") instanceof Map<?, ?> priceMap) {
            currency = findFirstNonBlankValue(priceMap, "currencyCode", "currency");
        }
        return currency == null ? amount : currency + " " + amount;
    }

    /** Combines separate departure date and time fields for the UI. */
    private String combineDepartureDateAndTime(String date, String time) {
        if (date == null) {
            return time;
        }
        return time == null ? date : date + "T" + time;
    }

    /** Converts Sabre hotel availability data into the app's hotel model. */
    private List<SabreHotelSearchResponse.HotelOption> parseHotelAvailabilityResponse(Map<?, ?> response) {
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

    /** Safely converts an unknown response value to a map. */
    private static Map<?, ?> asMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            return m;
        }
        return Map.of();
    }

    /** Safely converts an unknown response value to a list. */
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

    /** Runs a Sabre call and refreshes the cached token once after a 401. */
    private <T> T withAuthRetry(Function<String, T> call) {
        try {
            return call.apply(getSabreAuthToken());
        } catch (HttpClientErrorException.Unauthorized e) {
            System.err.println("Sabre returned 401; refreshing token and retrying once.");
            invalidateToken();
            return call.apply(getSabreAuthToken());
        }
    }

    /** Clears the cached token after Sabre rejects it. */
    private synchronized void invalidateToken() {
        this.sessionToken = null;
        this.tokenExpiry = Instant.EPOCH;
    }

    /** Returns a valid cached token or requests a new one from Sabre. */
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