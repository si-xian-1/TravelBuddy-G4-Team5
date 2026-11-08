package com.TravelBuddy.service;

import com.TravelBuddy.model.LocationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    @InjectMocks
    private LocationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "restClient", restClient);
    }

    private LocationResponse createLocation(String name, String type, String category) {
        LocationResponse location = new LocationResponse();
        location.setDisplayName(name);
        location.setType(type);
        location.setCategory(category);
        return location;
    }

    @Test
    @DisplayName("Returns empty list when no locations are found")
    void searchLocations_noResults_returnsEmptyList() {
        // Arrange
        when(restClient.get()
                .uri(ArgumentMatchers.<Function<UriBuilder, URI>>any())
                .retrieve()
                .body(LocationResponse[].class))
                .thenReturn(new LocationResponse[0]);

        // Act
        List<LocationResponse> results = service.searchLocations("unknown place", false);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Prioritizes accommodations at the top when searching for hotels")
    void searchLocations_hotelSearch_prioritizesAccommodations() {
        // Arrange
        LocationResponse museum = createLocation("Museum", "museum", "historic");
        LocationResponse hotel = createLocation("Hotel A", "hotel", "tourism");
        when(restClient.get()
                .uri(ArgumentMatchers.<Function<UriBuilder, URI>>any())
                .retrieve()
                .body(LocationResponse[].class))
                .thenReturn(new LocationResponse[]{museum, hotel});

        // Act
        List<LocationResponse> results = service.searchLocations("hotel", true);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Hotel A", results.get(0).getDisplayName());
        assertEquals("Museum", results.get(1).getDisplayName());
    }

    @Test
    @DisplayName("Preserves original search order when hotel search is false")
    void searchLocations_regularSearch_preservesOriginalOrder() {
        // Arrange
        LocationResponse museum = createLocation("Museum", "museum", "historic");
        LocationResponse hotel = createLocation("Hotel A", "hotel", "tourism");
        when(restClient.get()
                .uri(ArgumentMatchers.<Function<UriBuilder, URI>>any())
                .retrieve()
                .body(LocationResponse[].class))
                .thenReturn(new LocationResponse[]{museum, hotel});

        // Act
        List<LocationResponse> results = service.searchLocations("hotel", false);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Museum", results.get(0).getDisplayName());
        assertEquals("Hotel A", results.get(1).getDisplayName());
    }

    @Test
    @DisplayName("Returns empty list safely when external service returns null")
    void searchLocations_nullResponse_returnsEmptyList() {
        // Arrange
        when(restClient.get()
                .uri(ArgumentMatchers.<Function<UriBuilder, URI>>any())
                .retrieve()
                .body(LocationResponse[].class))
                .thenReturn(null);

        // Act
        List<LocationResponse> results = service.searchLocations("somewhere", false);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
