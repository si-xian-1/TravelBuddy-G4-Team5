package com.TravelBuddy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;

class TravelerServiceTests {

    private static final int USER_ID = 42;

    private TravelerRepository travelerRepository;
    private PasswordEncoder passwordEncoder;
    private TravelerService travelerService;

    @BeforeEach
    void setUp() {
        travelerRepository = mock(TravelerRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        travelerService = new TravelerService(travelerRepository, passwordEncoder);
    }

    // ---------- createUser ----------

    @Test
    void createsUserWithEncodedPassword() {
        User user = validUser();
        when(passwordEncoder.encode("test-password")).thenReturn("encoded-password");
        when(travelerRepository.createUser(user)).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            assertEquals("encoded-password", saved.getPassword());
            return 1;
        });

        int result = travelerService.createUser(user);

        assertEquals(1, result);
        verify(passwordEncoder).encode("test-password");
        verify(travelerRepository).createUser(user);
    }

    @Test
    void doesNotTrimPasswordBeforeEncodingOnSignup() {
        User user = validUser();
        user.setPassword("  spaced password  ");
        when(passwordEncoder.encode("  spaced password  ")).thenReturn("encoded-password");
        when(travelerRepository.createUser(user)).thenReturn(1);

        travelerService.createUser(user);

        verify(passwordEncoder).encode("  spaced password  ");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsSignupWhenMobileNumberIsMissingOrBlank(String mobileNumber) {
        User user = validUser();
        user.setMobileNumber(mobileNumber);

        assertSignupRejected(user);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false})
    void rejectsSignupWhenLocationConsentIsMissingOrFalse(Boolean consent) {
        User user = validUser();
        user.setHasConsentLocation(consent);

        assertSignupRejected(user);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsSignupWhenPasswordIsMissingOrBlank(String password) {
        User user = validUser();
        user.setPassword(password);

        assertSignupRejected(user);
    }

    @Test
    void mapsDuplicateUsernameDuringSignupToConflict() {
        User user = validUser();
        when(passwordEncoder.encode("test-password")).thenReturn("encoded-password");
        when(travelerRepository.createUser(user))
                .thenThrow(new DuplicateKeyException("duplicate username"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.createUser(user));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void encodesPasswordMaskAsOrdinaryPasswordOnSignup() {
        // Current behaviour: the mask is only rejected on update, not on signup.
        User user = validUser();
        user.setPassword(User.PASSWORD_MASK);
        when(passwordEncoder.encode(User.PASSWORD_MASK)).thenReturn("encoded-mask");
        when(travelerRepository.createUser(user)).thenReturn(1);

        int result = travelerService.createUser(user);

        assertEquals(1, result);
        verify(passwordEncoder).encode(User.PASSWORD_MASK);
    }

    @Test
    void doesNotTrimMobileNumberOnSignup() {
        User user = validUser();
        user.setMobileNumber("  91234567 ");
        when(passwordEncoder.encode("test-password")).thenReturn("encoded-password");
        when(travelerRepository.createUser(user)).thenReturn(1);

        travelerService.createUser(user);

        assertEquals("  91234567 ", user.getMobileNumber());
        verify(travelerRepository).createUser(user);
    }

    // ---------- getUser ----------

    @Test
    void returnsUserWhenFound() {
        User expected = validUser();
        when(travelerRepository.getUser(USER_ID)).thenReturn(expected);

        User result = travelerService.getUser(USER_ID);

        assertSame(expected, result);
        verify(travelerRepository).getUser(USER_ID);
    }

    @Test
    void throwsNotFoundWhenUserDoesNotExist() {
        when(travelerRepository.getUser(USER_ID)).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.getUser(USER_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // ---------- updateUser ----------

    @Test
    void rejectsPasswordMaskDuringProfileUpdate() {
        User update = new User();
        update.setPassword(User.PASSWORD_MASK);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.updateUser(USER_ID, update));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(travelerRepository, passwordEncoder);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void preservesExistingPasswordWhenUpdatePasswordIsMissingOrBlank(String password) {
        User update = new User();
        update.setPassword(password);
        when(travelerRepository.updateUser(USER_ID, update)).thenReturn(1);

        int result = travelerService.updateUser(USER_ID, update);

        assertEquals(1, result);
        assertNull(update.getPassword()); // null lets the repository's COALESCE keep the old hash
        verify(travelerRepository).updateUser(USER_ID, update);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void encodesReplacementPasswordDuringProfileUpdate() {
        User update = new User();
        update.setPassword("replacement-password");
        when(passwordEncoder.encode("replacement-password")).thenReturn("encoded-replacement");
        when(travelerRepository.updateUser(USER_ID, update)).thenAnswer(inv -> {
            User saved = inv.getArgument(1);
            assertEquals("encoded-replacement", saved.getPassword());
            return 1;
        });

        int result = travelerService.updateUser(USER_ID, update);

        assertEquals(1, result);
        verify(passwordEncoder).encode("replacement-password");
        verify(travelerRepository).updateUser(USER_ID, update);
    }

    @Test
    void doesNotTrimPasswordBeforeEncodingOnUpdate() {
        User update = new User();
        update.setPassword("  spaced password  ");
        when(passwordEncoder.encode("  spaced password  ")).thenReturn("encoded-replacement");
        when(travelerRepository.updateUser(USER_ID, update)).thenReturn(1);

        travelerService.updateUser(USER_ID, update);

        verify(passwordEncoder).encode("  spaced password  ");
    }

    @Test
    void mapsDuplicateUsernameDuringUpdateToConflict() {
        User update = new User();
        update.setPassword("replacement-password");
        when(passwordEncoder.encode("replacement-password")).thenReturn("encoded-replacement");
        when(travelerRepository.updateUser(USER_ID, update))
                .thenThrow(new DuplicateKeyException("duplicate username"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.updateUser(USER_ID, update));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void returnsZeroWhenUpdatingNonexistentUser() {
        // Current behaviour: no NOT_FOUND mapping, the repository's 0 is passed through.
        User update = new User();
        when(travelerRepository.updateUser(USER_ID, update)).thenReturn(0);

        int result = travelerService.updateUser(USER_ID, update);

        assertEquals(0, result);
        verify(travelerRepository).updateUser(USER_ID, update);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void passesBlankMobileNumberToRepositoryDuringUpdate(String mobileNumber) {
        // Current behaviour: blank values are not validated on update, so the
        // repository's COALESCE will overwrite the stored number with the blank.
        User update = new User();
        update.setMobileNumber(mobileNumber);
        when(travelerRepository.updateUser(USER_ID, update)).thenReturn(1);

        int result = travelerService.updateUser(USER_ID, update);

        assertEquals(1, result);
        assertEquals(mobileNumber, update.getMobileNumber());
        verify(travelerRepository).updateUser(USER_ID, update);
    }

    @Test
    void allowsRevokingLocationConsentDuringUpdate() {
        User update = new User();
        update.setHasConsentLocation(false);
        when(travelerRepository.updateUser(USER_ID, update)).thenReturn(1);

        int result = travelerService.updateUser(USER_ID, update);

        assertEquals(1, result);
        verify(travelerRepository).updateUser(USER_ID, update);
    }

    // ---------- deleteUser ----------

    @Test
    void deletesUserAndReturnsRepositoryResult() {
        when(travelerRepository.deleteUser(USER_ID)).thenReturn(1);

        int result = travelerService.deleteUser(USER_ID);

        assertEquals(1, result);
        verify(travelerRepository).deleteUser(USER_ID);
    }

    @Test
    void returnsZeroWhenDeletingNonexistentUser() {
        // Current behaviour: no NOT_FOUND mapping, the repository's 0 is passed through.
        when(travelerRepository.deleteUser(USER_ID)).thenReturn(0);

        int result = travelerService.deleteUser(USER_ID);

        assertEquals(0, result);
        verify(travelerRepository).deleteUser(USER_ID);
    }

    // ---------- helpers ----------

    private void assertSignupRejected(User user) {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelerService.createUser(user));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(travelerRepository, passwordEncoder);
    }

    private User validUser() {
        User user = new User();
        user.setUsername("test-traveler");
        user.setMobileNumber("91234567");
        user.setPassword("test-password");
        user.setHasConsentLocation(true);
        return user;
    }
}