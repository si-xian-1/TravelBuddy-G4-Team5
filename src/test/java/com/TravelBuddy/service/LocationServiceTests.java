package com.TravelBuddy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.TravelBuddy.model.LocationResponse;

class LocationServiceTests {

    @Test
    void searchLocationsReturnsEmptyListWhenNoResultsAreFound() throws Exception {
        // Arrange
        LocationService locationService = new LocationService();
        setField(locationService, "restClient", mockRestClient(new LocationResponse[0]));

        // Act
        List<LocationResponse> results = locationService.searchLocations("unknown place", false);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void searchLocationsPrioritizesAccommodationResultsWhenHotelSearchIsRequested() throws Exception {
        // Arrange
        LocationService locationService = new LocationService();

        LocationResponse firstResult = new LocationResponse();
        firstResult.setDisplayName("Museum");
        firstResult.setType("museum");
        firstResult.setCategory("historic");

        LocationResponse secondResult = new LocationResponse();
        secondResult.setDisplayName("Hotel A");
        secondResult.setType("hotel");
        secondResult.setCategory("tourism");

        setField(locationService, "restClient",
                mockRestClient(new LocationResponse[] { firstResult, secondResult }));

        // Act
        List<LocationResponse> results = locationService.searchLocations("hotel", true);

        // Assert
        assertEquals(2, results.size());
        assertEquals("Hotel A", results.get(0).getDisplayName());
        assertEquals("Museum", results.get(1).getDisplayName());
    }

    @Test
    void searchLocationsKeepsOriginalOrderWhenHotelSearchIsNotRequested() throws Exception {
        // Arrange
        LocationService locationService = new LocationService();

        LocationResponse firstResult = new LocationResponse();
        firstResult.setDisplayName("Museum");
        firstResult.setType("museum");
        firstResult.setCategory("historic");

        LocationResponse secondResult = new LocationResponse();
        secondResult.setDisplayName("Hotel A");
        secondResult.setType("hotel");
        secondResult.setCategory("tourism");

        setField(locationService, "restClient",
                mockRestClient(new LocationResponse[] { firstResult, secondResult }));

        // Act
        List<LocationResponse> results = locationService.searchLocations("hotel", false);

        // Assert
        assertEquals(2, results.size());
        assertEquals("Museum", results.get(0).getDisplayName());
        assertEquals("Hotel A", results.get(1).getDisplayName());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static RestClient mockRestClient(LocationResponse[] results) {
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(LocationResponse[].class)).thenReturn(results);

        return mockRestClient;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}