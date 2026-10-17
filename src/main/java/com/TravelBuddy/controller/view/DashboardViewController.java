package com.TravelBuddy.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DashboardViewController {
    /**
     * Render view dashboard page
     */
    @GetMapping("/")
    public String Dashboard() {
        return "index";
    }

    /**
     * Render view all itineraries page
     */
    @GetMapping("/allItineraries")
    public String allItineraries() {
        return "itineraryOverview";
    }

    /**
     * Render detailed itinerary page and pass in itineraryId for retrival of itinerary details
     */
    @GetMapping("/itinerary/view/{itineraryId}")
    public String detailedItinerary(@PathVariable("itineraryId") Integer itineraryId, Model model) {
        model.addAttribute("itineraryId", itineraryId);
        return "detailedItinerary";
    }

    /**
     * Render the form for adding an attraction to an itinerary
     */
    @GetMapping("/itinerary/view/{itineraryId}/attractions/new")
    public String newAttraction(@PathVariable("itineraryId") Integer itineraryId, Model model) {
        model.addAttribute("itineraryId", itineraryId);
        return "newAttraction";
    }

    /**
     * Render profile page
     */
    @GetMapping("/myProfile")
    public String Profile() {
        return "myProfile";
    }
}
