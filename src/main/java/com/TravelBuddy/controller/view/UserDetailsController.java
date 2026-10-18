package com.TravelBuddy.controller.view;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.TravelBuddy.model.User;

/**
 * Gloabl controller to provide user session attributes to all thymeleaf views
 * UserDetailsController
 */
@ControllerAdvice 
public class UserDetailsController {
    /**
     * Binds currently authenticated user's details to the model for all view templates
     * @param model used by thymeleaf views
     * @param currentUser authenticated principal or null
     */
    @ModelAttribute
    public void getCurrentUser(Model model, @AuthenticationPrincipal User currentUser) {
        // If user is authenticated, expose user details globally for view rendering
        if (currentUser != null) {
            model.addAttribute("userId", currentUser.getUserId());
            model.addAttribute("username", currentUser.getUsername());
        }
    }
}
