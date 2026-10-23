package com.TravelBuddy.service;

import java.util.List;

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

    public GeminiRiskAssessmentService(ChatClient.Builder chatClientBuilder, GeminiAgentTools geminiAgentTools) {
        this.chatClient = chatClientBuilder.build();
        this.geminiAgentTools = geminiAgentTools;
    }

    public RiskAssessmentResponse evaluateRiskAndEscapeRoutes(
            Double userLat,
            Double userLon,
            User currentUser,
            ProximitySummary proximity,
            List<DisasterEvents> activeDisasters) {

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

        String systemPrompt = """
            You are TravelBuddy's Autonomous Emergency AI Agent.
            Step 1: Call 'checkProximity' using the traveler's coordinates.
            Step 2: If in danger, call 'getSafeShelters' to inspect available safe destinations in the database.
            Step 3: If the user is in an unsupported region (like South Korea where no local shelters exist), do not attempt routing. State clearly that local shelter data is unavailable.
            Step 4: If valid local shelters exist, pick the closest safe shelter and call 'getEvacuationRoute' for both walking and driving options.
            Step 5: Output ONLY a valid JSON object matching the requested schema. Never use markdown code blocks.
            
            JSON schema required:
            {
              "riskLevel": "CRITICAL", "HIGH", "MODERATE", or "LOW",
              "riskSummary": "1-2 sentences summarizing the situation and chosen shelter/route details.",
              "recommendedAction": "1 sentence primary instruction",
              "emergencyChecklist": ["Step 1", "Step 2", "Step 3"]
            }
            """;

        String userPrompt = String.format("Traveler '%s' Location: lat %f, lon %f", travelerLabel, userLat, userLon);

        try {
            String jsonOutput = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .tools(geminiAgentTools)
                    .call()
                    .content();

            JsonNode parsed = parseGeminiAdvice(jsonOutput);
            boolean inDanger = proximity != null && proximity.inDangerZone();

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

        } catch (Exception e) {
            System.err.println("[Agent Engine] Gemini orchestration failed: " + e.getMessage());
            // Graceful failure fallback payload allowing frontend to display error state and retry message
            return new RiskAssessmentResponse(
                "UNKNOWN", 
                "AI assessment temporarily unavailable due to model or network load. Please retry later.", 
                "Please retry generating the assessment.", 
                false, 
                null, 
                null, 
                List.of("Monitor official emergency feeds", "Retry assessment generation later"), 
                false, 
                "Error Fallback", 
                travelerLabel
            );
        }
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