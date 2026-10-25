package com.TravelBuddy.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.TravelBuddy.model.Disaster.DisasterEvents;
import com.TravelBuddy.model.ProximitySummary;
import com.TravelBuddy.model.RiskAssessmentResponse;
import com.TravelBuddy.model.User;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiRiskAssessmentService {

    private final ChatClient chatClient;
    private final GeminiAgentTools geminiAgentTools;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    // Store active AI modal requests for termination of models
    private final Map<String, Thread> activeThreads = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> activeFutures = new ConcurrentHashMap<>();

    public GeminiRiskAssessmentService(ChatClient.Builder chatClientBuilder, GeminiAgentTools geminiAgentTools) {
        this.chatClient = chatClientBuilder.build();
        this.geminiAgentTools = geminiAgentTools;
    }

    /**
     * Terminate an in-progress AI generation task on the backend when user cancels.
     */
    public void cancelAssessment(String requestId, String username) {
        terminateKey(requestId);
        terminateKey(username);
    }

    private void terminateKey(String key) {
        if (key == null || key.isBlank()) return;
        CompletableFuture<String> future = activeFutures.remove(key);
        if (future != null) {
            future.cancel(true);
        }
        Thread thread = activeThreads.remove(key);
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    public RiskAssessmentResponse evaluateRiskAndEscapeRoutes(
            Double userLat,
            Double userLon,
            User currentUser,
            ProximitySummary proximity,
            List<DisasterEvents> activeDisasters) {
        return evaluateRiskAndEscapeRoutes(userLat, userLon, currentUser, proximity, activeDisasters, null);
    }

    public RiskAssessmentResponse evaluateRiskAndEscapeRoutes(
            Double userLat,
            Double userLon,
            User currentUser,
            ProximitySummary proximity,
            List<DisasterEvents> activeDisasters,
            String requestId) {

        String travelerLabel = resolveTravelerLabel(currentUser);

        if (userLat == null || userLon == null) {
            return new RiskAssessmentResponse(
                "UNKNOWN", 
                "Telemetry unavailable. Enable GPS.", 
                "Enable GPS to calculate proximity and routes.", 
                false, 
                null, 
                null, 
                List.of("Check global feeds", "Enable GPS location access"), 
                false, 
                "Deterministic Guard", 
                travelerLabel
            );
        }

        boolean inDanger = proximity != null && proximity.inDangerZone();

        String systemPrompt = """
            You are TravelBuddy's Autonomous Emergency AI Agent.
            If the traveler is in a SAFE ZONE: Confirm safe status (riskLevel: "LOW"). Recommended action should advise normal caution. No route recommendation needed.
            If the traveler is in a DANGER ZONE:
            Step 1: Call 'getSafeShelters' to inspect available safe destinations in the database.
            Step 2: If the user is in an unsupported region where no local shelters exist, do not attempt routing. State clearly that local shelter data is unavailable.
            Step 3: If valid local shelters exist, pick the closest safe shelter and call 'getEvacuationRoute' for both walking and driving options.
            Step 4: Output ONLY a valid JSON object matching the requested schema. Never use markdown code blocks.
            
            JSON schema required:
            {
              "riskLevel": "CRITICAL", "HIGH", "MODERATE", or "LOW",
              "riskSummary": "1-2 sentences summarizing the situation and chosen shelter/route details.",
              "recommendedAction": "1 sentence primary instruction",
              "emergencyChecklist": ["Step 1", "Step 2", "Step 3"]
            }
            """;

        String userPrompt = String.format("Traveler '%s' Location: lat %f, lon %f | Telemetry: %s",
                travelerLabel, userLat, userLon, inDanger ? "IN DANGER ZONE" : "SAFE ZONE");

        String userKey = (currentUser != null && currentUser.getUsername() != null && !currentUser.getUsername().isBlank())
                ? currentUser.getUsername() : null;

        if (requestId != null && !requestId.isBlank()) {
            activeThreads.put(requestId, Thread.currentThread());
        }
        if (userKey != null) {
            activeThreads.put(userKey, Thread.currentThread());
        }

        try {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("AI generation was cancelled by user.");
            }

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    System.out.println("[Agent Engine] Calling Gemini AI model for traveler: " + travelerLabel + " (inDanger: " + inDanger + ")");
                    var promptSpec = chatClient.prompt()
                            .system(systemPrompt)
                            .user(userPrompt);

                    // When user is in a safe place, no route recommendation is needed, so do not call tools
                    if (inDanger) {
                        promptSpec = promptSpec.tools(geminiAgentTools);
                    }

                    String content = promptSpec.call().content();
                    System.out.println("[Agent Engine] Gemini AI response received successfully:\n" + content);
                    return content;
                } catch (Exception ex) {
                    System.out.println("==================================================");
                    System.out.println("[Agent Engine ERROR] Gemini AI model invocation failed!");
                    System.out.println("[Agent Engine ERROR] Exception Class: " + ex.getClass().getName());
                    System.out.println("[Agent Engine ERROR] Error Message: " + ex.getMessage());
                    if (ex.getCause() != null) {
                        System.out.println("[Agent Engine ERROR] Root Cause: " + ex.getCause().getClass().getName() + " - " + ex.getCause().getMessage());
                    }
                    ex.printStackTrace(System.out);
                    System.out.println("==================================================");
                    throw ex;
                }
            });

            if (requestId != null && !requestId.isBlank()) {
                activeFutures.put(requestId, future);
            }
            if (userKey != null) {
                activeFutures.put(userKey, future);
            }

            String jsonOutput;
            try {
                jsonOutput = future.get(60, TimeUnit.SECONDS);
            } catch (CancellationException | InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("AI generation was cancelled by user.");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InterruptedException || Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("AI generation was cancelled by user.");
                }
                if (cause instanceof Exception ex) {
                    throw ex;
                }
                throw e;
            }

            JsonNode parsed = parseGeminiAdvice(jsonOutput);

            return new RiskAssessmentResponse(
                    parsed.path("riskLevel").asText(inDanger ? "HIGH" : "LOW"),
                    parsed.path("riskSummary").asText("Assessing local disaster feeds and forecasts."),
                    parsed.path("recommendedAction").asText("Monitor alerts."),
                    !inDanger,
                    null,
                    null,
                    objectMapper.convertValue(parsed.path("emergencyChecklist"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)),
                    true,
                    "Gemini AI Agent",
                    travelerLabel
            );

        } catch (InterruptedException e) {
            return buildCancelledResponse(requestId, travelerLabel);
        } catch (Exception e) {
            boolean wasCancelled = Thread.currentThread().isInterrupted() ||
                    e instanceof InterruptedException ||
                    (e.getCause() instanceof InterruptedException);

            if (wasCancelled) {
                return buildCancelledResponse(requestId, travelerLabel);
            }

            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }

            System.out.println("==================================================");
            System.out.println("[Agent Engine ERROR] Gemini orchestration failed: " + root.getClass().getName() + " - " + root.getMessage());
            e.printStackTrace(System.out);
            System.out.println("==================================================");

            String errorDetail = root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();

            return new RiskAssessmentResponse(
                "UNKNOWN", 
                "AI model error: " + errorDetail, 
                "Please check terminal logs or retry generating the assessment.", 
                false, 
                null, 
                null, 
                List.of("Monitor official emergency feeds", "Check application console for details"), 
                false, 
                "Error Fallback", 
                travelerLabel
            );
        } finally {
            if (requestId != null) {
                activeThreads.remove(requestId);
                activeFutures.remove(requestId);
            }
            if (userKey != null) {
                activeThreads.remove(userKey);
                activeFutures.remove(userKey);
            }
        }
    }

    private RiskAssessmentResponse buildCancelledResponse(String requestId, String travelerLabel) {
        System.out.println("[Agent Engine] Gemini AI generation successfully terminated for request: " + requestId);
        return new RiskAssessmentResponse(
            "CANCELLED", 
            "Assessment generation was cancelled by user.", 
            "Cancelled.", 
            false, 
            null, 
            null, 
            List.of(), 
            false, 
            "User Cancelled", 
            travelerLabel
        );
    }

    private String resolveTravelerLabel(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) return "Traveler";
        if (user.getEmergencyContactName() != null && !user.getEmergencyContactName().isBlank()) {
            return String.format("%s (Emergency Contact: %s)", user.getUsername(), user.getEmergencyContactName());
        }
        return user.getUsername();
    }

    private JsonNode parseGeminiAdvice(String content) {
        if (content == null || content.isBlank()) return objectMapper.createObjectNode();
        try {
            String cleaned = content;
            if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
            else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            return objectMapper.readTree(cleaned.trim());
        } catch (Exception e) {
            return objectMapper.createObjectNode()
                    .put("riskLevel", "UNKNOWN")
                    .put("riskSummary", content)
                    .put("recommendedAction", "Monitor local emergency broadcasts.");
        }
    }
}
