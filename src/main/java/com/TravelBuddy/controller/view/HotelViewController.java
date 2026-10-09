
package com.TravelBuddy.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HotelViewController {

    /**
     * Render the Add Hotel page for a selected itinerary.
     */
    @GetMapping("/itinerary/{itineraryId}/hotels/new")
    public String showAddHotelPage(
            @PathVariable int itineraryId,
            @RequestParam int userId,
            Model model) {

        model.addAttribute("itineraryId", itineraryId);
        model.addAttribute("userId", userId);

        return "add-hotel";
    }
}