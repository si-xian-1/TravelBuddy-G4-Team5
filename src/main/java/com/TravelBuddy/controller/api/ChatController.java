package com.TravelBuddy.controller.api;

import com.TravelBuddy.service.SabreService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final SabreService sabreService;

    // Inject both ChatClient.Builder and SabreService
    public ChatController(ChatClient.Builder chatClientBuilder, SabreService sabreService) {
        this.chatClient = chatClientBuilder.build();
        this.sabreService = sabreService;
    }

    @GetMapping("/chat/travel-assistance")
    public String getTravelAssistance(
            @RequestParam(defaultValue = "My flight from TPE to SIN on 2026-10-20 was disrupted. What alternative flights are available?") String prompt) {

        return this.chatClient.prompt()
                .user(prompt)
                .tools(sabreService) // <--- Pass the service bean directly!
                .call()
                .content();
    }
}
