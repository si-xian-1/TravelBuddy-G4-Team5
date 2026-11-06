package com.TravelBuddy.service;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelerProfileTests {
    @Mock
    private TravelerRepository repository;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private TravelerService service;

    @Test
    @DisplayName("Should fail validation when mobile number is shorter than 8 digits")
    void mobileNumberShorterThanEightDigitsIsRejected() {
        // Arrange
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            User user = new User();
            user.setUsername("traveler");
            user.setMobileNumber("91234567");
            user.setEmergencyContactMobileNumber("98765432");
            user.setHasConsentLocation(true);
            user.setMobileNumber("9123456");

            // Act
            var errors = validator.validate(user);

            // Assert
            assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("mobileNumber")));
        }
    }

    @Test
    @DisplayName("Should fail validation when emergency contact mobile number is shorter than 8 digits")
    void emergencyContactNumberShorterThanEightDigitsIsRejected() {
        // Arrange
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            User user = new User();
            user.setUsername("traveler");
            user.setMobileNumber("91234567");
            user.setEmergencyContactMobileNumber("98765432");
            user.setHasConsentLocation(true);
            user.setEmergencyContactMobileNumber("9876543");

            // Act
            var errors = validator.validate(user);

            // Assert
            assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("emergencyContactNumber")));
        }
    }

    @Test
    @DisplayName("Should pass validation when mobile number and emergency contact number are valid 8-digit numbers")
    void eightDigitPhoneNumbersAreAccepted() {
        // Arrange
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            User user = new User();
            user.setUsername("traveler");
            user.setMobileNumber("91234567");
            user.setEmergencyContactMobileNumber("98765432");
            user.setHasConsentLocation(true);

            // Act
            var errors = validator.validate(user);

            // Assert
            assertTrue(errors.isEmpty());
        }
    }

    @Test
    @DisplayName("Should pass validation when location consent is set to false (withdrawn)")
    void locationConsentCanBeWithdrawn() {
        // Arrange
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            User user = new User();
            user.setUsername("traveler");
            user.setMobileNumber("91234567");
            user.setEmergencyContactMobileNumber("98765432");
            user.setHasConsentLocation(true);
            user.setHasConsentLocation(false);

            // Act
            var errors = validator.validate(user);

            // Assert
            assertTrue(errors.isEmpty());
        }
    }

    @Test
    @DisplayName("Should pass validation when emergency contact mobile number is empty (optional)")
    void emptyEmergencyContactNumberIsAccepted() {
        // Arrange
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            User user = new User();
            user.setUsername("traveler");
            user.setMobileNumber("91234567");
            user.setEmergencyContactMobileNumber("98765432");
            user.setHasConsentLocation(true);
            user.setEmergencyContactMobileNumber("");

            // Act
            var errors = validator.validate(user);

            // Assert
            assertTrue(errors.isEmpty());
        }
    }

    @Test
    @DisplayName("Should fail validation when mobile number contains non-digit characters")
    void nonDigitMobileNumberIsRejected() {
        // Arrange
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            User user = new User();
            user.setUsername("traveler");
            user.setMobileNumber("91234567");
            user.setEmergencyContactMobileNumber("98765432");
            user.setHasConsentLocation(true);
            user.setMobileNumber("abcdefgh");

            // Act
            var errors = validator.validate(user);

            // Assert
            assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("mobileNumber")));
        }
    }

    @Test
    @DisplayName("Should successfully update traveler profile while preserving password and location consent")
    void profileUpdatePreservesPasswordAndConsent() {
        // Arrange
        User updates = new User();
        updates.setUsername("new-name");
        updates.setMobileNumber("91234567");
        updates.setEmergencyContactName("Contact");
        updates.setEmergencyContactMobileNumber("98765432");
        when(repository.updateUser(7, updates)).thenReturn(1);

        // Act
        int rowsUpdated = service.updateUser(7, updates);

        // Assert
        assertEquals(1, rowsUpdated);
        assertNull(updates.getPassword());
        assertNull(updates.getHasConsentLocation());
        verify(repository).updateUser(7, updates);
        verifyNoInteractions(encoder);
    }

    @Test
    @DisplayName("Should throw CONFLICT when updated username already exists in repository")
    void duplicateUsernameReturnsConflict() {
        // Arrange
        User updates = new User();
        updates.setUsername("taken");
        when(repository.updateUser(7, updates)).thenThrow(new DuplicateKeyException("duplicate"));

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateUser(7, updates));

        // Assert
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when mobile number is blank whitespace before updating repository")
    void blankMobileIsRejectedBeforeWriting() {
        // Arrange
        User updates = new User();
        updates.setMobileNumber(" ");

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateUser(7, updates));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository);
    }
}
