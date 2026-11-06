package com.TravelBuddy.controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.TravelBuddy.model.AlternativeRequest;
import com.TravelBuddy.model.User;
import com.TravelBuddy.service.AiTaskCancellationManager;
import com.TravelBuddy.service.SabreService;

import jakarta.validation.Valid;

/**
 * Handle alternative itinerary recommendation for travelers with affected itinerary due to disaster situation
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private static final long AI_TIMEOUT_SECONDS = 60;

    private final ChatClient chatClient;
    private final SabreService sabreService;
    private final AiTaskCancellationManager cancellationManager;

    public ChatController(
            ChatClient.Builder chatClientBuilder,
            SabreService sabreService,
            AiTaskCancellationManager cancellationManager) {
        this.chatClient = chatClientBuilder.build();
        this.sabreService = sabreService;
        this.cancellationManager = cancellationManager;
    }

    /**
     * Builds the replan prompt, lets Gemini call Sabre tools
     * @param request
     * @param requestId
     * @param currentUser
     * @return generated selectable HTML to the Gemini generation modal
     */
    @PostMapping("/recommend-alternative")
    public ResponseEntity<String> recommendAlternative(
            @Valid @RequestBody AlternativeRequest request,
            @RequestParam(required = false) String requestId,
            @AuthenticationPrincipal User currentUser) {

        System.out.println(">>> ChatController received request for item type: " + request.getItemType());

        String effectiveRequestId = (request.getRequestId() != null && !request.getRequestId().isBlank())
                ? request.getRequestId()
                : requestId;

        String targetLocation = (request.getLocation() != null && !request.getLocation().isBlank())
                ? request.getLocation()
                : "TPE";

        String destinationLocation = (request.getDestinationLocation() != null
                && !request.getDestinationLocation().isBlank())
                        ? request.getDestinationLocation()
                        : targetLocation;

        String systemPrompt = String.format(
                """
                        You are TravelBuddy, a friendly AI crisis-management travel assistant.

                        CRISIS CONTEXT:
                        - Affected Items     : %s
                        - Origin Location    : %s
                        - Destination        : %s
                        - Affected Dates     : %s to %s
                        - Disaster Alert     : %s

                        YOUR TASK:
                        1. Briefly explain (1-2 sentences) how this disaster impacts the travel plans.
                        2. Call 'searchHotels' or 'searchAlternativeFlights' based on the affected item type.
                        3. Present the alternatives as a selectable HTML form. For EVERY option you find, format it EXACTLY like this:
                           <div class="form-check mb-3 p-3 border rounded">
                               <input class="form-check-input" type="checkbox" name="selectedAlternative" value='{"itemType":"HOTEL|FLIGHT","newName":"[Name or Flight Number]","price":"[Price]","address":"[Hotel address if hotel]","checkIn":"[YYYY-MM-DD if hotel]","checkOut":"[YYYY-MM-DD if hotel]","airline":"[Airline if flight]","flightNumber":"[Flight number if flight]","departureAirport":"[Departure airport if flight]","arrivalAirport":"[Arrival airport if flight]"}' id="alt-[UniqueId]">
                               <label class="form-check-label font-weight-bold" for="alt-[UniqueId]">
                                   [Name / Flight Number] - [Price]
                               </label>
                               <p class="text-sm mb-0 mt-1">[Key dates/times] <br> <span class="text-danger">[Safety notes]</span></p>
                           </div>
                        4. End with a short reassurance message. Do NOT use markdown code blocks (```html) in your response, just return raw HTML.
                        5. Do NOT pass null for any location or date parameter in tool calls.
                        6. For flight searches, pass only the exact three-letter IATA airport codes
                           from the affected flight details. Never pass a country code (such as US),
                           city name, country name, or other free-text location as an airport code.
                        7. If the hotel or flight tool returns no options, state that no verified alternatives
                           are currently available and return no selectable options. Never invent or reuse an
                           option that was not returned by the tool.
                        """,
                request.getItemType(), targetLocation, destinationLocation,
                request.getStartDate(), request.getEndDate(), request.getDisasterWarning());

        String userInstruction = (request.getUserPrompt() != null && !request.getUserPrompt().isBlank())
                ? request.getUserPrompt()
                : "Find me safe alternative options for this affected " + request.getItemType();

        String affectedItems = request.getContext() != null && !request.getContext().isBlank()
                ? request.getContext()
                : "No additional item details were supplied.";

        String toolInstruction = switch (request.getItemType().toUpperCase()) {
            case "HOTEL" -> "Call searchHotels only.";
            case "FLIGHT" -> "Call searchAlternativeFlights only.";
            case "BOTH" -> "Call both searchHotels and searchAlternativeFlights, once for each affected item.";
            default -> "Do not call a tool.";
        };

        systemPrompt = systemPrompt + String.format(
                """

                        AFFECTED ITEM DETAILS (JSON):
                        %s

                        TOOL REQUIREMENT:
                        %s

                        HOTEL SAFETY AND DUPLICATE RULE:
                        - Read the current hotel name, current check-in/check-out, and itinerary start/end dates from the affected item details.
                        - Never recommend the current hotel with the same check-in and check-out dates.
                        - Prefer a different hotel. If the current hotel is the only possible result, it may be used
                          only with both check-in and check-out changed, and both dates must remain within the itinerary
                          start/end dates. Pass the current hotel name as excludedHotelName when calling searchHotels.
                        - Every hotel option must include checkIn and checkOut in its JSON value.

                        For each result, use a checkbox named "selectedAlternative" so the user can select
                        one hotel and one flight. Its value must be valid JSON with these fields:
                        {"itemType":"HOTEL|FLIGHT","newName":"...","price":"...","address":"...","checkIn":"...","checkOut":"...","airline":"...","flightNumber":"...","departureAirport":"...","arrivalAirport":"..."}
                        Include the itemType, exact displayed price, hotel address when applicable, and for flights
                        the airline, flight number, departure airport, and arrival airport.
                        Do not invent dates, airports, coordinates, or prices. The application preserves the
                        original booking details when the alternative result does not provide them.
                        """,
                affectedItems, toolInstruction);

        final String finalSystemPrompt = systemPrompt;
        final String finalUserInstruction = userInstruction;

        try {
            String aiResponse = cancellationManager.executeWithCancellation(
                    effectiveRequestId,
                    () -> this.chatClient.prompt()
                            .system(finalSystemPrompt)
                            .user(finalUserInstruction)
                            .tools(sabreService)
                            .call()
                            .content(),
                    AI_TIMEOUT_SECONDS);

            if (aiResponse == null || aiResponse.isBlank()) {
                aiResponse = "Unable to display results. Please try refreshing.";
            }

            return ResponseEntity.ok(aiResponse);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.ok("<div class=\"alert alert-warning\">AI alternative recommendation was cancelled.</div>");
        } catch (Exception e) {
            System.out.println("[ChatController ERROR] AI generation failed: " + e.getMessage());
            return ResponseEntity.ok("<div class=\"alert alert-danger\">Failed to retrieve alternative recommendations: " + e.getMessage() + "</div>");
        }
    }

    /**
     * Terminate an in progress AI generation task
     * @param requestId Unique identifier for AI generation termination
     * @param currentUser Authenticated traveler profile
     * @return Status confirming cancellation of AI generation task
     */
    @PostMapping({"/cancel"})
    public ResponseEntity<Map<String, Object>> cancelAiGeneration(
            @RequestParam(required = false) String requestId,
            @AuthenticationPrincipal User currentUser) {

        cancellationManager.cancel(requestId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "cancelled");
        response.put("requestId", requestId != null ? requestId : "");
        return ResponseEntity.ok(response);
    }
}
