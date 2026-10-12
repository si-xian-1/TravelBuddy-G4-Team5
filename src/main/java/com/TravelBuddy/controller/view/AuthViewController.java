package com.TravelBuddy.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthViewController {
    /**
     * Render the login page aka landing page
     */
    @GetMapping("/login")
    public String Login() {
        return "login";
    }

    /**
     * Render sign up page
     */
    @GetMapping("/signup")
    public String SignUp() {
        return "signup";
    }

}
