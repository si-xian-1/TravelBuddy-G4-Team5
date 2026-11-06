package com.TravelBuddy.controller.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.TravelBuddy.service.SabreService;

class ChatControllerTests {

    private ChatClient chatClient;
    private ChatClient.Builder chatClientBuilder;
    private SabreService sabreService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        chatClientBuilder = mock(ChatClient.Builder.class);
        sabreService = mock(SabreService.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);

        ChatController controller = new ChatController(chatClientBuilder, sabreService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void recommendAlternativeReturnsGeneratedResponse() throws Exception {
        // Arrange
        String aiResponse = "<div>Verified alternative</div>";
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .tools(sabreService)
                .call()
                .content())
                .thenReturn(aiResponse);

        String requestBody = """
                {
                  "itineraryId": 1,
                  "itemType": "HOTEL",
                  "location": "Taipei",
                  "destinationLocation": "Taipei",
                  "startDate": "2026-11-10",
                  "endDate": "2026-11-12",
                  "disasterWarning": "Typhoon warning"
                }
                """;

        // Act
        mockMvc.perform(post("/api/chat/recommend-alternative")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                // Assert
                .andExpect(status().isOk())
                .andExpect(content().string(aiResponse));
    }

    @Test
    void recommendAlternativeReturnsFallbackWhenAiResponseIsBlank() throws Exception {
        // Arrange
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .tools(sabreService)
                .call()
                .content())
                .thenReturn("  ");

        String requestBody = """
                {
                  "itineraryId": 1,
                  "itemType": "FLIGHT"
                }
                """;

        // Act
        mockMvc.perform(post("/api/chat/recommend-alternative")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                // Assert
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "We retrieved options, but could not format the summary. Please try refreshing."));
    }

    @Test
    void travelAssistanceReturnsGeneratedResponseForProvidedPrompt() throws Exception {
        // Arrange
        String prompt = "Suggest safe travel options.";
        String aiResponse = "Here are some suggestions.";
        when(chatClient.prompt()
                .user(anyString())
                .tools(sabreService)
                .call()
                .content())
                .thenReturn(aiResponse);

        // Act
        mockMvc.perform(get("/api/chat/travel-assistance")
                        .param("prompt", prompt))
                // Assert
                .andExpect(status().isOk())
                .andExpect(content().string(aiResponse));
    }

    @Test
    void travelAssistanceUsesDefaultPromptWhenNoneIsProvided() throws Exception {
        // Arrange
        String aiResponse = "Please follow local safety guidance.";
        when(chatClient.prompt()
                .user(anyString())
                .tools(sabreService)
                .call()
                .content())
                .thenReturn(aiResponse);

        // Act
        mockMvc.perform(get("/api/chat/travel-assistance"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(content().string(aiResponse));
    }
}