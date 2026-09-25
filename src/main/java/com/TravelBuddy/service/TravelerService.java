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
        validateUser(user);
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
        validateUser(user);

        if (travelerRepository.isUsernameTakenByOtherUser(user.getUsername(), userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
        }

        if (user.getPassword() != null) {
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

    private void validateUser(User user) {
        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User details are required"
            );
        }

        validateText(user.getUsername(), "Username", 50, true);
        validateText(user.getMobileNumber(), "Mobile number", 8, true);
        validateText(user.getPassword(), "Password", 255, true);

        if (User.PASSWORD_MASK.equals(user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid password"
            );
        }

        if (user.isHasConsentLocation() != true) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid consent location"
            );
        }

        if (user.getEmergencyContactName() != null && !user.getEmergencyContactName().isEmpty()) {
            validateText(user.getEmergencyContactName(), "Emergency contact name", 50, false);
        }
        
        if (user.getEmergencyContactMobileNumber() != null && !user.getEmergencyContactMobileNumber().isEmpty()) {
            validateText(user.getEmergencyContactMobileNumber(), "Emergency contact mobile number", 8, false);
        }
    }

    private void validateText(String value, String field, int maxLength, boolean required) {
        if (required && (value == null || value.isBlank())) {
            if (required) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        field + " is required"
                );
            }
            return;
        }

        if (value != null && value.length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " can be at most " + maxLength + " characters"
            );
        }

        if (field.toUpperCase().contains("MOBILE NUMBER")){
            for (char c: value.toCharArray()){
                if (!Character.isDigit(c)){
                    throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " must be numbers"
                    );
                }
            }
        }
    }
}