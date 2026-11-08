package com.TravelBuddy.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;
import com.TravelBuddy.service.TravelerUserDetailsService;

@ExtendWith(MockitoExtension.class)
class AuthenticationTest {

    private static final String USERNAME = "test-traveler";
    private static final String PASSWORD = "correct-password";

    @Mock
    private TravelerRepository travelerRepository;

    @InjectMocks
    private TravelerUserDetailsService userDetailsService;

    private AuthenticationManager authenticationManager;
    private User testUser;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder =
                Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        testUser = new User();
        testUser.setUserId(1);
        testUser.setUsername(USERNAME);
        testUser.setPassword(passwordEncoder.encode(PASSWORD));
        testUser.setRole("TRAVELER");

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        authenticationManager = new ProviderManager(provider);
    }

    @Test
    @DisplayName("Successfully authenticate traveler with valid username and password")
    void authenticatesWithCorrectCredentials() {
        // Arrange
        when(travelerRepository.findCredentialsByUsername(USERNAME)).thenReturn(testUser);

        // Act
        Authentication result = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD));

        // Assert
        assertEquals(USERNAME, result.getName());
        assertTrue(result.isAuthenticated());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TRAVELER")));
        assertNull(result.getCredentials()); // erased after authentication
    }

    @Test
    @DisplayName("Reject login attempt when given an incorrect password")
    void rejectsIncorrectPassword() {
        // Arrange
        when(travelerRepository.findCredentialsByUsername(USERNAME)).thenReturn(testUser);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () ->
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(USERNAME, "wrong-password")));
    }

    @Test
    @DisplayName("Reject login attempt when given an unregistered username")
    void rejectsUnknownUsername() {
        // Arrange
        when(travelerRepository.findCredentialsByUsername("unknown")).thenReturn(null);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () ->
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken("unknown", PASSWORD)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("Reject login attempt when username is empty or blank")
    void rejectsNullOrBlankUsername(String username) {
        // Arrange
        when(travelerRepository.findCredentialsByUsername(any())).thenReturn(null);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () ->
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(username, PASSWORD)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("Reject login attempt when password is empty or blank")
    void rejectsNullOrBlankPassword(String password) {
        // Arrange
        when(travelerRepository.findCredentialsByUsername(USERNAME)).thenReturn(testUser);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () ->
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(USERNAME, password)));
    }
}
