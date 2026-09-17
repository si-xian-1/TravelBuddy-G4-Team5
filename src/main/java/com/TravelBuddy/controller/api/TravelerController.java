package com.TravelBuddy.controller.api;
import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.TravelerRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/travelers")
//creates REST endpoint:
public class TravelerController {
      private final TravelerRepository travelerRepository;

    public TravelerController(TravelerRepository travelerRepository) {
        this.travelerRepository = travelerRepository;
    }

    @PostMapping
    public String createUser(@RequestBody User user) {
        travelerRepository.createUser(user);
        return "User created successfully";
    }
}
