package com.TravelBuddy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestClient;

import com.TravelBuddy.model.Disaster;
import com.TravelBuddy.model.Disaster.DisasterEvents;
import com.TravelBuddy.model.ProximitySummary;

@ExtendWith(MockitoExtension.class)
class GdacsServiceTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private ResourceLoader resourceLoader;

    private GdacsService gdacsService;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        gdacsService = new GdacsService(restClientBuilder, resourceLoader);
    }

    @Test
    @DisplayName("Reports safe status with no danger alert when traveler geographic coordinates are omitted")
    void calculateProximitySummary_whenCoordinatesNull_returnsSafeSummary() {
        // Arrange & Act
        ProximitySummary result = gdacsService.calculateProximitySummary(null, null);

        // Assert
        assertNotNull(result);
        assertFalse(result.inDangerZone());
        assertNull(result.nearestDistanceKm());
        assertEquals("None", result.nearestDisasterName());
    }

    @Test
    @DisplayName("Confirms traveler is safe when no active global disaster events are recorded")
    void calculateProximitySummary_whenNoDisasters_returnsSafe() {
        // Arrange
        Double userLat = 1.3521;
        Double userLon = 103.8198;

        // Act
        ProximitySummary result = gdacsService.calculateProximitySummary(userLat, userLon);

        // Assert
        assertNotNull(result);
        assertFalse(result.inDangerZone());
        assertNull(result.nearestDistanceKm());
        assertEquals("None", result.nearestDisasterName());
    }

    @Test
    @DisplayName("Issues no hazard warning for travel itinerary when no disaster events exist")
    void evaluateProximityAndDate_whenNoDisasters_returnsNoWarning() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // Act
        String warning = gdacsService.evaluateProximityAndDateFromDisasters(
                1.3521, 103.8198, now, now.plusDays(5));

        // Assert
        assertNull(warning);
    }

    @Test
    @DisplayName("Safely handles disaster retrieval without crashing when external data feed is unavailable")
    void getDisasterEvents_whenApiFails_returnsSafeFallback() {
        // Arrange (restClient is mocked by @Mock and throws or returns null on unstubbed calls)

        // Act
        Disaster result = gdacsService.getDisasterEvents();

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Retrieves an empty list of active hazards when no disaster feed has been loaded")
    void getActiveDisasters_whenUninitialized_returnsEmptyList() {
        // Arrange & Act
        List<DisasterEvents> disasters = gdacsService.getActiveDisasters();

        // Assert
        assertNotNull(disasters);
        assertTrue(disasters.isEmpty());
    }
}
