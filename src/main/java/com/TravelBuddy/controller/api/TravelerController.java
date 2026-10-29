package com.TravelBuddy.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.User;
import com.TravelBuddy.service.TravelerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/travelers")
public class TravelerController {

    private final TravelerService travelerService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public TravelerController(
            TravelerService travelerService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {

        this.travelerService = travelerService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Create a new account
     * @param user
     * @return Status of account creation
     */
    @PostMapping("/signup")
    public String createUser(@Valid @RequestBody User user) {
        travelerService.createUser(user);
        return "User created successfully";
    }

    /**
     * Authenticates a user with provided credentials and establish an http session
     * @param user
     * @param request current http servlet request used to establish session
     * @param response http servlet response where session cookie (JSESSIONID) is set
     * @return Authenticated user details
     */
    @PostMapping("/login")
    public User login(
            @Valid @RequestBody User user,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Authenticate user against TravelerUserDetailsService and PasswordEncoder
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        user.getPassword()));

        // Create a new security context and bind the authenticated token
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Persists security context in http session and emit session cookie
        securityContextRepository.saveContext(
                context,
                request,
                response);

        return (User) authentication.getPrincipal();
    }

    /**
     * Update own profile details
     * @param currentUser
     * @param user
     * @return Status of update operation
     */
    @PutMapping("/updateProfile")
    public String updateUser(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody User user,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (travelerService.updateUser(currentUser.getUserId(), user) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Traveler not found");
        }

        currentUser.setUsername(user.getUsername());
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
        return "User updated successfully";
    }

    /**
     * Retrieve profile details
     * @param currentUser
     * @return User object of profile details
     */
    @GetMapping("/myProfile")
    public User getUser(@AuthenticationPrincipal User currentUser) {
        return travelerService.getUser(currentUser.getUserId());
    }

    /**
     * Delete account
     * @param currentUser
     * @return Status of delete operation
     */
    @DeleteMapping("/deleteProfile")
    public String deleteUser(@AuthenticationPrincipal User currentUser) {
        if (travelerService.deleteUser(currentUser.getUserId()) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Traveler not found");
        }

        return "User account deleted";
    }
}
