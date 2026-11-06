package com.TravelBuddy.service;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordTests {
    @Mock
    private TravelerRepository repository;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private TravelerService service;

    @Test
    @DisplayName("Should throw BAD_REQUEST and not update database when new password and confirm password do not match")
    void mismatchedPasswordsNeverWrite() {
        // Arrange
        String newPassword = "new";
        String confirmPassword = "typo";

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.changePassword(7, newPassword, confirmPassword));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository, encoder);
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST and not update database when password is blank or whitespace")
    void blankPasswordsNeverWrite() {
        // Arrange
        String password = " ";

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.changePassword(7, password, password));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository, encoder);
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST and not update database when password exceeds 255 characters")
    void overlyLongPasswordsNeverWrite() {
        // Arrange
        String password = "a".repeat(256);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.changePassword(7, password, password));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository, encoder);
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when new password is the same as the current password")
    void rejectsExistingPasswordByCheckingHash() {
        // Arrange
        when(repository.getPasswordHash(7)).thenReturn("old-hash");
        when(encoder.matches("old", "old-hash")).thenReturn(true);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.changePassword(7, "old", "old"));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repository, never()).updatePassword(anyInt(), anyString(), anyString());
        verify(encoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should successfully encode new password and update password in repository for valid input")
    void savesEncodedPasswordForAuthenticatedUserOnly() {
        // Arrange
        when(repository.getPasswordHash(7)).thenReturn("old-hash");
        when(encoder.encode(" new ")).thenReturn("new-hash");
        when(repository.updatePassword(7, "old-hash", "new-hash")).thenReturn(1);

        // Act
        service.changePassword(7, " new ", " new ");

        // Assert
        verify(encoder).matches(" new ", "old-hash");
        verify(repository).updatePassword(7, "old-hash", "new-hash");
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when attempting to update password directly via profile update endpoint")
    void profileEndpointCannotBypassPasswordChecks() {
        // Arrange
        User user = new User();
        user.setPassword("replacement");

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateUser(7, user));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository, encoder);
    }
}
