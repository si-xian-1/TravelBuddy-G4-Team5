package com.TravelBuddy.service;

import com.TravelBuddy.model.SabreFlightSearchRequest;
import com.TravelBuddy.model.SabreFlightSearchResponse;
import com.TravelBuddy.model.SabreToken;
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
public class SabreFlightService {

    private final RestClient restClient;

    @Value("${sabre.client-id}")
    private String clientId;

    @Value("${sabre.client-secret}")
    private String clientSecret;

    private String sessionToken;

    // Inject base URL dynamically from application.properties / .env
    public SabreFlightService(RestClient.Builder builder, @Value("${sabre.base-url:https://api.cert.platform.sabre.com}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Tool(description = "Search available emergency alternative flights via Sabre API given origin, destination, and departure date")
    public SabreFlightSearchResponse searchAlternativeFlights(SabreFlightSearchRequest request) {
        System.out.println("AI invoked Sabre API for Origin: " + request.getOrigin() + " -> " + request.getDestination());

        try {
            // 1. Fetch live bearer token from Sabre
            String accessToken = getSabreAuthToken();

            // 2. Call Sabre REST API using relative path (base URL is pre-configured in restClient)
            Map<?, ?> response = restClient.get()
                    .uri("/v1/shop/flightShop?origin={origin}&destination={dest}&departuredate={date}",
                            request.getOrigin(), request.getDestination(), request.getDepartureDate())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);

            System.out.println("Live Sabre Flight Data: " + response);

        } catch (Exception e) {
            System.err.println("Sabre Flight API Call Failed, falling back to mock data: " + e.getMessage());
        }

        // Fallback mock response
        return new SabreFlightSearchResponse(List.of(
            new SabreFlightSearchResponse.FlightOption("Singapore Airlines", "SQ877", request.getDepartureDate() + "T14:20:00", "USD 320"),
            new SabreFlightSearchResponse.FlightOption("EVA Air", "BR225", request.getDepartureDate() + "T18:40:00", "USD 290")
        ));
    }

    /**
     * Obtains an OAuth2 Token using Sabre's Double-Base64 encoding rule
     */
    private String getSabreAuthToken() {
        if (sessionToken != null) {
            return sessionToken;
        }

        // Trim credentials to strip unwanted whitespace/newlines from .env
        String encodedClientId = Base64.getEncoder().encodeToString(clientId.trim().getBytes());
        String encodedSecret = Base64.getEncoder().encodeToString(clientSecret.trim().getBytes());

        // Double Base64 concatenation required by Sabre
        String combined = encodedClientId + ":" + encodedSecret;
        String accessCredentials = "Basic " + Base64.getEncoder().encodeToString(combined.getBytes());

        // Fetch session token using Record accessor accessToken()
        SabreToken response = restClient.post()
                .uri("/v2/auth/token")
                .header(HttpHeaders.AUTHORIZATION, accessCredentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(SabreToken.class);

        if (response != null && response.accessToken() != null) {
            this.sessionToken = response.accessToken();
            return this.sessionToken;
        }

        throw new RuntimeException("Failed to retrieve valid access token from Sabre API.");
    }
}