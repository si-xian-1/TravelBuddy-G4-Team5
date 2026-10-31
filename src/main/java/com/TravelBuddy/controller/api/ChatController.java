package com.TravelBuddy.controller.api;

import com.TravelBuddy.model.AlternativeRequest;
import com.TravelBuddy.model.User;
import com.TravelBuddy.service.SabreService;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;
    private final SabreService sabreService;

    public ChatController(OllamaChatModel ollamaChatModel, SabreService sabreService) {
        this.chatClient = ChatClient.builder(ollamaChatModel).build();
        this.sabreService = sabreService;
    }

    @PostMapping("/recommend-alternative")
    public ResponseEntity<String> recommendAlternative(
            @Valid @RequestBody AlternativeRequest request,
            @AuthenticationPrincipal User currentUser) {

        System.out.println(">>> ChatController received request for item type: " + request.getItemType());

        String targetLocation = (request.getLocation() != null && !request.getLocation().isBlank())
                ? request.getLocation()
                : "TPE";

        String destinationLocation = (request.getDestinationLocation() != null && !request.getDestinationLocation().isBlank())
                ? request.getDestinationLocation()
                : targetLocation;

        String systemPrompt = String.format("""
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
           <input class="form-check-input" type="radio" name="selectedAlternative" value='{"newName": "[Name or Flight Number]", "price": "[Price]"}' id="alt-[UniqueId]">
           <label class="form-check-label font-weight-bold" for="alt-[UniqueId]">
               [Name / Flight Number] - [Price]
           </label>
           <p class="text-sm mb-0 mt-1">[Key dates/times] <br> <span class="text-danger">[Safety notes]</span></p>
       </div>
    4. End with a short reassurance message. Do NOT use markdown code blocks (```html) in your response, just return raw HTML.
    5. Do NOT pass null for any location or date parameter in tool calls.
    """,
                request.getItemType(), targetLocation, destinationLocation,
                request.getStartDate(), request.getEndDate(), request.getDisasterWarning()
        );

        String userInstruction = (request.getUserPrompt() != null && !request.getUserPrompt().isBlank())
                ? request.getUserPrompt()
                : "Find me safe alternative options for this affected " + request.getItemType();

        String aiResponse = this.chatClient.prompt()
                .system(systemPrompt)
                .user(userInstruction)
                .tools(sabreService) // Automatically registers all @Tool methods inside SabreService
                .call()
                .content();

        if (aiResponse == null || aiResponse.isBlank()) {
            aiResponse = "We retrieved options, but could not format the summary. Please try refreshing.";
        }

        return ResponseEntity.ok(aiResponse);
    }

    @GetMapping("/travel-assistance")
    public String getTravelAssistance(
            @RequestParam(defaultValue = "I am stranded in Taipei from 2026-10-20 to 2026-10-22 due to a typhoon. Can you recommend safe hotels?") String prompt) {

        return this.chatClient.prompt()
                .user(prompt)
                .tools(sabreService)
                .call()
                .content();
    }
}
