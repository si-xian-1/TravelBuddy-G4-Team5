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

class ItineraryServiceTest {
    private ItineraryRepository repository;
    private TravelerRepository travelers;
    private GdacsService gdacs;
    private ItineraryService service;

    @BeforeEach
    void setUp() {
        repository = mock(ItineraryRepository.class);
        travelers = mock(TravelerRepository.class);
        gdacs = mock(GdacsService.class);
        service = new ItineraryService(repository, travelers, gdacs);
    }

    private Itinerary validItinerary() {
        Itinerary itinerary = new Itinerary();
        itinerary.setItineraryName("Japan trip");
        itinerary.setStartDate(LocalDate.of(2027, 1, 10));
        itinerary.setEndDate(LocalDate.of(2027, 1, 15));
        return itinerary;
    }

    @Test
    void createItinerary_validInput_returnsIdAndSetsOwner() {
        // Arrange
        Itinerary itinerary = validItinerary();
        when(travelers.getUser(7)).thenReturn(new User());
        when(repository.createItinerary(itinerary)).thenReturn(10);

        // Act
        int result = service.createItinerary(itinerary, 7);

        // Assert
        assertEquals(10, result);
        assertEquals(Integer.valueOf(7), itinerary.getUserId());
        verify(repository).createItinerary(itinerary);
    }

    @Test
    void createItinerary_startAfterEnd_throwsWithoutSaving() {
        Itinerary itinerary = validItinerary();
        itinerary.setStartDate(itinerary.getEndDate().plusDays(1));

        InvalidInputsException exception = assertThrows(InvalidInputsException.class,
                () -> service.createItinerary(itinerary, 7));

        assertEquals("Start date cannot be after end date.", exception.getMessage());
        verifyNoInteractions(repository, travelers);
    }

    @Test
    void createItinerary_missingUser_throwsWithoutSaving() {
        Itinerary itinerary = validItinerary();
        when(travelers.getUser(7)).thenReturn(null);

        assertThrows(InvalidInputsException.class, () -> service.createItinerary(itinerary, 7));

        verifyNoInteractions(repository);
    }

    @Test
    void deleteItinerary_notOwner_throwsWithoutDeleting() {
        when(repository.verifyItineraryAndUser(10, 7)).thenReturn(false);

        assertThrows(UnauthorizedUserException.class, () -> service.deleteItinerary(10, 7));

        verify(repository, never()).deleteItinerary(anyInt(), anyInt());
    }

    @Test
    void getSelectedItinerary_addsWarningsToAllRelatedItems() {
        Itinerary itinerary = validItinerary();
        Hotel hotel = new Hotel();
        hotel.setLatitude(35.68);
        hotel.setLongitude(139.76);
        hotel.setCheckIn(LocalDateTime.of(2027, 1, 10, 15, 0));
        hotel.setCheckOut(LocalDateTime.of(2027, 1, 12, 11, 0));
        Attraction attraction = new Attraction();
        attraction.setLatitude(35.71);
        attraction.setLongitude(139.81);
        attraction.setVisitDate(LocalDateTime.of(2027, 1, 11, 10, 0));
        Flight flight = new Flight();
        flight.setDepartureLatitude(1.36);
        flight.setDepartureLongitude(103.99);
        flight.setArrivalLatitude(35.55);
        flight.setArrivalLongitude(139.78);
        flight.setDepartureTime(LocalDateTime.of(2027, 1, 10, 1, 0));
        flight.setArrivalTime(LocalDateTime.of(2027, 1, 10, 9, 0));
        itinerary.setHotels(List.of(hotel));
        itinerary.setAttractions(List.of(attraction));
        itinerary.setFlights(List.of(flight));
        when(repository.verifyItineraryAndUser(10, 7)).thenReturn(true);
        when(repository.findSelectedItineraryByUserId(10, 7)).thenReturn(itinerary);
        when(gdacs.evaluateProximityAndDateFromDisasters(35.68, 139.76,
                hotel.getCheckIn(), hotel.getCheckOut())).thenReturn("Hotel warning");
        when(gdacs.evaluateProximityAndDateFromDisasters(35.71, 139.81,
                attraction.getVisitDate(), attraction.getVisitDate())).thenReturn("Attraction warning");
        when(gdacs.evaluateProximityAndDateFromDisasters(35.55, 139.78,
                flight.getDepartureTime(), flight.getArrivalTime())).thenReturn("Flight warning");

        assertSame(itinerary, service.getSelectedItinerary(10, 7));

        assertEquals("Hotel warning", hotel.getDisasterWarning());
        assertEquals("Attraction warning", attraction.getDisasterWarning());
        assertEquals("Flight warning", flight.getDisasterWarning());
        verify(gdacs).evaluateProximityAndDateFromDisasters(35.68, 139.76,
                hotel.getCheckIn(), hotel.getCheckOut());
        verify(gdacs).evaluateProximityAndDateFromDisasters(35.71, 139.81,
                attraction.getVisitDate(), attraction.getVisitDate());
        verify(gdacs).evaluateProximityAndDateFromDisasters(35.55, 139.78,
                flight.getDepartureTime(), flight.getArrivalTime());
        verifyNoMoreInteractions(gdacs);
    }

    @Test
    void getSelectedItinerary_emptyRelatedLists_doesNotEvaluateDisasters() {
        Itinerary itinerary = validItinerary();
        when(repository.verifyItineraryAndUser(10, 7)).thenReturn(true);
        when(repository.findSelectedItineraryByUserId(10, 7)).thenReturn(itinerary);

        assertSame(itinerary, service.getSelectedItinerary(10, 7));

        verifyNoInteractions(gdacs);
    }
}
