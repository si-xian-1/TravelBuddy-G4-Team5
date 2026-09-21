package com.TravelBuddy.controller.api;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.User;
import com.TravelBuddy.service.TravelerService;

@RestController
@RequestMapping("/api/travelers")
//creates REST endpoint:
    public class TravelerController {
      private final TravelerService travelerService;

    public TravelerController(TravelerService travelerService) {
        this.travelerService = travelerService;
    }

    @PostMapping("/users")
    public String createUser(@RequestBody User user) {
        travelerService.createUser(user);
        return "User created successfully";
    }

    @PutMapping("/users/{userID}")
    public String updateUser(@PathVariable("userID") int userId, @RequestBody User user){
        if (travelerService.updateUser(userId, user) == 0){
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Traveler not found");
        }
        return "User updated successfully";
    }
    @GetMapping("/users/{userID}")
    public User getUser(@PathVariable("userID") int userId){
        return travelerService.getUser(userId);
    }
}
