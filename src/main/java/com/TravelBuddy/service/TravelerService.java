package com.TravelBuddy.service;

import com.TravelBuddy.model.User;
import com.TravelBuddy.model.EmergencyContact;
import com.TravelBuddy.repository.TravelerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TravelerService {

    private final TravelerRepository travelerRepository;

    public TravelerService(TravelerRepository travelerRepository) {
        this.travelerRepository = travelerRepository;
    }

    public int createUser(User user) {
        validateUser(user, true);
        return travelerRepository.createUser(user);
    }
    public int updateUser(int userId, User user){
        if (userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID must be positive");
        }
        validateUser(user, false);
        return travelerRepository.updateUser(userId, user);
    }

    private void validateUser(User user, boolean creating) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User details are required");
        }

        // Creation requires these fields; partial updates validate only supplied values.
        validateText(user.getUsername(), "Username", 50, creating);
        validateText(user.getMobileNumber(), "Mobile number", 20, creating);
        validateText(user.getPassword(), "Password", 255, creating);
        if (user.isHasConsentLocation() == null){
            
        }

        EmergencyContact contact = user.getEmergencyContact();
        if (contact != null) {
            validateText(contact.getName(), "Emergency contact name", 50, false);
            validateText(contact.getMobileNumber(), "Emergency contact mobile number", 8, false);
        }
    }

    private void validateText(String value, String field, int maxLength, boolean required) {
        if (value == null) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
            }
            return;
        }
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " cannot be blank");
        }
        if (value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " can be at most " + maxLength + " characters");
        }
    }
}
