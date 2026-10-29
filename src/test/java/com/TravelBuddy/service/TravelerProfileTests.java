package com.TravelBuddy.service;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TravelerProfileTests {
    private final TravelerRepository repository = mock(TravelerRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final TravelerService service = new TravelerService(repository, encoder);

    @Test
    void phoneValidationRequiresEightDigitsAndAllowsConsentWithdrawal() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            User user = new User();
            user.setUsername("traveler");
            user.setMobileNumber("9123456");
            user.setEmergencyContactMobileNumber("9876543");
            user.setHasConsentLocation(false);
            var errors = validator.validate(user);
            assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("mobileNumber")));
            assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("emergencyContactNumber")));
            user.setMobileNumber("91234567");
            user.setEmergencyContactMobileNumber("98765432");
            assertTrue(validator.validate(user).isEmpty());
            user.setEmergencyContactMobileNumber("");
            assertTrue(validator.validate(user).isEmpty());
            user.setMobileNumber("abcdefgh");
            assertFalse(validator.validate(user).isEmpty());
        }
    }

    @Test
    void profileUpdatePreservesPasswordAndConsent() {
        User updates = new User();
        updates.setUsername("new-name");
        updates.setMobileNumber("91234567");
        updates.setEmergencyContactName("Contact");
        updates.setEmergencyContactMobileNumber("98765432");
        when(repository.updateUser(7, updates)).thenReturn(1);

        assertEquals(1, service.updateUser(7, updates));
        assertNull(updates.getPassword());
        assertNull(updates.getHasConsentLocation());
        verify(repository).updateUser(7, updates);
        verifyNoInteractions(encoder);
    }

    @Test
    void duplicateUsernameReturnsConflict() {
        User updates = new User();
        updates.setUsername("taken");
        when(repository.updateUser(7, updates)).thenThrow(new DuplicateKeyException("duplicate"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateUser(7, updates));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void blankMobileIsRejectedBeforeWriting() {
        User updates = new User();
        updates.setMobileNumber(" ");
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateUser(7, updates));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository);
    }
}
