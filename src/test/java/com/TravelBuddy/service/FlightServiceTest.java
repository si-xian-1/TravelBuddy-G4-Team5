package com.TravelBuddy.service;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.exception.UnauthorizedUserException;
import com.TravelBuddy.model.*;
import com.TravelBuddy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlightServiceTest {
    private FlightRepository repository;
    private ItineraryService itineraries;
    private GdacsService gdacs;
    private FlightService service;

    @BeforeEach
    void setUp() {
        repository = mock(FlightRepository.class);
        itineraries = mock(ItineraryService.class);
        gdacs = mock(GdacsService.class);
        service = new FlightService(repository, itineraries, gdacs);
    }

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
    void createFlight_validInput_setsItineraryAndSaves() {
        // Arrange
        Flight item = validFlight();
        // Mocked void ownership check succeeds by default.
        when(repository.createFlight(10, item)).thenReturn(item);

        // Act
        Flight result = service.createFlight(10, 7, item);

        // Assert
        assertSame(item, result);
        assertEquals(Integer.valueOf(10), item.getItineraryId());
        verify(itineraries).checkItineraryBelongToUser(10, 7);
        verify(repository).createFlight(10, item);
    }

    @Test
    void createFlight_notOwner_throwsWithoutSaving() {
        Flight item = validFlight();
        doThrow(new UnauthorizedUserException("Access denied"))
                .when(itineraries).checkItineraryBelongToUser(10, 7);

        assertThrows(UnauthorizedUserException.class,
                () -> service.createFlight(10, 7, item));

        verifyNoInteractions(repository, gdacs);
    }

    @Test
    void createFlight_zeroDepartureCoordinates_throwsWithoutSaving() {
        Flight item = validFlight();
        // Mocked void ownership check succeeds by default.
        item.setDepartureLatitude(0.0);
        item.setDepartureLongitude(0.0);

        assertThrows(InvalidInputsException.class,
                () -> service.createFlight(10, 7, item));

        verifyNoInteractions(repository);
    }

    @Test
    void createFlight_zeroArrivalCoordinates_throwsWithoutSaving() {
        Flight item = validFlight();
        // Mocked void ownership check succeeds by default.
        item.setArrivalLatitude(0.0);
        item.setArrivalLongitude(0.0);

        assertThrows(InvalidInputsException.class,
                () -> service.createFlight(10, 7, item));

        verifyNoInteractions(repository);
    }

    @Test
    void getSelectedFlight_missingFlight_throws() {
        when(repository.findSelectedFlightByItineraryId(20, 10)).thenReturn(null);

        InvalidInputsException exception = assertThrows(InvalidInputsException.class,
                () -> service.getSelectedFlight(10, 7, 20));

        assertEquals("Flight not found in this itinerary.", exception.getMessage());
        verify(itineraries).checkItineraryBelongToUser(10, 7);
        verify(repository).findSelectedFlightByItineraryId(20, 10);
    }

    @Test
    void getAllFlights_addsDisasterWarning() {
        Flight item = validFlight();
        // Mocked void ownership check succeeds by default.
        when(repository.findAllFlightsByItineraryId(10)).thenReturn(List.of(item));
        when(gdacs.evaluateProximityAndDateFromDisasters(
                item.getArrivalLatitude(), item.getArrivalLongitude(),
                item.getDepartureTime(), item.getArrivalTime())).thenReturn("Nearby disaster");

        List<Flight> result = service.getAllFlights(10, 7);

        assertEquals(1, result.size());
        assertSame(item, result.get(0));
        assertEquals("Nearby disaster", result.get(0).getDisasterWarning());
        verify(itineraries).checkItineraryBelongToUser(10, 7);
        verify(gdacs).evaluateProximityAndDateFromDisasters(
                item.getArrivalLatitude(), item.getArrivalLongitude(),
                item.getDepartureTime(), item.getArrivalTime());
        verifyNoMoreInteractions(gdacs);
    }

    @Test
    void getAllFlights_emptyList_doesNotEvaluateDisasters() {
        // Mocked void ownership check succeeds by default.
        when(repository.findAllFlightsByItineraryId(10)).thenReturn(List.of());

        assertTrue(service.getAllFlights(10, 7).isEmpty());

        verifyNoInteractions(gdacs);
    }
}
