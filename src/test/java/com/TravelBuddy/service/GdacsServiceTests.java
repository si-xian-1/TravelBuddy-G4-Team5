package com.TravelBuddy.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import com.TravelBuddy.model.Disaster;
import com.TravelBuddy.model.Disaster.DisasterDetails;
import com.TravelBuddy.model.Disaster.DisasterEvents;
import com.TravelBuddy.model.Disaster.LocationCoordinates;
import com.TravelBuddy.model.ProximitySummary;
import com.TravelBuddy.model.enums.EventType;

class GdacsServiceTests {

    @Test
    void calculateProximitySummaryMarksUserAsInDangerWhenDisasterIsNearby() throws Exception {
        // Arrange
        GdacsService gdacsService = new GdacsService(RestClient.builder(), mock(ResourceLoader.class));
        DisasterEvents disaster = createDisaster(
                0.0,
                0.0,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 10, 0, 0)
        );
        setField(gdacsService, "activeDisasters", List.of(disaster));

        // Act
        ProximitySummary summary = gdacsService.calculateProximitySummary(0.0, 0.0);

        // Assert
        assertTrue(summary.isInDangerZone());
        assertTrue(summary.getNearestDistanceKm() <= 120.0);
        assertNotNull(summary.getNearestDisasterName());
    }

    @Test
    void evaluateProximityAndDateFromDisastersReturnsWarningWhenTripOverlapsDisasterWindow() throws Exception {
        // Arrange
        GdacsService gdacsService = new GdacsService(RestClient.builder(), mock(ResourceLoader.class));
        DisasterEvents disaster = createDisaster(
                0.0,
                0.0,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 10, 0, 0)
        );
        setField(gdacsService, "activeDisasters", List.of(disaster));

        // Act
        String warning = gdacsService.evaluateProximityAndDateFromDisasters(
                0.0,
                0.0,
                LocalDateTime.of(2025, 1, 5, 9, 0),
                LocalDateTime.of(2025, 1, 6, 9, 0)
        );

        // Assert
        assertNotNull(warning);
        assertTrue(warning.contains("WARNING"));
    }

        @Test
    void calculateProximitySummaryMarksFarAwayLocationAsSafe() throws Exception {
        // Arrange
        GdacsService gdacsService = new GdacsService(RestClient.builder(), mock(ResourceLoader.class));
        setField(gdacsService, "activeDisasters", List.of(
                createDisasterWithData(
                        new double[] { 0.0, 0.0 },
                        LocalDateTime.of(2025, 1, 1, 0, 0),
                        LocalDateTime.of(2025, 1, 10, 0, 0))));

        // Act
        ProximitySummary summary = gdacsService.calculateProximitySummary(10.0, 10.0);

        // Assert
        assertFalse(summary.isInDangerZone());
        assertTrue(summary.getNearestDistanceKm() > 120.0);
    }

    @Test
    void calculateProximitySummaryTreatsDangerRadiusBoundaryAsDangerous() throws Exception {
        // Arrange
        GdacsService gdacsService = new GdacsService(RestClient.builder(), mock(ResourceLoader.class));
        setField(gdacsService, "activeDisasters", List.of(
                createDisasterWithData(
                        new double[] { 0.0, 0.0 },
                        LocalDateTime.of(2025, 1, 1, 0, 0),
                        LocalDateTime.of(2025, 1, 10, 0, 0))));

        // Act
        ProximitySummary summary = gdacsService.calculateProximitySummary(120.0 / 111.0, 0.0);

        // Assert
        assertEquals(120.0, summary.getNearestDistanceKm());
        assertTrue(summary.isInDangerZone());
    }

    @Test
    void calculateProximitySummaryReturnsSafeWhenThereAreNoDisasters() throws Exception {
        // Arrange
        GdacsService gdacsService = new GdacsService(RestClient.builder(), mock(ResourceLoader.class));
        setField(gdacsService, "activeDisasters", List.of());

        // Act
        ProximitySummary summary = gdacsService.calculateProximitySummary(0.0, 0.0);

        // Assert
        assertFalse(summary.isInDangerZone());
        assertNull(summary.getNearestDistanceKm());
        assertEquals("None", summary.getNearestDisasterName());
    }

    @Test
    void calculateProximitySummarySkipsDisasterWithMissingCoordinates() throws Exception {
        // Arrange
        GdacsService gdacsService = new GdacsService(RestClient.builder(), mock(ResourceLoader.class));
        setField(gdacsService, "activeDisasters", List.of(
                createDisasterWithData(
                        null,
                        LocalDateTime.of(2025, 1, 1, 0, 0),
                        LocalDateTime.of(2025, 1, 10, 0, 0))));

        // Act
        ProximitySummary summary = gdacsService.calculateProximitySummary(0.0, 0.0);

        // Assert
        assertFalse(summary.isInDangerZone());
        assertNull(summary.getNearestDistanceKm());
        assertEquals("None", summary.getNearestDisasterName());
    }

    @Test
    void evaluateProximityAndDateSkipsDisasterWithMissingDates() throws Exception {
        // Arrange
        GdacsService gdacsService = new GdacsService(RestClient.builder(), mock(ResourceLoader.class));
        setField(gdacsService, "activeDisasters", List.of(
                createDisasterWithData(new double[] { 0.0, 0.0 }, null, null)));

        // Act
        String warning = gdacsService.evaluateProximityAndDateFromDisasters(
                0.0,
                0.0,
                LocalDateTime.of(2025, 1, 5, 0, 0),
                LocalDateTime.of(2025, 1, 6, 0, 0));

        // Assert
        assertNull(warning);
    }

    @Test
    void evaluateProximityAndDateReturnsNoWarningForTripBeforeDisaster() throws Exception {
        // Arrange
        GdacsService gdacsService = new GdacsService(RestClient.builder(), mock(ResourceLoader.class));
        setField(gdacsService, "activeDisasters", List.of(
                createDisasterWithData(
                        new double[] { 0.0, 0.0 },
                        LocalDateTime.of(2025, 1, 1, 0, 0),
                        LocalDateTime.of(2025, 1, 10, 0, 0))));

        // Act
        String warning = gdacsService.evaluateProximityAndDateFromDisasters(
                0.0,
                0.0,
                LocalDateTime.of(2024, 12, 1, 0, 0),
                LocalDateTime.of(2024, 12, 10, 0, 0));

        // Assert
        assertNull(warning);
    }

    @Test
    void evaluateProximityAndDateReturnsNoWarningAfterRecoveryWindow() throws Exception {
        // Arrange
        GdacsService gdacsService = new GdacsService(RestClient.builder(), mock(ResourceLoader.class));
        setField(gdacsService, "activeDisasters", List.of(
                createDisasterWithData(
                        new double[] { 0.0, 0.0 },
                        LocalDateTime.of(2025, 1, 1, 0, 0),
                        LocalDateTime.of(2025, 1, 10, 0, 0))));

        // Act
        String warning = gdacsService.evaluateProximityAndDateFromDisasters(
                0.0,
                0.0,
                LocalDateTime.of(2025, 3, 1, 0, 0),
                LocalDateTime.of(2025, 3, 10, 0, 0));

        // Assert
        assertNull(warning);
    }

    @Test
    void calculateProximitySummarySelectsNearestDisaster() throws Exception {
        // Arrange
        GdacsService gdacsService = new GdacsService(RestClient.builder(), mock(ResourceLoader.class));
        DisasterEvents fartherDisaster = createDisasterWithData(
                new double[] { 0.9, 0.0 },
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 10, 0, 0));
        DisasterEvents nearerDisaster = createDisasterWithData(
                new double[] { 0.1, 0.0 },
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 10, 0, 0));
        setField(gdacsService, "activeDisasters", List.of(fartherDisaster, nearerDisaster));

        // Act
        ProximitySummary summary = gdacsService.calculateProximitySummary(0.0, 0.0);

        // Assert
        assertSame(nearerDisaster, summary.getNearestDisaster());
        assertEquals(11.0, summary.getNearestDistanceKm());
    }

    @Test
    void getDisasterEventsFallsBackToMockDataWhenApiReturnsNoEvents() throws Exception {
        // Arrange
        ApiTestContext context = createApiTestContext();
        context.server.expect(requestTo("https://www.gdacs.org/gdacsapi/api/Events/geteventlist/events4app"))
                .andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));

        // Act
        Disaster result = context.service.getDisasterEvents();

        // Assert
        assertEquals(1, result.getAllDisastersEvents().size());
        assertEquals(101L, result.getAllDisastersEvents().get(0).getDisasterDetails().getDisasterId());
        context.server.verify();
    }

    @Test
    void getDisasterEventsFallsBackToMockDataWhenApiFails() throws Exception {
        // Arrange
        ApiTestContext context = createApiTestContext();
        context.server.expect(requestTo("https://www.gdacs.org/gdacsapi/api/Events/geteventlist/events4app"))
                .andRespond(withServerError());

        // Act
        Disaster result = context.service.getDisasterEvents();

        // Assert
        assertEquals(1, result.getAllDisastersEvents().size());
        assertEquals(101L, result.getAllDisastersEvents().get(0).getDisasterDetails().getDisasterId());
        context.server.verify();
    }

    
    private static DisasterEvents createDisaster(
            double longitude,
            double latitude,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) throws Exception {
        // Arrange
        DisasterEvents disaster = new DisasterEvents();

        LocationCoordinates coordinates = new LocationCoordinates();
        setField(coordinates, "coordinates", new double[] { longitude, latitude });

        DisasterDetails details = new DisasterDetails();
        setField(details, "disasterType", EventType.TC);
        setField(details, "disasterId", 100L);
        setField(details, "alertLevel", "Orange");
        setField(details, "disasterCountryLocation", "Japan");
        setField(details, "disasterStartDate", startDate);
        setField(details, "disasterEndDate", endDate);

        setField(disaster, "locationCoordinates", coordinates);
        setField(disaster, "disasterDetails", details);

        return disaster;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static DisasterEvents createDisasterWithData(
            double[] coordinates,
            LocalDateTime startDate,
            LocalDateTime endDate) throws Exception {
        DisasterEvents disaster = new DisasterEvents();

        LocationCoordinates location = new LocationCoordinates();
        setField(location, "coordinates", coordinates);

        DisasterDetails details = new DisasterDetails();
        setField(details, "disasterType", EventType.TC);
        setField(details, "disasterId", 101L);
        setField(details, "alertLevel", "Orange");
        setField(details, "disasterCountryLocation", "Japan");
        setField(details, "disasterStartDate", startDate);
        setField(details, "disasterEndDate", endDate);

        setField(disaster, "locationCoordinates", location);
        setField(disaster, "disasterDetails", details);
        return disaster;
    }

    private static ApiTestContext createApiTestContext() throws Exception {
        String fallbackJson = """
                {
                  "features": [{
                    "geometry": { "coordinates": [1.0, 2.0] },
                    "properties": {
                      "eventtype": "TC",
                      "eventid": 101,
                      "alertlevel": "Orange",
                      "country": "Japan",
                      "fromdate": "2025-01-01T00:00:00",
                      "todate": "2025-01-10T00:00:00"
                    }
                  }]
                }
                """;

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        when(resourceLoader.getResource("classpath:demo.json"))
                .thenReturn(new ByteArrayResource(fallbackJson.getBytes(StandardCharsets.UTF_8)));

        GdacsService service = new GdacsService(builder, resourceLoader);
        setField(service, "demoDisasterData", "classpath:demo.json");
        return new ApiTestContext(service, server);
    }

    private static class ApiTestContext {
        private final GdacsService service;
        private final MockRestServiceServer server;

        private ApiTestContext(GdacsService service, MockRestServiceServer server) {
            this.service = service;
            this.server = server;
        }
    }
}