package com.TravelBuddy.service;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;
import org.springframework.stereotype.Service;

@Service
public class TravelerService {

    private final TravelerRepository travelerRepository;

    public TravelerService(TravelerRepository travelerRepository) {
        this.travelerRepository = travelerRepository;
    }

    public int createUser(User user) {
        return travelerRepository.createUser(user);
    }

    public User authenticateUser(String username, String password) {
        return travelerRepository.authenticateUser(username, password);
    }
}