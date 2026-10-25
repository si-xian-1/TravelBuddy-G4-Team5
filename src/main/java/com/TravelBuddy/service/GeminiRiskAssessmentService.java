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

    public GeminiRiskAssessmentService(
            ChatClient.Builder chatClientBuilder,
            GeminiAgentTools geminiAgentTools) {
        this.chatClient = chatClientBuilder.build();
        this.geminiAgentTools = geminiAgentTools;
    }

    /**
     * Cancels an assessment started by the supplied request.
     */
    public void cancelAssessment(String requestId) {
        if (requestId == null || requestId.isBlank()) {
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
     * Lets the AI agent inspect the location, assess the risk, and decide whether
     * evacuation tools are required.
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

        String systemPrompt = """
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
                6. Call getEvacuationRoute twice using the exact selected shelter
                   data: once with foot and once with vehicle.
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
                  "footOption": route metadata or null,
                  "vehicleOption": route metadata or null,
                  "emergencyChecklist": ["Practical step 1", "Practical step 2"]
                }
                EvacuationRouteOption fields are:
                title, destination, estimatedDistance, estimatedDuration,
                destinationLat, destinationLon, routeAdvice, safetyTips,
                routeVerified, roadBlocked, blockedHazardName.
                Do not include coordinates. The application attaches the verified
                road geometry after receiving your response.
                Set both route options to null for safe-zone users and whenever
                no verified local shelter route is returned.
                """;

        String userPrompt = String.format(
                "Traveler '%s' current location: latitude %f, longitude %f.",
                travelerLabel, userLat, userLon);

        registerRequest(requestId);
        try {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Assessment cancelled.");
            }

            CompletableFuture<String> future = requestAssessment(systemPrompt, userPrompt, travelerLabel);
            if (requestId != null && !requestId.isBlank()) {
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

    private EvacuationRouteOption readRouteOption(JsonNode response, String fieldName) {
        JsonNode route = response.get(fieldName);
        if (route == null || route.isNull() || !route.isObject()) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(route, EvacuationRouteOption.class);
    }

    private List<String> readChecklist(JsonNode response) {
        JsonNode checklist = response.get("emergencyChecklist");
        if (checklist == null || !checklist.isArray()) {
            return List.of();
        }
        return OBJECT_MAPPER.convertValue(
                checklist,
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
    }

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

    private void registerRequest(String requestId) {
        if (requestId != null && !requestId.isBlank()) {
            activeThreads.put(requestId, Thread.currentThread());
        }
    }

    private void removeRequest(String requestId) {
        if (requestId != null && !requestId.isBlank()) {
            activeThreads.remove(requestId);
            activeFutures.remove(requestId);
        }
    }

    private String getTravelerLabel(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return "Traveler";
        }
        return user.getUsername();
    }

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
