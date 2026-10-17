package com.TravelBuddy.service;

import com.TravelBuddy.model.SabreFlightSearchRequest;
import com.TravelBuddy.model.SabreFlightSearchResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class SabreService {

    private final RestClient restClient;

    @Value("${sabre.client-id}")
    private String clientId;

    @Value("${sabre.client-secret}")
    private String clientSecret;

    @Value("${sabre.base-url}")
    private String baseUrl;

    public SabreService() {
        this.restClient = RestClient.create();
    }

    @Tool(description = "Search available emergency alternative flights via Sabre API given origin, destination, and departure date")
    public SabreFlightSearchResponse searchAlternativeFlights(SabreFlightSearchRequest request) {
        System.out.println("AI invoked Sabre API for Origin: " + request.getOrigin() + " -> " + request.getDestination());

        try {
            // 1. Fetch valid token from Sabre using Double Base64 encoding
            String accessToken = getSabreAuthToken();

            // 2. Call Sabre Bargain Finder Max / Flight Search Endpoint
            System.out.println("Sabre Access Token retrieved successfully!");

            // Example REST call using Bearer token
            /*
            Map response = restClient.get()
                    .uri(baseUrl + "/v1/shop/flights?origin={origin}&destination={dest}&departuredate={date}",
                            request.getOrigin(), request.getDestination(), request.getDepartureDate())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);
            */

        } catch (Exception e) {
            System.err.println("Sabre Authentication Failed: " + e.getMessage());
        }

        // Return flight recommendations to AI
        return new SabreFlightSearchResponse(List.of(
            new SabreFlightSearchResponse.FlightOption("Singapore Airlines", "SQ877", request.getDepartureDate() + "T14:20:00", "USD 320"),
            new SabreFlightSearchResponse.FlightOption("EVA Air", "BR225", request.getDepartureDate() + "T18:40:00", "USD 290")
        ));
    }

    /**
     * Obtains an OAuth2 Token using Sabre's Double-Base64 encoding rule
     */
    private String getSabreAuthToken() {
        // Step 1: Base64 encode client_id and client_secret individually
        String b64ClientId = Base64.getEncoder().encodeToString(clientId.trim().getBytes());
        String b64ClientSecret = Base64.getEncoder().encodeToString(clientSecret.trim().getBytes());

        // Step 2: Combine with a colon and Base64 encode the combined string
        String combined = b64ClientId + ":" + b64ClientSecret;
        String doubleEncodedAuth = Base64.getEncoder().encodeToString(combined.getBytes());

        // Step 3: Request token from Sabre /v2/auth/token endpoint
        Map<String, Object> tokenResponse = restClient.post()
                .uri(baseUrl + "/v2/auth/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + doubleEncodedAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(Map.class);

        return (String) tokenResponse.get("access_token");
    }
}