package com.TravelBuddy.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardViewController {
    /**
     * Render view dashboard page
     */
    @GetMapping("/home")
    public String Dashboard() {
        return "index";
    }

    /**
     * Render view all itineraries page
     */
    @GetMapping("/allItineraries")
    public String AllItineraries() {
        return "itineraryOverview";
    }

    /**
     * Render profile page
     */
    @GetMapping("/myProfile")
    public String Profile() {
        return "myProfile";
    }
}
