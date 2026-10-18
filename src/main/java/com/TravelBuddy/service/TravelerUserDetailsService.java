package com.TravelBuddy.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;

/**
 * Custom UserDetailsService implementation to load traveler account credentials
 * from the database during Spring Security authentication.
*/
@Service
public class TravelerUserDetailsService implements UserDetailsService {
    private final TravelerRepository travelerRepository;

    public TravelerUserDetailsService(
            TravelerRepository travelerRepository) {
        this.travelerRepository = travelerRepository;
    }

    /**
     * Locates the user based on the username for credential verification.
     * @param username the username identifying the user whose data is required
     * @return user stored credentials and authorities
     * @throws UsernameNotFoundException if no user with the given username is found
     */
    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = travelerRepository.findCredentialsByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("Invalid username or password");
        }

        return user;
    }
}
