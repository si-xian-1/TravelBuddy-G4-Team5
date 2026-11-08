package com.TravelBuddy.controller.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.TravelBuddy.model.LocationResponse;
import com.TravelBuddy.service.LocationService;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationController locationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(locationController).build();
    }

    @Test
    @DisplayName("Searching for accommodations with a destination query returns matching hotel recommendations")
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
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].display_name").value("Hotel A"));

        verify(locationService).searchLocations("Paris", true);
    }

    @Test
    @DisplayName("Searching for places without specifying accommodation preference finds general travel destinations")
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
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].display_name").value("Museum"));

        verify(locationService).searchLocations("Singapore", false);
    }

    @Test
    @DisplayName("Searching without a destination keyword is rejected")
    void searchLocationsReturnsBadRequestWhenQueryParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/location/search"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    @DisplayName("Searching for an unrecognized location yields no matching places")
    void searchLocationsReturnsEmptyListWhenNoMatchesFound() throws Exception {
        when(locationService.searchLocations("NonExistent", false)).thenReturn(List.of());

        mockMvc.perform(get("/api/location/search")
                        .param("query", "NonExistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(locationService).searchLocations("NonExistent", false);
    }
}
