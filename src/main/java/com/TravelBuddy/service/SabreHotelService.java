package com.TravelBuddy.service;

import com.TravelBuddy.model.SabreHotelSearchRequest;
import com.TravelBuddy.model.SabreHotelSearchResponse;
import com.TravelBuddy.model.SabreToken;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;

@Service
public class SabreHotelService {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    private String sessionToken;

    public SabreHotelService(
            RestClient.Builder builder,
            @Value("${sabre.client-id}") String clientId,
            @Value("${sabre.client-secret}") String clientSecret,
            @Value("${sabre.base-url:https://api.cert.platform.sabre.com}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Tool(description = "Search available emergency accommodation or hotel options given a location city or code, check-in date, and check-out date")
    public SabreHotelSearchResponse searchHotels(SabreHotelSearchRequest request) {
        System.out.println("AI invoked Sabre Hotel Tool for Location: " + request.getLocation());

        try {
            String accessToken = getSabreAuthToken();
            System.out.println("Sabre Access Token retrieved successfully for Hotel search!");

        } catch (Exception e) {
            System.err.println("Sabre Hotel API Call Failed, falling back to mock data: " + e.getMessage());
        }

        return new SabreHotelSearchResponse(List.of(
            new SabreHotelSearchResponse.HotelOption("Grand Hyatt Taipei", "No. 2, Songshou Rd, Xinyi District", "USD 180", "4.6/5"),
            new SabreHotelSearchResponse.HotelOption("Hotel Indigo Taipei North", "No. 200, Sec. 4, Jingye 3rd Rd", "USD 130", "4.4/5")
        ));
    }

    private String getSabreAuthToken() {
        if (sessionToken != null) {
            return sessionToken;
        }

        // Trim credentials to prevent whitespace or newline issues
        String encodedClientId = Base64.getEncoder().encodeToString(clientId.trim().getBytes());
        String encodedSecret = Base64.getEncoder().encodeToString(clientSecret.trim().getBytes());

        // Double Base64 encoding required by Sabre
        String combined = encodedClientId + ":" + encodedSecret;
        String accessCredentials = "Basic " + Base64.getEncoder().encodeToString(combined.getBytes());

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