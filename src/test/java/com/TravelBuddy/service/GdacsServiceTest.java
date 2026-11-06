package com.TravelBuddy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import com.TravelBuddy.model.Disaster.DisasterEvents;
import com.TravelBuddy.model.ProximitySummary;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Unit tests for Disaster Monitoring in GdacsService.
 */
@ExtendWith(MockitoExtension.class)
class GdacsServiceTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private ResourceLoader resourceLoader;

    @InjectMocks
    private GdacsService gdacsService;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        gdacsService = new GdacsService(restClientBuilder, resourceLoader);
    }

    @Test
    @DisplayName("Disaster monitoring returns ALERT if user is near disaster based on proximity")
    void calculateProximitySummary_whenUserNearDisaster_returnsAlert() {
        // Arrange
        // Disaster located in Singapore (Longitude: 103.8198, Latitude: 1.3521)
        DisasterEvents disaster = createDisaster(103.8198, 1.3521, "FL", "Singapore");
        ReflectionTestUtils.setField(gdacsService, "activeDisasters", List.of(disaster));

        // User is about 9 km away (< 120 km danger zone threshold)
        Double userLat = 1.3521;
        Double userLon = 103.9000;

        // Act
        ProximitySummary result = gdacsService.calculateProximitySummary(userLat, userLon);

        // Assert
        assertNotNull(result);
        assertTrue(result.inDangerZone(), "Disaster monitoring should return alert (inDangerZone = true) when user is near disaster");
        assertNotNull(result.nearestDistanceKm());
        assertTrue(result.nearestDistanceKm() <= 120.0, "Distance should be within 120 km threshold");
        assertTrue(result.nearestDisasterName().contains("Floods"));
    }

    @Test
    @DisplayName("Disaster monitoring returns SAFE if user is not near disaster based on proximity")
    void calculateProximitySummary_whenUserNotNearDisaster_returnsSafe() {
        // Arrange
        // Disaster in Tokyo, Japan (Longitude: 139.6917, Latitude: 35.6895)
        DisasterEvents disaster = createDisaster(139.6917, 35.6895, "EQ", "Japan");
        ReflectionTestUtils.setField(gdacsService, "activeDisasters", List.of(disaster));

        // User in Singapore (> 5,000 km away, outside 120 km threshold)
        Double userLat = 1.3521;
        Double userLon = 103.8198;

        // Act
        ProximitySummary result = gdacsService.calculateProximitySummary(userLat, userLon);

        // Assert
        assertNotNull(result);
        assertFalse(result.inDangerZone(), "Disaster monitoring should return safe (inDangerZone = false) when user is far from disaster");
        assertNotNull(result.nearestDistanceKm());
        assertTrue(result.nearestDistanceKm() > 120.0, "Distance should exceed 120 km threshold");
    }

    // Situmulate disaster creation
    private DisasterEvents createDisaster(double lon, double lat, String type, String country) {
        String json = String.format("""
                {
                  "geometry": { "coordinates": [%f, %f] },
                  "properties": {
                    "eventtype": "%s",
                    "country": "%s",
                    "fromdate": "2026-11-01T00:00:00",
                    "todate": "2026-11-05T00:00:00"
                  }
                }
                """, lon, lat, type, country);
        try {
            return new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .registerModule(new JavaTimeModule())
                    .readValue(json, DisasterEvents.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct test disaster fixture", e);
        }
    }
}
