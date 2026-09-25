package com.TravelBuddy.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.User;
import com.TravelBuddy.service.TravelerService;

@RestController
@RequestMapping("/api/travelers")
public class TravelerController {

    private final TravelerService travelerService;

    public TravelerController(TravelerService travelerService) {
        this.travelerService = travelerService;
    }

    @PostMapping("/signup")
    public String createUser(@RequestBody User user) {
        travelerService.createUser(user);
        return "User created successfully";
    }

    @PostMapping("/login")
    public User login(@RequestBody User user) {
        return travelerService.authenticateUser(
                user.getUsername(),
                user.getPassword()
        );
    }

    @PutMapping("/{userID}")
    public String updateUser(
            @PathVariable("userID") int userId,
            @RequestBody User user
    ) {
        if (travelerService.updateUser(userId, user) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Traveler not found"
            );
        }

        return "User updated successfully";
    }

    @GetMapping("/{userID}")
    public User getUser(@PathVariable("userID") int userId) {
        return travelerService.getUser(userId);
    }

    @DeleteMapping("/{userID}")
    public String deleteUser(@PathVariable("userID") int userId) {
        if (travelerService.deleteUser(userId) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Traveler not found"
            );
        }

        return "User account deleted";
    }
}