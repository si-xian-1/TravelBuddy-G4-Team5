package com.TravelBuddy.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;

@Service
public class TravelerService {

    private final TravelerRepository travelerRepository;
    private final PasswordEncoder passwordEncoder;

    public TravelerService(TravelerRepository travelerRepository, PasswordEncoder passwordEncoder) {
        this.travelerRepository = travelerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * New traveler account creation and validation
     * @param user
     * @return userId
     */
    public int createUser(User user) {
        if (user.getMobileNumber() == null || user.getMobileNumber().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number is required");
        }

        if (user.getHasConsentLocation() == null || !user.getHasConsentLocation()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid consent location");
        }
        // Implement password validation to ensure user entered a password. Password
        // length check is implemented in model
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

    /**
     * Get traveler details
     * @param userId
     * @return user details or not found status if user does not exists
     */
    public User getUser(int userId) {
        User user = travelerRepository.getUser(userId);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Traveler not found");
        }

        return user;
    }

    /**
     * Update traveler details
     * @param userId
     * @param user
     * @return rows updated
     */
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

    /**
     * Delete traveler account
     * @param userId
     * @return delete status
     */
    public int deleteUser(int userId) {
        return travelerRepository.deleteUser(userId);
    }
}