package com.TravelBuddy.service;

import com.TravelBuddy.model.User;
import com.TravelBuddy.model.EmergencyContact;
import com.TravelBuddy.repository.TravelerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TravelerService {

    private final TravelerRepository travelerRepository;
    private final PasswordEncoder passwordEncoder =
            Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public TravelerService(TravelerRepository travelerRepository) {
        this.travelerRepository = travelerRepository;
    }

    public int createUser(User user) {
        validateUser(user, true);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return travelerRepository.createUser(user);
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
        validateUser(user, false);

        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return travelerRepository.updateUser(userId, user);
    }

    public int deleteUser(int userId) {
        return travelerRepository.deleteUser(userId);
    }

    private void validateUser(User user, boolean creating) {
        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User details are required"
            );
        }

        validateText(user.getUsername(), "Username", 50, creating);
        validateText(user.getMobileNumber(), "Mobile number", 20, creating);
        validateText(user.getPassword(), "Password", 255, creating);

        if (User.PASSWORD_MASK.equals(user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid password"
            );
        }

        if (creating && user.isHasConsentLocation() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid consent location"
            );
        }

        EmergencyContact contact = user.getEmergencyContact();

        if (contact != null) {
            validateText(
                    contact.getName(),
                    "Emergency contact name",
                    50,
                    false
            );

            validateText(
                    contact.getMobileNumber(),
                    "Emergency contact mobile number",
                    8,
                    false
            );
        }
    }

    private void validateText(
            String value,
            String field,
            int maxLength,
            boolean required
    ) {
        if (value == null) {
            if (required) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        field + " is required"
                );
            }
            return;
        }

        if (value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " cannot be blank"
            );
        }

        if (value.length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " can be at most " + maxLength + " characters"
            );
        }
    }
}