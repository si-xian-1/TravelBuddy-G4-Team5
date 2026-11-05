package com.TravelBuddy.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.ai.chat.client.ChatClient;
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
    private final Map<String, Thread> activeThreads = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> activeFutures = new ConcurrentHashMap<>();

    /**
     * Creates the service and prepares the Gemini client and agent tools.
     *
     * @param chatClientBuilder builder used to create the Gemini chat client
     * @param geminiAgentTools tools Gemini may call during an assessment
     */
    public GeminiRiskAssessmentService(
            ChatClient.Builder chatClientBuilder,
            GeminiAgentTools geminiAgentTools) {
        this.chatClient = chatClientBuilder.build();
        this.geminiAgentTools = geminiAgentTools;
    }

    /**
     * Stops an AI assessment that is still running.
     *
     * @param requestId identifier of the assessment to cancel; blank IDs are ignored
     * @return the method updates the running task state when a request exists
     */
    public void cancelAssessment(String requestId) {
        if (!hasRequestId(requestId)) {
            return;
        }

        CompletableFuture<String> future = activeFutures.remove(requestId);
        if (future != null) {
            future.cancel(true);
        }

        Thread thread = activeThreads.remove(requestId);
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    /**
     * Asks Gemini to assess the traveler's current risk and, when needed, create
     * verified evacuation route options.
     *
     * @param userLat traveler's latitude, or null when location is unavailable
     * @param userLon traveler's longitude, or null when location is unavailable
     * @param currentUser logged-in traveler used to create the traveler label
     * @param requestId identifier used to support cancellation
     * @return completed risk assessment, location-unavailable response, cancelled
     *         response, or error fallback response
     */
    public RiskAssessmentResponse evaluateRiskAndEscapeRoutes(
            Double userLat,
            Double userLon,
            User currentUser,
            String requestId) {

        String travelerLabel = getTravelerLabel(currentUser);
        if (userLat == null || userLon == null) {
            return buildLocationUnavailableResponse(travelerLabel);
        }

        String systemPrompt = buildSystemPrompt();

        String userPrompt = String.format(
                "Traveler '%s' current location: latitude %f, longitude %f.",
                travelerLabel, userLat, userLon);

        registerRequest(requestId);
        try {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Assessment cancelled.");
            }

            CompletableFuture<String> future = requestAssessment(systemPrompt, userPrompt, travelerLabel);
            if (hasRequestId(requestId)) {
                activeFutures.put(requestId, future);
            }

            String jsonOutput = getAssessmentResult(future);
            return buildAssessmentResponse(
                    parseGeminiAdvice(jsonOutput),
                    travelerLabel,
                    userLat,
                    userLon);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return buildCancelledResponse(requestId, travelerLabel);
        } catch (Exception e) {
            System.out.println("[Agent Engine ERROR] Assessment failed: " + e.getMessage());
            return buildErrorResponse(travelerLabel, e);
        } finally {
            removeRequest(requestId);
        }
    }

    /**
     * Builds the instructions Gemini follows when calling disaster and routing tools.
     *
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
                9. Return only one valid JSON object. Do not use markdown.

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
     * Starts the asynchronous Gemini request.
     *
     * @param systemPrompt instructions that define the agent's behavior
     * @param userPrompt traveler's location message
     * @param travelerLabel name included in diagnostic logging
     * @return future containing Gemini's raw response text
     */
    private CompletableFuture<String> requestAssessment(
            String systemPrompt,
            String userPrompt,
            String travelerLabel) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("[Agent Engine] Calling Gemini for " + travelerLabel);
                return chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .tools(geminiAgentTools)
                        .call()
                        .content();
            } catch (Exception e) {
                throw new IllegalStateException("Gemini invocation failed.", e);
            }
        });
    }

    /**
     * Waits for Gemini to finish, cancelling the future on interruption or timeout.
     *
     * @param future asynchronous Gemini request
     * @return raw JSON or text returned by Gemini
     * @throws InterruptedException when the assessment is cancelled
     * @throws ExecutionException when Gemini fails
     * @throws TimeoutException when the request exceeds the configured timeout
     */
    private String getAssessmentResult(CompletableFuture<String> future)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return future.get(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (CancellationException e) {
            throw new InterruptedException("Assessment cancelled.");
        } catch (InterruptedException e) {
            future.cancel(true);
            throw e;
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    /**
     * Converts Gemini's JSON response into the application's response model.
     *
     * @param response parsed Gemini response
     * @param travelerLabel display label for the traveler
     * @param userLat traveler's latitude
     * @param userLon traveler's longitude
     * @return application-ready risk assessment response
     */
    private RiskAssessmentResponse buildAssessmentResponse(
            JsonNode response,
            String travelerLabel,
            double userLat,
            double userLon) {
        boolean inSafeZone = response.path("inSafeZone").asBoolean(false);
        EvacuationRouteOption footOption = readRouteOption(response, "footOption");
        EvacuationRouteOption vehicleOption = readRouteOption(response, "vehicleOption");

        return new RiskAssessmentResponse(
                response.path("riskLevel").asText("UNKNOWN"),
                response.path("riskSummary").asText("The assessment could not be completed."),
                response.path("recommendedAction").asText("Follow official emergency instructions."),
                inSafeZone,
                inSafeZone ? null : footOption,
                inSafeZone ? null : vehicleOption,
                readChecklist(response),
                true,
                "Gemini AI Agent",
                travelerLabel);
    }

    /**
     * Reads one route option from the JSON response.
     *
     * @param response parsed Gemini response
     * @param fieldName JSON field containing the route option
     * @return route option, or null when the field is absent or invalid
     */
    private EvacuationRouteOption readRouteOption(JsonNode response, String fieldName) {
        JsonNode route = response.get(fieldName);
        if (route == null || route.isNull() || !route.isObject()) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(route, EvacuationRouteOption.class);
    }

    /**
     * Reads the emergency checklist and safely converts it to strings.
     *
     * @param response parsed Gemini response
     * @return checklist items, or an empty list when none were returned
     */
    private List<String> readChecklist(JsonNode response) {
        JsonNode checklist = response.get("emergencyChecklist");
        if (checklist == null || !checklist.isArray()) {
            return List.of();
        }
        return OBJECT_MAPPER.convertValue(
                checklist,
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    /**
     * Removes optional Markdown fences and parses Gemini's response as JSON.
     *
     * @param content raw Gemini response
     * @return parsed JSON, or an UNKNOWN-risk fallback object when parsing fails
     */
    private JsonNode parseGeminiAdvice(String content) {
        if (content == null || content.isBlank()) {
            return OBJECT_MAPPER.createObjectNode();
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

        try {
            return OBJECT_MAPPER.readTree(json.trim());
        } catch (Exception e) {
            return OBJECT_MAPPER.createObjectNode()
                    .put("riskLevel", "UNKNOWN")
                    .put("riskSummary", content)
                    .put("recommendedAction", "Monitor official emergency broadcasts.")
                    .put("inSafeZone", false);
        }
    }

    /**
     * Records the current thread so a matching request can be interrupted.
     *
     * @param requestId request identifier
     * @return nothing
     */
    private void registerRequest(String requestId) {
        if (hasRequestId(requestId)) {
            activeThreads.put(requestId, Thread.currentThread());
        }
    }

    /**
     * Removes a request from the cancellation tracking maps.
     *
     * @param requestId request identifier
     * @return
     */
    private void removeRequest(String requestId) {
        if (hasRequestId(requestId)) {
            activeThreads.remove(requestId);
            activeFutures.remove(requestId);
        }
    }

    /**
     * Checks whether a request identifier can be used as a map key.
     *
     * @param requestId request identifier to check
     * @return true when the ID is non-null and not blank
     */
    private boolean hasRequestId(String requestId) {
        return requestId != null && !requestId.isBlank();
    }

    /**
     * Gets a safe display name for the traveler.
     *
     * @param user logged-in user, possibly null
     * @return username when available, otherwise "Traveler"
     */
    private String getTravelerLabel(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return "Traveler";
        }
        return user.getUsername();
    }

    /**
     * Creates the response shown when browser location was not supplied.
     *
     * @param travelerLabel display label for the traveler
     * @return location-required response with no route options
     */
    private RiskAssessmentResponse buildLocationUnavailableResponse(String travelerLabel) {
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
                travelerLabel);
    }

    /**
     * Creates the response returned after the traveler cancels generation.
     *
     * @param requestId cancelled request identifier
     * @param travelerLabel display label for the traveler
     * @return cancelled assessment response
     */
    private RiskAssessmentResponse buildCancelledResponse(String requestId, String travelerLabel) {
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
                travelerLabel);
    }

    /**
     * Creates a user-facing fallback when Gemini assessment fails.
     *
     * @param travelerLabel display label for the traveler
     * @param exception failure that explains the fallback details
     * @return unavailable assessment response
     */
    private RiskAssessmentResponse buildErrorResponse(String travelerLabel, Exception exception) {
        String detail = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();

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
                travelerLabel);
    }
}
