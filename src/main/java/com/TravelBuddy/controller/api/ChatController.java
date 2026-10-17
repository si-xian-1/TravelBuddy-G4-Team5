package com.TravelBuddy.controller.api;

import com.TravelBuddy.service.SabreFlightService;
import com.TravelBuddy.service.SabreHotelService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final SabreFlightService sabreFlightService;
    private final SabreHotelService sabreHotelService;

    public ChatController(ChatClient.Builder chatClientBuilder, 
                          SabreFlightService sabreFlightService, 
                          SabreHotelService sabreHotelService) {
        this.chatClient = chatClientBuilder.build();
        this.sabreFlightService = sabreFlightService;
        this.sabreHotelService = sabreHotelService;
    }

    @GetMapping("/chat/travel-assistance")
    public String getTravelAssistance(
            @RequestParam(defaultValue = "I am stranded in Taipei from 2026-10-20 to 2026-10-22 due to a typhoon. Can you recommend safe hotels?") String prompt) {

        return this.chatClient.prompt()
                .user(prompt)
                .tools(sabreFlightService, sabreHotelService)
                .call()
                .content();
    }
}
