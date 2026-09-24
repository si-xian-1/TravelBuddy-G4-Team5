package com.TravelBuddy.service;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TravelerPasswordTests {
    private final TravelerRepository repository = mock(TravelerRepository.class);
    private final TravelerService service = new TravelerService(repository);

    @Test
    void creationStoresSaltedHash() {
        User first = new User("Alice", "91234567", "example-password", false);
        User second = new User("Bob", "91234568", "example-password", false);
        service.createUser(first);
        service.createUser(second);
        assertNotEquals("example-password", first.getPassword());
        assertNotEquals(first.getPassword(), second.getPassword());
        assertTrue(Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()
                .matches("example-password", first.getPassword()));
        verify(repository).createUser(first);
    }

    @Test
    void updatesHashNewPasswordsButIgnoreMaskAndMissingPassword() {
        User user = new User();
        user.setPassword("replacement-password");
        service.updateUser(1, user);
        assertTrue(Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()
                .matches("replacement-password", user.getPassword()));
        user.setPassword(User.PASSWORD_MASK);
        service.updateUser(1, user);
        assertNull(user.getPassword());
        service.updateUser(1, user);
        assertNull(user.getPassword());
        verify(repository, times(3)).updateUser(1, user);
    }

    @Test
    void responseSerializesOnlyFixedMask() {
        User user = new User();
        user.setPassword("secret-or-hash");
        JsonMapper mapper = JsonMapper.builder().build();
        String json = mapper.writeValueAsString(user);
        assertEquals("........", mapper.readTree(json).get("password").asText());
        assertFalse(json.contains("secret-or-hash"));
        assertFalse(json.contains("maskedPassword"));
        User incoming = mapper.readValue("{\"password\":\"new-secret\"}", User.class);
        assertEquals("new-secret", incoming.getPassword());
        assertNull(mapper.readValue("{}", User.class).getPassword());
        assertFalse(mapper.writeValueAsString(user).contains("secret-or-hash"));
    }
}
