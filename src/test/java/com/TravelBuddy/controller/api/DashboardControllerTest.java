package com.TravelBuddy.controller.api;

import static org.mockito.ArgumentMatchers.anyDouble;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.TravelBuddy.model.Disaster.DisasterEvents;
import com.TravelBuddy.model.EvacuationRouteOption;
import com.TravelBuddy.model.ProximitySummary;
import com.TravelBuddy.model.RiskAssessmentResponse;
import com.TravelBuddy.model.User;
import com.TravelBuddy.service.GdacsService;
import com.TravelBuddy.service.GeminiRiskAssessmentService;
import com.TravelBuddy.service.MapboxRoutingService;

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

    private MockMvc mockMvc;
    private User currentUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
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
    @DisplayName("Disaster overview alerts traveler with proximity warning when situated in an active danger zone")
    void disasterOverview_whenCoordinatesProvidedAndNearDisaster_returnsAlert() throws Exception {
        DisasterEvents disaster = new DisasterEvents();
        ProximitySummary dangerSummary = new ProximitySummary(
                true, "WARNING - Flooding detected", 10.0, "Floods (Singapore)", disaster);

        when(gdacsService.getActiveDisasters()).thenReturn(List.of(disaster));
        when(gdacsService.calculateProximitySummary(1.3521, 103.8198)).thenReturn(dangerSummary);

        mockMvc.perform(get("/api/dashboard/disaster-overview")
                        .param("userLat", "1.3521")
                        .param("userLon", "103.8198"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disasters").isArray())
                .andExpect(jsonPath("$.disasters.length()").value(1))
                .andExpect(jsonPath("$.inDangerZone").value(true))
                .andExpect(jsonPath("$.nearestDistanceKm").value(10.0))
                .andExpect(jsonPath("$.nearestDisasterName").value("Floods (Singapore)"))
                .andExpect(jsonPath("$.warning").value("WARNING - Flooding detected"));

        verify(gdacsService).getActiveDisasters();
        verify(gdacsService).calculateProximitySummary(1.3521, 103.8198);
    }

    @Test
    @DisplayName("Disaster overview reassures traveler that their location is clear when far from active hazards")
    void disasterOverview_whenCoordinatesProvidedAndFar_returnsSafe() throws Exception {
        ProximitySummary safeSummary = new ProximitySummary(
                false, null, 500.0, "Earthquakes (Japan)", null);

        when(gdacsService.getActiveDisasters()).thenReturn(List.of());
        when(gdacsService.calculateProximitySummary(1.3521, 103.8198)).thenReturn(safeSummary);

        mockMvc.perform(get("/api/dashboard/disaster-overview")
                        .param("userLat", "1.3521")
                        .param("userLon", "103.8198"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inDangerZone").value(false))
                .andExpect(jsonPath("$.nearestDistanceKm").value(500.0))
                .andExpect(jsonPath("$.warning").doesNotExist());

        verify(gdacsService).calculateProximitySummary(1.3521, 103.8198);
    }

    @Test
    @DisplayName("Disaster overview lists ongoing global events when traveler location is not provided")
    void disasterOverview_whenCoordinatesMissing_returnsDisastersWithoutProximity() throws Exception {
        when(gdacsService.getActiveDisasters()).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/disaster-overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disasters").isArray())
                .andExpect(jsonPath("$.warning").doesNotExist())
                .andExpect(jsonPath("$.inDangerZone").doesNotExist());

        verify(gdacsService).getActiveDisasters();
        verify(gdacsService, never()).calculateProximitySummary(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Risk assessment produces urgent evacuation options and guidance when traveler is near a disaster")
    void riskAssessment_whenNearDisaster_returnsDangerAssessmentAndRoutes() throws Exception {
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
                "test-traveler");

        when(geminiRiskAssessmentService.evaluateRiskAndEscapeRoutes(1.25, 2.5, currentUser, "request-1"))
                .thenReturn(dangerAssessment);

        mockMvc.perform(get("/api/dashboard/risk-assessment")
                        .param("userLat", "1.25")
                        .param("userLong", "2.5")
                        .param("requestId", "request-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.riskSummary").value("Disaster within 10km."))
                .andExpect(jsonPath("$.recommendedAction").value("Evacuate immediately."))
                .andExpect(jsonPath("$.inSafeZone").value(false))
                .andExpect(jsonPath("$.footOption.destination").value("Shelter A"));

        verify(geminiRiskAssessmentService).evaluateRiskAndEscapeRoutes(1.25, 2.5, currentUser, "request-1");
    }

    @Test
    @DisplayName("Risk assessment provides precautionary advice without evacuation routes when traveler is in a safe location")
    void riskAssessment_whenNotNearDisaster_returnsSafeAssessmentAndOnlyPrecautions() throws Exception {
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
                "test-traveler");

        when(geminiRiskAssessmentService.evaluateRiskAndEscapeRoutes(1.25, 2.5, currentUser, "request-2"))
                .thenReturn(safeAssessment);

        mockMvc.perform(get("/api/dashboard/risk-assessment")
                        .param("userLat", "1.25")
                        .param("userLong", "2.5")
                        .param("requestId", "request-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.inSafeZone").value(true))
                .andExpect(jsonPath("$.footOption").doesNotExist())
                .andExpect(jsonPath("$.vehicleOption").doesNotExist())
                .andExpect(jsonPath("$.recommendedAction").value("Stay informed and enjoy your travel."));

        verify(geminiRiskAssessmentService).evaluateRiskAndEscapeRoutes(1.25, 2.5, currentUser, "request-2");
    }

    @Test
    @DisplayName("Evacuation route path defaults to driving directions when no transport preference is specified")
    void routeGeometry_usesVehicleProfileByDefault() throws Exception {
        List<List<Double>> geometry = List.of(List.of(103.8, 1.3), List.of(103.9, 1.4));
        when(mapboxRoutingService.getRouteGeometry(1.0, 2.0, 3.0, 4.0, "vehicle")).thenReturn(geometry);

        mockMvc.perform(get("/api/dashboard/route-geometry")
                        .param("userLat", "1.0")
                        .param("userLon", "2.0")
                        .param("destLat", "3.0")
                        .param("destLon", "4.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0][0]").value(103.8))
                .andExpect(jsonPath("$[0][1]").value(1.3))
                .andExpect(jsonPath("$[1][0]").value(103.9))
                .andExpect(jsonPath("$[1][1]").value(1.4));

        verify(mapboxRoutingService).getRouteGeometry(1.0, 2.0, 3.0, 4.0, "vehicle");
    }

    @Test
    @DisplayName("Evacuation route path produces pedestrian walkway directions when walking mode is selected")
    void routeGeometry_usesFootProfileWhenRequested() throws Exception {
        List<List<Double>> geometry = List.of(List.of(103.8, 1.3));
        when(mapboxRoutingService.getRouteGeometry(1.0, 2.0, 3.0, 4.0, "foot")).thenReturn(geometry);

        mockMvc.perform(get("/api/dashboard/route-geometry")
                        .param("userLat", "1.0")
                        .param("userLon", "2.0")
                        .param("destLat", "3.0")
                        .param("destLon", "4.0")
                        .param("transportMode", "FOOT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0][0]").value(103.8));

        verify(mapboxRoutingService).getRouteGeometry(1.0, 2.0, 3.0, 4.0, "foot");
    }

    @Test
    @DisplayName("Evacuation route calculation indicates no path available when destinations cannot be reached")
    void routeGeometry_whenNoRouteAvailable_returnsNotFound() throws Exception {
        when(mapboxRoutingService.getRouteGeometry(1.0, 2.0, 3.0, 4.0, "vehicle")).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/route-geometry")
                        .param("userLat", "1.0")
                        .param("userLon", "2.0")
                        .param("destLat", "3.0")
                        .param("destLon", "4.0"))
                .andExpect(status().isNotFound());

        verify(mapboxRoutingService).getRouteGeometry(1.0, 2.0, 3.0, 4.0, "vehicle");
    }

    @Test
    @DisplayName("Cancelling risk assessment stops ongoing evaluation and confirms task termination")
    void cancelRiskAssessment_forwardsRequestIdAndReturnsCancelledStatus() throws Exception {
        mockMvc.perform(post("/api/dashboard/risk-assessment/cancel")
                        .param("requestId", "request-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"))
                .andExpect(jsonPath("$.requestId").value("request-1"));

        verify(geminiRiskAssessmentService).cancelAssessment("request-1");
    }
}
