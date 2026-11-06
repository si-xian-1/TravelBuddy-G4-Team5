package com.TravelBuddy.controller.api;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.TravelBuddy.model.Disaster.DisasterEvents;
import com.TravelBuddy.model.ProximitySummary;
import com.TravelBuddy.model.RiskAssessmentResponse;
import com.TravelBuddy.model.User;
import com.TravelBuddy.service.GdacsService;
import com.TravelBuddy.service.GeminiRiskAssessmentService;
import com.TravelBuddy.service.MapboxRoutingService;

import static org.mockito.ArgumentMatchers.anyDouble;

import static org.mockito.Mockito.mock;

class DashboardControllerTests {

    private GdacsService gdacsService;
    private GeminiRiskAssessmentService geminiRiskAssessmentService;
    private MapboxRoutingService mapboxRoutingService;
    private MockMvc mockMvc;
    private User currentUser;

    @BeforeEach
    void setUp() {
        gdacsService = mock(GdacsService.class);
        geminiRiskAssessmentService = mock(GeminiRiskAssessmentService.class);
        mapboxRoutingService = mock(MapboxRoutingService.class);

        DashboardController controller = new DashboardController(
                gdacsService,
                geminiRiskAssessmentService,
                mapboxRoutingService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        currentUser = new User();
        currentUser.setUserId(42);
        currentUser.setUsername("test-traveler");
        currentUser.setRole("TRAVELER");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                currentUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TRAVELER"))));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disasterOverviewIncludesDisastersAndProximityWhenCoordinatesAreProvided() throws Exception {
        // Arrange
        DisasterEvents disaster = new DisasterEvents();
        ProximitySummary proximity = new ProximitySummary(
                true, "WARNING - Nearby disaster", 12.0, "Typhoon (Japan)", disaster);
        when(gdacsService.getActiveDisasters()).thenReturn(List.of(disaster));
        when(gdacsService.calculateProximitySummary(1.25, 2.5)).thenReturn(proximity);

        // Act
        mockMvc.perform(get("/api/dashboard/disaster-overview")
                        .param("userLat", "1.25")
                        .param("userLon", "2.5"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disasters").isArray())
                .andExpect(jsonPath("$.disasters.length()").value(1))
                .andExpect(jsonPath("$.warning").value("WARNING - Nearby disaster"))
                .andExpect(jsonPath("$.nearestDistanceKm").value(12.0))
                .andExpect(jsonPath("$.nearestDisasterName").value("Typhoon (Japan)"))
                .andExpect(jsonPath("$.inDangerZone").value(true));

        verify(gdacsService).getActiveDisasters();
        verify(gdacsService).calculateProximitySummary(1.25, 2.5);
    }

    @Test
    void disasterOverviewReturnsDisastersWithoutProximityWhenCoordinatesAreMissing() throws Exception {
        // Arrange
        when(gdacsService.getActiveDisasters()).thenReturn(List.of());

        // Act
        mockMvc.perform(get("/api/dashboard/disaster-overview"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disasters").isArray())
                .andExpect(jsonPath("$.disasters.length()").value(0))
                .andExpect(jsonPath("$.warning").doesNotExist())
                .andExpect(jsonPath("$.inDangerZone").doesNotExist());

        verify(gdacsService).getActiveDisasters();
        verify(gdacsService, never()).calculateProximitySummary(anyDouble(), anyDouble());
    }

    @Test
    void riskAssessmentForwardsCoordinatesUserAndRequestId() throws Exception {
        // Arrange
        RiskAssessmentResponse assessment = new RiskAssessmentResponse();
        assessment.setRiskLevel("HIGH");
        assessment.setRiskSummary("A nearby disaster may affect your location.");
        when(geminiRiskAssessmentService.evaluateRiskAndEscapeRoutes(
                1.25, 2.5, currentUser, "request-1")).thenReturn(assessment);

        // Act
        mockMvc.perform(get("/api/dashboard/risk-assessment")
                        .param("userLat", "1.25")
                        .param("userLong", "2.5")
                        .param("requestId", "request-1"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.riskSummary")
                        .value("A nearby disaster may affect your location."));

        verify(geminiRiskAssessmentService).evaluateRiskAndEscapeRoutes(
                1.25, 2.5, currentUser, "request-1");
    }

    @Test
    void routeGeometryUsesVehicleProfileByDefault() throws Exception {
        // Arrange
        List<List<Double>> geometry = List.of(
                List.of(103.8, 1.3),
                List.of(103.9, 1.4));
        when(mapboxRoutingService.getRouteGeometry(1.0, 2.0, 3.0, 4.0, "vehicle"))
                .thenReturn(geometry);

        // Act
        mockMvc.perform(get("/api/dashboard/route-geometry")
                        .param("userLat", "1.0")
                        .param("userLon", "2.0")
                        .param("destLat", "3.0")
                        .param("destLon", "4.0"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0][0]").value(103.8))
                .andExpect(jsonPath("$[0][1]").value(1.3))
                .andExpect(jsonPath("$[1][0]").value(103.9))
                .andExpect(jsonPath("$[1][1]").value(1.4));

        verify(mapboxRoutingService).getRouteGeometry(1.0, 2.0, 3.0, 4.0, "vehicle");
    }

    @Test
    void routeGeometryUsesFootProfileWhenRequested() throws Exception {
        // Arrange
        List<List<Double>> geometry = List.of(List.of(103.8, 1.3));
        when(mapboxRoutingService.getRouteGeometry(1.0, 2.0, 3.0, 4.0, "foot"))
                .thenReturn(geometry);

        // Act
        mockMvc.perform(get("/api/dashboard/route-geometry")
                        .param("userLat", "1.0")
                        .param("userLon", "2.0")
                        .param("destLat", "3.0")
                        .param("destLon", "4.0")
                        .param("transportMode", "FOOT"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0][0]").value(103.8));

        verify(mapboxRoutingService).getRouteGeometry(1.0, 2.0, 3.0, 4.0, "foot");
    }

    @Test
    void routeGeometryReturnsNotFoundWhenNoRouteIsAvailable() throws Exception {
        // Arrange
        when(mapboxRoutingService.getRouteGeometry(1.0, 2.0, 3.0, 4.0, "vehicle"))
                .thenReturn(List.of());

        // Act
        mockMvc.perform(get("/api/dashboard/route-geometry")
                        .param("userLat", "1.0")
                        .param("userLon", "2.0")
                        .param("destLat", "3.0")
                        .param("destLon", "4.0"))
                // Assert
                .andExpect(status().isNotFound());

        verify(mapboxRoutingService).getRouteGeometry(1.0, 2.0, 3.0, 4.0, "vehicle");
    }

    @Test
    void cancelRiskAssessmentForwardsRequestIdAndReturnsConfirmation() throws Exception {
        // Arrange
        // No service setup is needed; cancellation is a void operation.

        // Act
        mockMvc.perform(post("/api/dashboard/risk-assessment/cancel")
                        .param("requestId", "request-1"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"))
                .andExpect(jsonPath("$.requestId").value("request-1"));

        verify(geminiRiskAssessmentService).cancelAssessment("request-1");
    }
}