package com.TravelBuddy.controller.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.TravelBuddy.model.EvacuationRouteOption;
import com.TravelBuddy.model.ProximitySummary;
import com.TravelBuddy.model.RiskAssessmentResponse;
import com.TravelBuddy.model.User;
import com.TravelBuddy.service.GdacsService;
import com.TravelBuddy.service.GeminiRiskAssessmentService;
import com.TravelBuddy.service.MapboxRoutingService;

/**
 * Unit tests for DashboardController endpoints
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private GdacsService gdacsService;

    @Mock
    private GeminiRiskAssessmentService geminiRiskAssessmentService;

    @Mock
    private MapboxRoutingService mapboxRoutingService;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    @DisplayName("Disaster monitoring returns ALERT if user is near disaster based on proximity")
    void getDisasterOverview_whenUserNearDisaster_returnsAlert() {
        // Arrange
        Double userLat = 1.3521;
        Double userLon = 103.8198;

        ProximitySummary dangerSummary = new ProximitySummary(
                true, "WARNING - Flooding detected", 10.0, "Floods (Singapore)", null);

        when(gdacsService.getActiveDisasters()).thenReturn(List.of());
        when(gdacsService.calculateProximitySummary(userLat, userLon)).thenReturn(dangerSummary);

        // Act
        ResponseEntity<Map<String, Object>> response = dashboardController.getDisasterOverview(userLat, userLon);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("inDangerZone"));
        assertEquals(10.0, body.get("nearestDistanceKm"));
        assertEquals("WARNING - Flooding detected", body.get("warning"));
    }

    @Test
    @DisplayName("Disaster monitoring returns SAFE if user is not near disaster based on proximity")
    void getDisasterOverview_whenUserNotNearDisaster_returnsSafe() {
        // Arrange
        Double userLat = 1.3521;
        Double userLon = 103.8198;

        ProximitySummary safeSummary = new ProximitySummary(
                false, null, 500.0, "Earthquakes (Japan)", null);

        when(gdacsService.getActiveDisasters()).thenReturn(List.of());
        when(gdacsService.calculateProximitySummary(userLat, userLon)).thenReturn(safeSummary);

        // Act
        ResponseEntity<Map<String, Object>> response = dashboardController.getDisasterOverview(userLat, userLon);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("inDangerZone"));
        assertNull(body.get("warning"));
        assertEquals(500.0, body.get("nearestDistanceKm"));
    }

    @Test
    @DisplayName("AI Risk assessment returns route recommendation and assessment when user is near disaster")
    void getAiRiskAssessment_whenUserNearDisaster_returnsRoutesAndAssessment() {
        // Arrange
        Double userLat = 1.3521;
        Double userLon = 103.8198;
        String requestId = "req-ai-danger";
        User user = new User();
        user.setUsername("JohnDoe");

        EvacuationRouteOption route = new EvacuationRouteOption();
        route.setTitle("Foot Route");
        route.setDestination("Shelter A");

        RiskAssessmentResponse dangerAssessment = new RiskAssessmentResponse(
                "HIGH",
                "Disaster within 10km.",
                "Evacuate immediately.",
                false,
                route,
                null,
                List.of("Grab emergency pack"),
                true,
                "Gemini AI",
                "JohnDoe");

        when(geminiRiskAssessmentService.evaluateRiskAndEscapeRoutes(userLat, userLon, user, requestId))
                .thenReturn(dangerAssessment);

        // Act
        ResponseEntity<RiskAssessmentResponse> response = dashboardController.getAiRiskAssessment(
                userLat, userLon, requestId, user);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        RiskAssessmentResponse result = response.getBody();
        assertNotNull(result);
        assertFalse(result.isInSafeZone());
        assertNotNull(result.getFootOption());
        assertEquals("Shelter A", result.getFootOption().getDestination());
        assertEquals("Evacuate immediately.", result.getRecommendedAction());
    }

    @Test
    @DisplayName("AI Risk assessment returns NO route recommendation and only precautions when user is safe")
    void getAiRiskAssessment_whenUserNotNearDisaster_returnsNoRoutesAndOnlyPrecautions() {
        // Arrange
        Double userLat = 1.3521;
        Double userLon = 103.8198;
        String requestId = "req-ai-safe";
        User user = new User();
        user.setUsername("Alice");

        RiskAssessmentResponse safeAssessment = new RiskAssessmentResponse(
                "LOW",
                "No hazards nearby.",
                "Stay informed and enjoy your travel.",
                true,
                null,
                null,
                List.of("Check local news"),
                true,
                "Gemini AI",
                "Alice");

        when(geminiRiskAssessmentService.evaluateRiskAndEscapeRoutes(userLat, userLon, user, requestId))
                .thenReturn(safeAssessment);

        // Act
        ResponseEntity<RiskAssessmentResponse> response = dashboardController.getAiRiskAssessment(
                userLat, userLon, requestId, user);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        RiskAssessmentResponse result = response.getBody();
        assertNotNull(result);
        assertTrue(result.isInSafeZone());
        assertNull(result.getFootOption());
        assertNull(result.getVehicleOption());
        assertEquals("Stay informed and enjoy your travel.", result.getRecommendedAction());
        assertEquals(List.of("Check local news"), result.getEmergencyChecklist());
    }
}
