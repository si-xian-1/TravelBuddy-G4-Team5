package com.TravelBuddy.controller.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.TravelBuddy.model.AlternativeRequest;
import com.TravelBuddy.model.User;
import com.TravelBuddy.service.AiTaskCancellationManager;
import com.TravelBuddy.service.SabreService;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Mock
    private SabreService sabreService;

    private AiTaskCancellationManager cancellationManager;
    private ChatController chatController;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        cancellationManager = spy(new AiTaskCancellationManager());
        chatController = new ChatController(chatClientBuilder, sabreService, cancellationManager);
    }

    private void mockChatClient(String htmlOutput) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any(SabreService.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(htmlOutput);
    }

    @Test
    @DisplayName("Requesting alternative travel recommendations returns generated booking options for affected plans")
    void recommendAlternative_success() {
        // Arrange
        AlternativeRequest request = new AlternativeRequest();
        request.setItemType("HOTEL");
        request.setLocation("TPE");
        request.setStartDate("2026-11-01");
        request.setEndDate("2026-11-05");
        request.setRequestId("req-chat-001");

        String expectedHtml = "<div class='form-check'>Hotel A</div>";
        mockChatClient(expectedHtml);

        User user = new User();
        user.setUsername("Traveler1");

        // Act
        ResponseEntity<String> response = chatController.recommendAlternative(request, null, user);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedHtml, response.getBody());
    }

    @Test
    @DisplayName("Requesting alternatives displays helpful fallback message when assistant generates no suggestions")
    void recommendAlternative_whenAiResponseIsBlank_returnsFallbackMessage() {
        // Arrange
        AlternativeRequest request = new AlternativeRequest();
        request.setItemType("FLIGHT");
        request.setLocation("TPE");
        request.setStartDate("2026-11-01");
        request.setEndDate("2026-11-05");
        request.setRequestId("req-chat-blank");

        mockChatClient("   ");

        User user = new User();
        user.setUsername("Traveler1");

        // Act
        ResponseEntity<String> response = chatController.recommendAlternative(request, null, user);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Unable to display results. Please try refreshing.", response.getBody());
    }

    @Test
    @DisplayName("Requesting alternatives displays a friendly error notification when recommendation assistant fails")
    void recommendAlternative_whenException_returnsErrorMessage() {
        // Arrange
        AlternativeRequest request = new AlternativeRequest();
        request.setItemType("HOTEL");
        request.setLocation("TPE");
        request.setStartDate("2026-11-01");
        request.setEndDate("2026-11-05");
        request.setRequestId("req-chat-err");

        when(chatClient.prompt()).thenThrow(new RuntimeException("Simulated AI failure"));

        User user = new User();
        user.setUsername("Traveler1");

        // Act
        ResponseEntity<String> response = chatController.recommendAlternative(request, null, user);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Failed to retrieve alternative recommendations"));
    }

    @Test
    @DisplayName("Stopping recommendation generation terminates the process and displays cancellation notice")
    void recommendAlternative_whenCancelled_returnsCancelledMessage() {
        // Arrange
        AlternativeRequest request = new AlternativeRequest();
        request.setItemType("FLIGHT");
        request.setLocation("SIN");
        request.setStartDate("2026-11-01");
        request.setEndDate("2026-11-05");
        request.setRequestId("req-chat-cancel");

        Thread.currentThread().interrupt();

        User user = new User();
        user.setUsername("Traveler2");

        // Act
        ResponseEntity<String> response = chatController.recommendAlternative(request, null, user);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("cancelled"));
        Thread.interrupted(); // Clear interrupted status for subsequent test execution
    }

    @Test
    @DisplayName("Cancelling ongoing travel recommendation stops background task and confirms cancellation")
    void cancelAiGeneration_returnsCancelledStatus() {
        // Arrange
        String requestId = "req-to-cancel-123";
        User user = new User();
        user.setUsername("Traveler3");

        // Act
        ResponseEntity<Map<String, Object>> response = chatController.cancelAiGeneration(requestId, user);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("cancelled", response.getBody().get("status"));
        assertEquals(requestId, response.getBody().get("requestId"));
        verify(cancellationManager).cancel(requestId);
    }
}
