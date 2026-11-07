package com.TravelBuddy.service;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.exception.UnauthorizedUserException;
import com.TravelBuddy.model.*;
import com.TravelBuddy.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItineraryServiceTest {

    @Mock
    private ItineraryRepository repository;

    @Mock
    private TravelerRepository travelers;

    @Mock
    private GdacsService gdacs;

    @InjectMocks
    private ItineraryService service;

    private Itinerary validItinerary() {
        Itinerary itinerary = new Itinerary();
        itinerary.setItineraryName("Japan trip");
        itinerary.setStartDate(LocalDate.of(2027, 1, 10));
        itinerary.setEndDate(LocalDate.of(2027, 1, 15));
        return itinerary;
    }

    @Test
    @DisplayName("Successfully creates itinerary and associates it with the owner")
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
    @DisplayName("Throws InvalidInputsException when start date is after end date")
    void createItinerary_startAfterEnd_throwsWithoutSaving() {
        // Arrange
        Itinerary itinerary = validItinerary();
        itinerary.setStartDate(itinerary.getEndDate().plusDays(1));

        // Act & Assert
        assertThrows(InvalidInputsException.class,
                () -> service.createItinerary(itinerary, 7));
        verify(repository, never()).createItinerary(any());
    }

    @Test
    @DisplayName("Throws InvalidInputsException when user does not exist")
    void createItinerary_missingUser_throwsWithoutSaving() {
        // Arrange
        Itinerary itinerary = validItinerary();
        when(travelers.getUser(7)).thenReturn(null);

        // Act & Assert
        assertThrows(InvalidInputsException.class,
                () -> service.createItinerary(itinerary, 7));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Throws UnauthorizedUserException when user does not own itinerary to delete")
    void deleteItinerary_notOwner_throwsWithoutDeleting() {
        // Arrange
        when(repository.verifyItineraryAndUser(10, 7)).thenReturn(false);

        // Act & Assert
        assertThrows(UnauthorizedUserException.class,
                () -> service.deleteItinerary(10, 7));
        verify(repository, never()).deleteItinerary(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Populates disaster warnings for all hotels, attractions, and flights in selected itinerary")
    void getSelectedItinerary_addsWarningsToAllRelatedItems() {
        // Arrange
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

        // Act
        Itinerary result = service.getSelectedItinerary(10, 7);

        // Assert
        assertEquals(itinerary, result);
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
    @DisplayName("Does not evaluate disasters when itinerary has no related booking items")
    void getSelectedItinerary_emptyRelatedLists_doesNotEvaluateDisasters() {
        // Arrange
        Itinerary itinerary = validItinerary();
        when(repository.verifyItineraryAndUser(10, 7)).thenReturn(true);
        when(repository.findSelectedItineraryByUserId(10, 7)).thenReturn(itinerary);

        // Act
        Itinerary result = service.getSelectedItinerary(10, 7);

        // Assert
        assertEquals(itinerary, result);
        verifyNoInteractions(gdacs);
    }

    @Test
    @DisplayName("Allows creating itinerary where start date and end date are the same")
    void createItinerary_sameDate_isAllowed() {
        // Arrange
        Itinerary itinerary = validItinerary();
        itinerary.setEndDate(itinerary.getStartDate());
        when(travelers.getUser(7)).thenReturn(new User());
        when(repository.createItinerary(itinerary)).thenReturn(10);

        // Act
        int result = service.createItinerary(itinerary, 7);

        // Assert
        assertEquals(10, result);
        verify(repository).createItinerary(itinerary);
    }

    @Test
    @DisplayName("Throws InvalidInputsException when updating itinerary dates excludes existing hotel bookings")
    void updateItinerary_excludingExistingHotel_isRejectedWithoutSaving() {
        // Arrange
        Itinerary existing = validItinerary();
        Hotel hotel = new Hotel();
        hotel.setCheckIn(LocalDateTime.of(2027, 1, 10, 15, 0));
        hotel.setCheckOut(LocalDateTime.of(2027, 1, 15, 11, 0));
        existing.setHotels(List.of(hotel));

        when(repository.verifyItineraryAndUser(10, 7)).thenReturn(true);
        when(repository.findSelectedItineraryByUserId(10, 7)).thenReturn(existing);

        Itinerary proposed = validItinerary();
        proposed.setEndDate(LocalDate.of(2027, 1, 14));

        // Act & Assert
        assertThrows(InvalidInputsException.class, () -> service.updateItinerary(10, 7, proposed));
        verify(repository, never()).updateItinerary(anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("Throws InvalidInputsException when updating itinerary dates excludes existing flight bookings")
    void updateItinerary_excludingExistingFlight_isRejectedWithoutSaving() {
        // Arrange
        Itinerary existing = validItinerary();
        Flight flight = new Flight();
        flight.setDepartureTime(LocalDateTime.of(2027, 1, 10, 23, 0));
        flight.setArrivalTime(LocalDateTime.of(2027, 1, 15, 1, 0));
        existing.setFlights(List.of(flight));

        when(repository.verifyItineraryAndUser(10, 7)).thenReturn(true);
        when(repository.findSelectedItineraryByUserId(10, 7)).thenReturn(existing);

        Itinerary proposed = validItinerary();
        proposed.setStartDate(LocalDate.of(2027, 1, 11));

        // Act & Assert
        assertThrows(InvalidInputsException.class, () -> service.updateItinerary(10, 7, proposed));
        verify(repository, never()).updateItinerary(anyInt(), anyInt(), any());
    }
}
