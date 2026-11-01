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
import org.springframework.ai.chat.client.ChatClient;

import com.TravelBuddy.model.RiskAssessmentResponse;
import com.TravelBuddy.model.User;

/**
 * Unit tests for Risk assessment AI feature in GeminiRiskAssessmentService
 */
@ExtendWith(MockitoExtension.class)
class GeminiRiskAssessmentServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Mock
    private GeminiAgentTools geminiAgentTools;

    @InjectMocks
    private GeminiRiskAssessmentService riskAssessmentService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        riskAssessmentService = new GeminiRiskAssessmentService(chatClientBuilder, geminiAgentTools);
    }

    // Contain AI in deterministic environment to produce predictable response
    private void mockChatClient(String jsonOutput) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any(GeminiAgentTools.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(jsonOutput);
    }

    @Test
    @DisplayName("If user is near disaster location, AI should return a route recommendation if any and the generated risk assessment")
    void evaluateRiskAndEscapeRoutes_whenUserNearDisaster_returnsRouteRecommendationAndRiskAssessment() {
        // Arrange
        Double userLat = 1.3521;
        Double userLon = 103.8198;
        User user = new User();
        user.setUsername("JohnDoe");
        String requestId = "req-danger-001";

        String dangerAiResponse = """
                {
                  "riskLevel": "HIGH",
                  "riskSummary": "Flooding detected 10 km from traveler location.",
                  "recommendedAction": "Evacuate immediately to designated shelter.",
                  "inSafeZone": false,
                  "footOption": {
                    "title": "Evacuation by foot to City Hall",
                    "destination": "City Hall",
                    "estimatedDistance": "1.2 km",
                    "estimatedDuration": "15 mins",
                    "routeVerified": true
                  },
                  "vehicleOption": {
                    "title": "Evacuation by vehicle to North Stadium",
                    "destination": "North Stadium",
                    "estimatedDistance": "8.5 km",
                    "estimatedDuration": "20 mins",
                    "routeVerified": true
                  },
                  "emergencyChecklist": [
                    "Pack essential documents",
                    "Move to higher ground"
                  ]
                }
                """;

        mockChatClient(dangerAiResponse);

        // Act
        RiskAssessmentResponse response = riskAssessmentService.evaluateRiskAndEscapeRoutes(
                userLat, userLon, user, requestId);

        // Assert
        assertNotNull(response);
        assertFalse(response.isInSafeZone(), "User should not be in safe zone");
        assertEquals("HIGH", response.getRiskLevel(), "Risk level should match generated assessment");
        assertEquals("Flooding detected 10 km from traveler location.", response.getRiskSummary());
        assertEquals("Evacuate immediately to designated shelter.", response.getRecommendedAction());

        // Route recommendation should be returned
        assertNotNull(response.getFootOption(), "AI should return foot route recommendation");
        assertEquals("City Hall", response.getFootOption().getDestination());
        assertNotNull(response.getVehicleOption(), "AI should return vehicle route recommendation");
        assertEquals("North Stadium", response.getVehicleOption().getDestination());

        // Checklist / precautions present
        assertEquals(List.of("Pack essential documents", "Move to higher ground"), response.getEmergencyChecklist());
    }

    @Test
    @DisplayName("If user is not near a disaster location, AI will not return a route recommendation and only the precaution user should take")
    void evaluateRiskAndEscapeRoutes_whenUserNotNearDisaster_returnsNoRouteRecommendationAndOnlyPrecautions() {
        // Arrange
        Double userLat = 1.3521;
        Double userLon = 103.8198;
        User user = new User();
        user.setUsername("Alice");
        String requestId = "req-safe-002";

        String safeAiResponse = """
                {
                  "riskLevel": "LOW",
                  "riskSummary": "No active natural hazards within 120 km of traveler.",
                  "recommendedAction": "Monitor official weather updates and enjoy your travel.",
                  "inSafeZone": true,
                  "footOption": null,
                  "vehicleOption": null,
                  "emergencyChecklist": [
                    "Keep emergency numbers accessible",
                    "Check local news daily"
                  ]
                }
                """;

        mockChatClient(safeAiResponse);

        // Act
        RiskAssessmentResponse response = riskAssessmentService.evaluateRiskAndEscapeRoutes(
                userLat, userLon, user, requestId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isInSafeZone(), "User should be marked in safe zone");
        assertEquals("LOW", response.getRiskLevel());

        // AI will not return a route recommendation
        assertNull(response.getFootOption(), "AI will not return foot route recommendation when not near disaster");
        assertNull(response.getVehicleOption(), "AI will not return vehicle route recommendation when not near disaster");

        // Only precautions user should take
        assertEquals("Monitor official weather updates and enjoy your travel.", response.getRecommendedAction());
        assertEquals(List.of("Keep emergency numbers accessible", "Check local news daily"), response.getEmergencyChecklist());
    }
}
