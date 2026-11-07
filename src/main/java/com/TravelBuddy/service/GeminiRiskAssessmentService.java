package com.TravelBuddy.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.TravelBuddy.model.EvacuationRouteOption;
import com.TravelBuddy.model.RiskAssessmentResponse;
import com.TravelBuddy.model.User;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiRiskAssessmentService {
    private static final long AI_TIMEOUT_SECONDS = 60;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ChatClient chatClient;
    private final GeminiAgentTools geminiAgentTools;
    private final AiTaskCancellationManager cancellationManager;

    public GeminiRiskAssessmentService(
            ChatClient.Builder chatClientBuilder,
            GeminiAgentTools geminiAgentTools) {
        this(chatClientBuilder, geminiAgentTools, new AiTaskCancellationManager());
    }

    @Autowired
    public GeminiRiskAssessmentService(
            ChatClient.Builder chatClientBuilder,
            GeminiAgentTools geminiAgentTools,
            AiTaskCancellationManager cancellationManager) {
        this.chatClient = chatClientBuilder.build();
        this.geminiAgentTools = geminiAgentTools;
        this.cancellationManager = cancellationManager;
    }

    /**
     * Stops an AI assessment that is still running.
     * @param requestId identifier of the assessment to cancel; blank IDs are ignored
     */
    public void cancelAssessment(String requestId) {
        cancellationManager.cancel(requestId);
    }

    /**
     * Asks Gemini to assess the traveler's location current risk and, when needed, provide verified evacuation route options.
     * @param userLat traveler's latitude, or null when location is unavailable
     * @param userLon traveler's longitude, or null when location is unavailable
     * @param currentUser logged-in traveler used to create the traveler label
     * @param requestId identifier used to support cancellation
     * @return completed risk assessment, location-unavailable response, cancelled response, or error fallback response
     */
    public RiskAssessmentResponse evaluateRiskAndEscapeRoutes(Double userLat, Double userLon,
        User currentUser, String requestId) {

        String username = currentUser != null ? currentUser.getUsername() : "Traveler";
        if (userLat == null || userLon == null) {
            return buildLocationUnavailableResponse(username);
        }

        String systemPrompt = buildSystemPrompt();

        String userPrompt = String.format(
                "Traveler '%s' current location: latitude %f, longitude %f.",
                username, userLat, userLon);

        try {
            String jsonOutput = cancellationManager.executeWithCancellation(
                    requestId,
                    () -> {
                        System.out.println("[Agent Engine] Calling Gemini for " + username);
                        return chatClient.prompt()
                                .system(systemPrompt)
                                .user(userPrompt)
                                .tools(geminiAgentTools)
                                .call()
                                .content();
                    },
                    AI_TIMEOUT_SECONDS);

            return buildAssessmentResponse(parseGeminiAdvice(jsonOutput),
                username, userLat, userLon);

        }
        // Deal with errors if AI generation was stopped by user or AI generation unable to respond properly
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return buildCancelledResponse(requestId, username);
        }
        catch (Exception e) {
            System.out.println("[Agent Engine ERROR] Assessment failed: " + e.getMessage());
            return buildErrorResponse(username, e);
        }
    }

    /**
     * Builds the instructions Gemini follows when calling disaster and routing tools.
     * @return system prompt containing the tool order and required JSON response format
     */
    private String buildSystemPrompt() {
        return """
                You are TravelBuddy's autonomous emergency AI agent.
                You must use the available tools to make your decision.

                1. Call checkProximity with the traveler's coordinates first.
                2. Use the tool result, nearest hazard, and distance to determine
                   whether the traveler is safe or in danger.
                3. If checkProximity reports a safe zone, return a LOW-risk assessment.
                   Do not call getSafeShelters or getEvacuationRoute.
                4. If checkProximity reports a danger zone, call getSafeShelters
                   with the same traveler coordinates. The tool returns only
                   shelters registered in the relevant disaster country.
                5. Select a returned local shelter using its exact name and
                   coordinates. Never select a shelter from another country.
                6. Call getEvacuationRoute exactly twice for the same shelter:
                   first with transportMode "foot", then with transportMode "vehicle".
                   Keep the results separate.
                7. If getSafeShelters returns no local shelters, do not call the
                   route tool. Explain that no valid local route is available.
                8. Never invent a shelter, distance, duration, route, or coordinate.
                9. Return only one complete, valid JSON object. Do not use markdown.
                10. Ensure the JSON response is fully completed, valid, and properly closed before returning.

                The JSON object must contain:
                {
                  "riskLevel": "CRITICAL | HIGH | MODERATE | LOW | UNKNOWN",
                  "riskSummary": "A meaningful 3-5 sentence analysis covering the nearest hazard, distance, risk level, likely local impact, uncertainty, and why a route was or was not recommended.",
                  "recommendedAction": "The main safety instruction.",
                  "inSafeZone": true or false,
                  "footOption": {
                    "title": "...",
                    "destination": "...",
                    "estimatedDistance": "...",
                    "estimatedDuration": "...",
                    "destinationLat": 0.0,
                    "destinationLon": 0.0,
                    "routeAdvice": "...",
                    "safetyTips": "Concise advice as a single text string (NOT an array).",
                    "routeVerified": true,
                    "roadBlocked": false,
                    "blockedHazardName": null
                  } or null,
                  "vehicleOption": route metadata matching the footOption schema or null,
                  "emergencyChecklist": ["Practical step 1", "Practical step 2"]
                }
                EvacuationRouteOption fields: title (string), destination (string), estimatedDistance (string),
                estimatedDuration (string), destinationLat (number), destinationLon (number), routeAdvice (string),
                safetyTips (must be a single string, NEVER an array or list),
                routeVerified (boolean), roadBlocked (boolean), blockedHazardName (string or null).
                Do not include coordinates. The application attaches the verified
                road geometry after receiving your response.
                Set both route options to null for safe-zone users and whenever
                no verified local shelter route is returned.
                """;
    }

    /**
     * Converts Gemini's JSON response into the application's response model.
     * @param response parsed Gemini response
     * @param username username of the logged in traveler
     * @param userLat traveler's latitude
     * @param userLon traveler's longitude
     * @return completed risk assessment response
     */
    private RiskAssessmentResponse buildAssessmentResponse(JsonNode response,
        String username, double userLat, double userLon) {
        boolean inSafeZone = response.path("inSafeZone").asBoolean(false);
        EvacuationRouteOption footOption = formatRouteOptions(response, "footOption");
        EvacuationRouteOption vehicleOption = formatRouteOptions(response, "vehicleOption");

        return new RiskAssessmentResponse(
            response.path("riskLevel").asText("UNKNOWN"),
            response.path("riskSummary").asText("The assessment could not be completed."),
            response.path("recommendedAction").asText("Follow official emergency instructions."),
            inSafeZone,
            inSafeZone ? null : footOption,
            inSafeZone ? null : vehicleOption,
            formatEmergencyCheckList(response),
            true,
            "Gemini AI Agent",
            username);
    }

    /**
     * Retrieve route options if any
     * @param response parsed Gemini response
     * @param fieldName JSON field containing the route option
     * @return route option, or null when the field is absent or invalid
     */
    private EvacuationRouteOption formatRouteOptions(JsonNode response, String fieldName) {
        JsonNode route = response.get(fieldName);
        if (route == null || route.isNull() || !route.isObject()) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(route, EvacuationRouteOption.class);
    }

    /**
     * Reads the emergency checklist and safely converts it to strings
     * @param response parsed Gemini response
     * @return checklist items, or an empty list when none were returned
     */
    private List<String> formatEmergencyCheckList(JsonNode response) {
        JsonNode checklist = response.get("emergencyChecklist");
        if (checklist == null || !checklist.isArray()) {
            return List.of();
        }
        return OBJECT_MAPPER.convertValue(
                checklist,
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    /**
     * Parse Gemini JSON response for proper result display to user
     * @param content raw Gemini response
     * @return parsed complete JSON object
     * @throws IllegalStateException when response is null, blank, incomplete, or malformed JSON
     */
    private JsonNode parseGeminiAdvice(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("AI returned an empty response.");
        }

        String json = content.trim();
        if (json.startsWith("```json")) {
            json = json.substring(7);
        } else if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        json = json.trim();

        int firstBrace = json.indexOf('{');
        int lastBrace = json.lastIndexOf('}');
        if (firstBrace == -1 || lastBrace == -1 || lastBrace < firstBrace) {
            throw new IllegalStateException("AI response does not contain a complete JSON object.");
        }
        json = json.substring(firstBrace, lastBrace + 1);

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("AI returned malformed or uncompleted JSON: " + e.getMessage(), e);
        }

        if (!root.isObject()
                || !root.hasNonNull("riskLevel")
                || !root.hasNonNull("riskSummary")
                || !root.hasNonNull("recommendedAction")
                || !root.has("inSafeZone")) {
            throw new IllegalStateException("AI returned an incomplete JSON response missing required fields.");
        }

        return root;
    }

    /**
     * When user did not on location and AI unable to proceed with recommendation
     * @param username username of the current logged in traveler
     * @return location-required response
     */
    private RiskAssessmentResponse buildLocationUnavailableResponse(String username) {
        return new RiskAssessmentResponse(
                "UNKNOWN",
                "Current location is unavailable.",
                "Enable location access to request an assessment.",
                false,
                null,
                null,
                List.of("Enable GPS location access", "Check official emergency feeds"),
                false,
                "Location Required",
                username);
    }

    /**
     * When traveler cancels AI generation
     * @param requestId cancelled request identifier
     * @param username username of the current logged in traveler
     * @return cancelled assessment response
     */
    private RiskAssessmentResponse buildCancelledResponse(String requestId, String username) {
        return new RiskAssessmentResponse(
                "CANCELLED",
                "Assessment generation was cancelled.",
                "Cancelled.",
                false,
                null,
                null,
                List.of(),
                false,
                "User Cancelled",
                username);
    }

    /**
     * Error response message builder when Gemini fails to response promptly
     * @param username username of the current logged in traveler
     * @param exception failure that explains the fallback details
     * @return unavailable assessment response
     */
    private RiskAssessmentResponse buildErrorResponse(String username, Exception exception) {
        String detail = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();

        return new RiskAssessmentResponse(
                "UNKNOWN",
                "The AI assessment is temporarily unavailable: " + detail,
                "Please retry or follow official emergency instructions.",
                false,
                null,
                null,
                List.of("Monitor official emergency feeds"),
                false,
                "Error Fallback",
                username);
    }
}
