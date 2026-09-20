package com.TravelBuddy.controller.api;
import com.TravelBuddy.model.User;
import com.TravelBuddy.service.TravelerService;

import org.springframework.web.bind.annotation.*;

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
}
