package com.TravelBuddy.service;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChangePasswordTests {
    private final TravelerRepository repository = mock(TravelerRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final TravelerService service = new TravelerService(repository, encoder);

    @Test
    void mismatchedAndBlankPasswordsNeverWrite() {
        assertThrows(ResponseStatusException.class, () -> service.changePassword(7, "new", "typo"));
        assertThrows(ResponseStatusException.class, () -> service.changePassword(7, " ", " "));
        assertThrows(ResponseStatusException.class, () -> service.changePassword(7, "a".repeat(256), "a".repeat(256)));
        verifyNoInteractions(repository, encoder);
    }

    @Test
    void rejectsExistingPasswordByCheckingHash() {
        when(repository.getPasswordHash(7)).thenReturn("old-hash");
        when(encoder.matches("old", "old-hash")).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> service.changePassword(7, "old", "old"));
        verify(repository, never()).updatePassword(anyInt(), anyString(), anyString());
        verify(encoder, never()).encode(anyString());
    }

    @Test
    void savesEncodedPasswordForAuthenticatedUserOnly() {
        when(repository.getPasswordHash(7)).thenReturn("old-hash");
        when(encoder.encode(" new ")).thenReturn("new-hash");
        when(repository.updatePassword(7, "old-hash", "new-hash")).thenReturn(1);
        service.changePassword(7, " new ", " new ");
        verify(encoder).matches(" new ", "old-hash");
        verify(repository).updatePassword(7, "old-hash", "new-hash");
    }

    @Test
    void profileEndpointCannotBypassPasswordChecks() {
        User user = new User();
        user.setPassword("replacement");
        assertThrows(ResponseStatusException.class, () -> service.updateUser(7, user));
        verifyNoInteractions(repository, encoder);
    }
}
