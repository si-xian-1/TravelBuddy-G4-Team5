package com.TravelBuddy.service;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.exception.UnauthorizedUserException;
import com.TravelBuddy.model.Flight;
import com.TravelBuddy.repository.FlightRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {
    @Mock
    private FlightRepository repository;

    @Mock
    private ItineraryService itineraries;

    @Mock
    private GdacsService gdacs;

    @InjectMocks
    private FlightService service;

    private Flight validFlight() {
        Flight item = new Flight();
        item.setAirline("Singapore Airlines");
        item.setFlightNumber("SQ632");
        item.setDepartureAirport("Changi");
        item.setArrivalAirport("Haneda");
        item.setDepartureLatitude(1.36);
        item.setDepartureLongitude(103.99);
        item.setArrivalLatitude(35.55);
        item.setArrivalLongitude(139.78);
        item.setDepartureTime(LocalDateTime.of(2027, 1, 10, 1, 0));
        item.setArrivalTime(LocalDateTime.of(2027, 1, 10, 9, 0));
        return item;
    }

    @Test
    @DisplayName("Successfully creates flight when valid inputs and owner are provided")
    void createFlight_validInput_setsItineraryAndSaves() {
        // Arrange
        Flight item = validFlight();
        when(repository.createFlight(10, item)).thenReturn(item);

        // Act
        Flight result = service.createFlight(10, 7, item);

        // Assert
        assertEquals(item, result);
        assertEquals(Integer.valueOf(10), item.getItineraryId());
        verify(itineraries).checkItineraryBelongToUser(10, 7);
        verify(repository).createFlight(10, item);
    }

    @Test
    @DisplayName("Throws UnauthorizedUserException when user does not own itinerary")
    void createFlight_notOwner_throwsWithoutSaving() {
        // Arrange
        Flight item = validFlight();
        doThrow(new UnauthorizedUserException("Access denied"))
                .when(itineraries).checkItineraryBelongToUser(10, 7);

        // Act & Assert
        assertThrows(UnauthorizedUserException.class,
                () -> service.createFlight(10, 7, item));
        verifyNoInteractions(repository, gdacs);
    }

    @Test
    @DisplayName("Throws InvalidInputsException when departure coordinates are (0,0)")
    void createFlight_zeroDepartureCoordinates_throwsWithoutSaving() {
        // Arrange
        Flight item = validFlight();
        item.setDepartureLatitude(0.0);
        item.setDepartureLongitude(0.0);

        // Act & Assert
        assertThrows(InvalidInputsException.class,
                () -> service.createFlight(10, 7, item));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Throws InvalidInputsException when arrival coordinates are (0,0)")
    void createFlight_zeroArrivalCoordinates_throwsWithoutSaving() {
        // Arrange
        Flight item = validFlight();
        item.setArrivalLatitude(0.0);
        item.setArrivalLongitude(0.0);

        // Act & Assert
        assertThrows(InvalidInputsException.class,
                () -> service.createFlight(10, 7, item));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Populates disaster warning when nearby disaster is detected at arrival destination")
    void getAllFlights_addsDisasterWarning() {
        // Arrange
        Flight item = validFlight();
        when(repository.findAllFlightsByItineraryId(10)).thenReturn(List.of(item));
        when(gdacs.evaluateProximityAndDateFromDisasters(
                item.getArrivalLatitude(), item.getArrivalLongitude(),
                item.getDepartureTime(), item.getArrivalTime())).thenReturn("Nearby disaster");

        // Act
        List<Flight> result = service.getAllFlights(10, 7);

        // Assert
        assertEquals(1, result.size());
        assertEquals(item, result.get(0));
        assertEquals("Nearby disaster", result.get(0).getDisasterWarning());
        verify(itineraries).checkItineraryBelongToUser(10, 7);
        verify(gdacs).evaluateProximityAndDateFromDisasters(
                item.getArrivalLatitude(), item.getArrivalLongitude(),
                item.getDepartureTime(), item.getArrivalTime());
        verifyNoMoreInteractions(gdacs);
    }

    @Test
    @DisplayName("Does not evaluate disasters when itinerary has no flights")
    void getAllFlights_emptyList_doesNotEvaluateDisasters() {
        // Arrange
        when(repository.findAllFlightsByItineraryId(10)).thenReturn(List.of());

        // Act
        List<Flight> result = service.getAllFlights(10, 7);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(gdacs);
    }

    @ParameterizedTest(name = "Latitude {0}, Longitude {1}")
    @DisplayName("Throws InvalidInputsException when creating flight with coordinates outside valid geographic range")
    @CsvSource({
        "-90.001, 139.76",
        "90.001, 139.76",
        "35.68, -180.001",
        "35.68, 180.001"
    })
    void createFlight_coordinatesOutsideGeographicRange_throwsInvalidInputsException(
            double latitude, double longitude) {
        // Arrange
        Flight item = validFlight();
        item.setArrivalLatitude(latitude);
        item.setArrivalLongitude(longitude);

        // Act & Assert
        assertThrows(InvalidInputsException.class, () -> service.createFlight(10, 7, item));
        verify(repository, never()).createFlight(anyInt(), any());
    }

    @ParameterizedTest(name = "Latitude {0}, Longitude {1}")
    @DisplayName("Throws InvalidInputsException when updating flight with coordinates outside valid geographic range")
    @CsvSource({
        "-90.001, 139.76",
        "90.001, 139.76",
        "35.68, -180.001",
        "35.68, 180.001"
    })
    void updateFlight_coordinatesOutsideGeographicRange_throwsInvalidInputsException(
            double latitude, double longitude) {
        // Arrange
        Flight item = validFlight();
        item.setArrivalLatitude(latitude);
        item.setArrivalLongitude(longitude);

        // Act & Assert
        assertThrows(InvalidInputsException.class, () -> service.updateFlight(10, 7, 20, item));
        verify(repository, never()).updateFlight(anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("Sets friendly fallback warning when disaster monitoring service fails")
    void getAllFlights_disasterFailure_preservesDataAndReportsFailure() {
        // Arrange
        Flight item = validFlight();
        when(repository.findAllFlightsByItineraryId(10)).thenReturn(List.of(item));
        when(gdacs.evaluateProximityAndDateFromDisasters(item.getArrivalLatitude(), item.getArrivalLongitude(), item.getDepartureTime(), item.getArrivalTime()))
                .thenThrow(new IllegalStateException("Fixture disaster outage"));

        // Act
        List<Flight> result = assertDoesNotThrow(() -> service.getAllFlights(10, 7));

        // Assert
        assertEquals(1, result.size());
        assertEquals(item, result.get(0));
        assertEquals("Disaster monitoring is currently unavailable. We are doing our best to resolve this.", result.get(0).getDisasterWarning());
    }
}
