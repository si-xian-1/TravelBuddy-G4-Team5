package com.TravelBuddy.controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.TravelBuddy.model.Disaster.DisasterEvents;
import com.TravelBuddy.model.RiskAssessmentResponse;
import com.TravelBuddy.model.User;
import com.TravelBuddy.model.ProximitySummary;
import com.TravelBuddy.service.GdacsService;
import com.TravelBuddy.service.GeminiRiskAssessmentService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final GdacsService gdacsService;
    private final GeminiRiskAssessmentService geminiRiskAssessmentService;

    public DashboardController(
            GdacsService gdacsService,
            GeminiRiskAssessmentService geminiRiskAssessmentService) {
        this.gdacsService = gdacsService;
        this.geminiRiskAssessmentService = geminiRiskAssessmentService;
    }

    /**
     * Retrieve active disaster events and evaluate live proximity for traveler location
     * @param userLat
     * @param userLon
     * @return Status code with disaster list and live distance alert summary
     */
    @GetMapping("/disaster-overview")
    public ResponseEntity<Map<String, Object>> getDisasterOverview(
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLon) {

        Map<String, Object> results = new HashMap<>();

        // Retrieve all active disasters gathered from GDACS api
        List<DisasterEvents> activeDisasters = gdacsService.getActiveDisasters();
        results.put("disasters", activeDisasters);

        // Disaster monitoring for traveler's current location using consolidated proximity logic
        if (userLat != null && userLon != null) {
            ProximitySummary proximity = gdacsService.calculateProximitySummary(userLat, userLon);
            results.put("warning", proximity.warning());
            results.put("nearestDistanceKm", proximity.nearestDistanceKm());
            results.put("nearestDisasterName", proximity.nearestDisasterName());
            results.put("inDangerZone", proximity.inDangerZone());
        }

        // If success, returns status 200 OK with disaster overview data
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

    /**
     * Generate an AI-powered risk assessment and dual-option evacuation plan (on-foot vs vehicle)
     * using Gemini AI based on the traveler's coordinates and active disasters.
     * Uses deterministic backend classification and reuses GDACS proximity status without reassessing.
     * @param userLat Traveler latitude
     * @param userLong Traveler longitude
     * @param requestId Unique identifier for AI generation termination
     * @param currentUser Authenticated traveler profile (for recommendation personalization)
     * @return Risk assessment and escape route recommendations
     */
    @GetMapping("/risk-assessment")
    public ResponseEntity<RiskAssessmentResponse> getAiRiskAssessment(
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLong,
            @RequestParam(required = false) String requestId,
            @AuthenticationPrincipal User currentUser) {

        List<DisasterEvents> activeDisasters = gdacsService.getActiveDisasters();

        ProximitySummary proximity = null;
        if (userLat != null && userLong != null) {
            proximity = gdacsService.calculateProximitySummary(userLat, userLong);
        }

        RiskAssessmentResponse assessment = geminiRiskAssessmentService.evaluateRiskAndEscapeRoutes(
                userLat, userLong, currentUser, proximity, activeDisasters, requestId);

        return ResponseEntity.ok(assessment);
    }

    /**
     * Terminate an in progress AI generation task
     * @param requestId Unique identifier for AI generation termination
     * @param currentUser Authenticated traveler profile
     * @return Status confirming cancellation of AI generation task
     */
    @PostMapping("/risk-assessment/cancel")
    public ResponseEntity<Map<String, Object>> cancelAiRiskAssessment(
            @RequestParam(required = false) String requestId,
            @AuthenticationPrincipal User currentUser) {

        String username = (currentUser != null && currentUser.getUsername() != null) ? currentUser.getUsername() : null;
        geminiRiskAssessmentService.cancelAssessment(requestId, username);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "cancelled");
        response.put("requestId", requestId != null ? requestId : "");
        return ResponseEntity.ok(response);
    }
}
