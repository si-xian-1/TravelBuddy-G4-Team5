package com.TravelBuddy.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;
import com.TravelBuddy.service.TravelerUserDetailsService;

class AuthenticationTests {

    private static final String USERNAME = "test-traveler";
    private static final String PASSWORD = "correct-password";

    private AuthenticationManager authenticationManager;
    private TravelerRepository travelerRepository;

    @BeforeEach
    void setUp() {
        travelerRepository = mock(TravelerRepository.class);
        PasswordEncoder passwordEncoder =
                Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        User user = new User();
        user.setUserId(1);
        user.setUsername(USERNAME);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole("TRAVELER");

        when(travelerRepository.findCredentialsByUsername(USERNAME))
                .thenReturn(user);
        when(travelerRepository.findCredentialsByUsername("unknown"))
                .thenReturn(null);

        TravelerUserDetailsService userDetailsService =
                new TravelerUserDetailsService(travelerRepository);
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        authenticationManager = new ProviderManager(provider);
    }

    @Test
    void authenticatesWithCorrectCredentials() {
        Authentication result = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD));

        assertEquals(USERNAME, result.getName());
        assertTrue(result.isAuthenticated());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TRAVELER")));
        assertNull(result.getCredentials()); // erased after authentication
    }

    @Test
    void rejectsIncorrectPassword() {
        assertThrows(BadCredentialsException.class, () ->
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(USERNAME, "wrong-password")));
    }

    @Test
    void rejectsUnknownUsername() {
        assertThrows(BadCredentialsException.class, () ->
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken("unknown", PASSWORD)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void rejectsNullOrBlankUsername(String username) {
        assertThrows(BadCredentialsException.class, () ->
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(username, PASSWORD)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   "})
    void rejectsNullOrBlankPassword(String password) {
        assertThrows(BadCredentialsException.class, () ->
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(USERNAME, password)));
    }
}