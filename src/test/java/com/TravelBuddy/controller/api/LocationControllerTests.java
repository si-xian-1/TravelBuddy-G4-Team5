package com.TravelBuddy.controller.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.TravelBuddy.model.LocationResponse;
import com.TravelBuddy.service.LocationService;

class LocationControllerTests {

    private MockMvc mockMvc;
    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = mock(LocationService.class);
        LocationController controller = new LocationController(locationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void searchLocationsReturnsResultsWhenQueryIsProvided() throws Exception {
        // Arrange
        LocationResponse hotel = new LocationResponse();
        hotel.setDisplayName("Hotel A");
        hotel.setType("hotel");
        hotel.setCategory("tourism");

        when(locationService.searchLocations("Paris", true)).thenReturn(List.of(hotel));

        // Act
        mockMvc.perform(get("/api/location/search")
                        .param("query", "Paris")
                        .param("isHotel", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].display_name").value("Hotel A"));

        // Assert
        verify(locationService).searchLocations("Paris", true);
    }

    @Test
    void searchLocationsUsesDefaultHotelFlagWhenParameterIsMissing() throws Exception {
        // Arrange
        LocationResponse result = new LocationResponse();
        result.setDisplayName("Museum");
        result.setType("museum");
        result.setCategory("historic");

        when(locationService.searchLocations("Singapore", false)).thenReturn(List.of(result));

        // Act
        mockMvc.perform(get("/api/location/search")
                        .param("query", "Singapore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].display_name").value("Museum"));

        // Assert
        verify(locationService).searchLocations("Singapore", false);
    }
}