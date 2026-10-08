package com.TravelBuddy.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;

@Service
public class TravelerService {

    private final TravelerRepository travelerRepository;
    private final PasswordEncoder passwordEncoder =
            Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public TravelerService(TravelerRepository travelerRepository) {
        this.travelerRepository = travelerRepository;
    }

    public int createUser(User user) {
        // Implement password validation to ensure user entered a password. Password length check is implemented in model
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        try {
            return travelerRepository.createUser(user);
        } catch (DuplicateKeyException ex) {
            // The unique database constraint also protects concurrent updates.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use", ex);
        }
    }

    public User authenticateUser(String username, String rawPassword) {
        User credentials = travelerRepository.findCredentialsByUsername(username);

        if (credentials != null
                && passwordEncoder.matches(rawPassword, credentials.getPassword())) {
            return getUser(credentials.getUserId());
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password"
        );
    }

    public User getUser(int userId) {
        User user = travelerRepository.getUser(userId);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Traveler not found"
            );
        }

        return user;
    }

    public int updateUser(int userId, User user) {
        // Prevent mask from overwriting actual password if passed during update
        if (User.PASSWORD_MASK.equals(user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password");
        }

        // Executes when user update their password
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        try {
            return travelerRepository.updateUser(userId, user);
        } catch (DuplicateKeyException ex) {
            // The unique database constraint also protects concurrent updates.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use", ex);
        }
    }

    public int deleteUser(int userId) {
        return travelerRepository.deleteUser(userId);
    }
}