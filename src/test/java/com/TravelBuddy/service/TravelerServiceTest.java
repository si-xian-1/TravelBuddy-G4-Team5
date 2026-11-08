package com.TravelBuddy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@ExtendWith(MockitoExtension.class)
class TravelerServiceTest {

    private static final int USER_ID = 42;

    @Mock
    private TravelerRepository travelerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TravelerService travelerService;

    private User validUser;

    @BeforeEach
    void setUp() {
        validUser = new User();
        validUser.setUsername("test-traveler");
        validUser.setMobileNumber("91234567");
        validUser.setPassword("test-password");
        validUser.setHasConsentLocation(true);
    }

    @Test
    @DisplayName("Creating a traveler account securely encodes the password and persists the profile")
    void createsUserWithEncodedPassword() {
        when(passwordEncoder.encode("test-password")).thenReturn("encoded-password");
        when(travelerRepository.createUser(validUser)).thenReturn(1);

        int result = travelerService.createUser(validUser);

        assertEquals(1, result);
        assertEquals("encoded-password", validUser.getPassword());
        verify(passwordEncoder).encode("test-password");
        verify(travelerRepository).createUser(validUser);
    }

    @Test
    @DisplayName("Password whitespace is preserved as entered during registration encoding")
    void doesNotTrimPasswordBeforeEncodingOnSignup() {
        validUser.setPassword("  spaced password  ");
        when(passwordEncoder.encode("  spaced password  ")).thenReturn("encoded-password");
        when(travelerRepository.createUser(validUser)).thenReturn(1);

        int result = travelerService.createUser(validUser);

        assertEquals(1, result);
        verify(passwordEncoder).encode("  spaced password  ");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Account creation requires a contact number")
    void rejectsSignupWhenMobileNumberIsMissingOrBlank(String mobileNumber) {
        validUser.setMobileNumber(mobileNumber);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.createUser(validUser));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(travelerRepository, passwordEncoder);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false})
    @DisplayName("Account creation requires explicit location sharing consent")
    void rejectsSignupWhenLocationConsentIsMissingOrFalse(Boolean consent) {
        validUser.setHasConsentLocation(consent);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.createUser(validUser));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(travelerRepository, passwordEncoder);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Account creation requires a non-empty password")
    void rejectsSignupWhenPasswordIsMissingOrBlank(String password) {
        validUser.setPassword(password);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.createUser(validUser));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(travelerRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Registering with an existing username signals a duplicate account conflict")
    void mapsDuplicateUsernameDuringSignupToConflict() {
        when(passwordEncoder.encode("test-password")).thenReturn("encoded-password");
        when(travelerRepository.createUser(validUser))
                .thenThrow(new DuplicateKeyException("duplicate username"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.createUser(validUser));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    @DisplayName("Retrieving profile for an existing traveler returns account details")
    void returnsUserWhenFound() {
        when(travelerRepository.getUser(USER_ID)).thenReturn(validUser);

        User result = travelerService.getUser(USER_ID);

        assertSame(validUser, result);
        verify(travelerRepository).getUser(USER_ID);
    }

    @Test
    @DisplayName("Retrieving profile for an unrecognized user indicates account not found")
    void throwsNotFoundWhenUserDoesNotExist() {
        when(travelerRepository.getUser(USER_ID)).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.getUser(USER_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("Updating profile safely preserves password and consent without unintended modification")
    void profileUpdatePreservesPasswordAndConsent() {
        User updates = new User();
        updates.setUsername("new-name");
        updates.setMobileNumber("91234567");
        updates.setEmergencyContactName("Contact");
        updates.setEmergencyContactMobileNumber("98765432");
        when(travelerRepository.updateUser(USER_ID, updates)).thenReturn(1);

        int rowsUpdated = travelerService.updateUser(USER_ID, updates);

        assertEquals(1, rowsUpdated);
        assertNull(updates.getPassword());
        verify(travelerRepository).updateUser(USER_ID, updates);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Updating profile with an existing username is rejected as an account conflict")
    void duplicateUsernameReturnsConflict() {
        User updates = new User();
        updates.setUsername("taken");
        when(travelerRepository.updateUser(USER_ID, updates))
                .thenThrow(new DuplicateKeyException("duplicate"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.updateUser(USER_ID, updates));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    @DisplayName("Updating profile with blank contact number is rejected before persisting")
    void blankMobileIsRejectedBeforeWriting() {
        User updates = new User();
        updates.setMobileNumber(" ");

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.updateUser(USER_ID, updates));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(travelerRepository);
    }

    @Test
    @DisplayName("Updating profile allows traveler to withdraw location tracking consent")
    void allowsRevokingLocationConsentDuringUpdate() {
        User update = new User();
        update.setHasConsentLocation(false);
        when(travelerRepository.updateUser(USER_ID, update)).thenReturn(1);

        int result = travelerService.updateUser(USER_ID, update);

        assertEquals(1, result);
        verify(travelerRepository).updateUser(USER_ID, update);
    }

    @Test
    @DisplayName("Directly setting password in profile update is directed to dedicated password change")
    void rejectsDirectPasswordModificationInProfileUpdate() {
        User update = new User();
        update.setPassword("replacement-password");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.updateUser(USER_ID, update));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(travelerRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Updating an account that does not exist indicates no profile was modified")
    void returnsZeroWhenUpdatingNonexistentUser() {
        User update = new User();
        when(travelerRepository.updateUser(USER_ID, update)).thenReturn(0);

        int result = travelerService.updateUser(USER_ID, update);

        assertEquals(0, result);
        verify(travelerRepository).updateUser(USER_ID, update);
    }

    @Test
    @DisplayName("Deleting a traveler account removes the record successfully")
    void deletesUserAndReturnsRepositoryResult() {
        when(travelerRepository.deleteUser(USER_ID)).thenReturn(1);

        int result = travelerService.deleteUser(USER_ID);

        assertEquals(1, result);
        verify(travelerRepository).deleteUser(USER_ID);
    }

    @Test
    @DisplayName("Deleting a non-existent account reports no records removed")
    void returnsZeroWhenDeletingNonexistentUser() {
        when(travelerRepository.deleteUser(USER_ID)).thenReturn(0);

        int result = travelerService.deleteUser(USER_ID);

        assertEquals(0, result);
        verify(travelerRepository).deleteUser(USER_ID);
    }

    @Test
    @DisplayName("Contact number with fewer than eight digits is rejected")
    void mobileNumberShorterThanEightDigitsIsRejected() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            validUser.setMobileNumber("9123456");

            var errors = validator.validate(validUser);
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.getPropertyPath().toString().equals("mobileNumber")));
        }
    }

    @Test
    @DisplayName("Emergency contact number with fewer than eight digits is rejected")
    void emergencyContactNumberShorterThanEightDigitsIsRejected() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            validUser.setEmergencyContactMobileNumber("9876543");

            var errors = validator.validate(validUser);
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.getPropertyPath().toString().equals("emergencyContactNumber")));
        }
    }

    @Test
    @DisplayName("Valid eight-digit primary and emergency phone numbers are accepted")
    void eightDigitPhoneNumbersAreAccepted() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            validUser.setEmergencyContactMobileNumber("98765432");

            var errors = validator.validate(validUser);
            assertTrue(errors.isEmpty());
        }
    }

    @Test
    @DisplayName("Empty optional emergency contact number is permitted")
    void emptyEmergencyContactNumberIsAccepted() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            validUser.setEmergencyContactMobileNumber("");

            var errors = validator.validate(validUser);
            assertTrue(errors.isEmpty());
        }
    }

    @Test
    @DisplayName("Contact number containing alphabetic characters is rejected")
    void nonDigitMobileNumberIsRejected() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            validUser.setMobileNumber("abcdefgh");

            var errors = validator.validate(validUser);
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.getPropertyPath().toString().equals("mobileNumber")));
        }
    }
}
