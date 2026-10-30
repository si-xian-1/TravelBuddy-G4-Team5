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

class HotelServiceTest {
    private HotelRepository repository;
    private ItineraryService itineraries;
    private GdacsService gdacs;
    private HotelService service;

    @BeforeEach
    void setUp() {
        repository = mock(HotelRepository.class);
        itineraries = mock(ItineraryService.class);
        gdacs = mock(GdacsService.class);
        service = new HotelService(repository, itineraries, gdacs);
    }

    private Hotel validHotel() {
        Hotel item = new Hotel();
        item.setHotelName("Tokyo Hotel");
        item.setLatitude(35.68);
        item.setLongitude(139.76);
        item.setCheckIn(LocalDateTime.of(2027, 1, 10, 15, 0));
        item.setCheckOut(LocalDateTime.of(2027, 1, 12, 11, 0));
        return item;
    }

    @Test
    void createHotel_validInput_setsItineraryAndSaves() {
        // Arrange
        Hotel item = validHotel();
        // Mocked void ownership check succeeds by default.
        when(repository.createHotel(10, item)).thenReturn(item);

        // Act
        Hotel result = service.createHotel(10, 7, item);

        // Assert
        assertSame(item, result);
        assertEquals(Integer.valueOf(10), item.getItineraryId());
        verify(itineraries).checkItineraryBelongToUser(10, 7);
        verify(repository).createHotel(10, item);
    }

    @Test
    void createHotel_notOwner_throwsWithoutSaving() {
        Hotel item = validHotel();
        doThrow(new UnauthorizedUserException("Access denied"))
                .when(itineraries).checkItineraryBelongToUser(10, 7);

        assertThrows(UnauthorizedUserException.class,
                () -> service.createHotel(10, 7, item));

        verifyNoInteractions(repository, gdacs);
    }

    @Test
    void createHotel_checkInAfterCheckOut_throwsWithoutSaving() {
        Hotel item = validHotel();
        item.setCheckIn(item.getCheckOut().plusHours(1));

        assertThrows(InvalidInputsException.class, () -> service.createHotel(10, 7, item));

        verifyNoInteractions(repository);
    }

    @Test
    void createHotel_zeroCoordinates_throwsWithoutSaving() {
        Hotel item = validHotel();
        // Mocked void ownership check succeeds by default.
        item.setLatitude(0.0);
        item.setLongitude(0.0);

        assertThrows(InvalidInputsException.class,
                () -> service.createHotel(10, 7, item));

        verifyNoInteractions(repository);
    }

    @Test
    void getAllHotel_addsDisasterWarning() {
        Hotel item = validHotel();
        // Mocked void ownership check succeeds by default.
        when(repository.findAllHotelsByItineraryId(10)).thenReturn(List.of(item));
        when(gdacs.evaluateProximityAndDateFromDisasters(
                item.getLatitude(), item.getLongitude(),
                item.getCheckIn(), item.getCheckOut())).thenReturn("Nearby disaster");

        List<Hotel> result = service.getAllHotel(10, 7);

        assertEquals(1, result.size());
        assertSame(item, result.get(0));
        assertEquals("Nearby disaster", result.get(0).getDisasterWarning());
        verify(itineraries).checkItineraryBelongToUser(10, 7);
        verify(gdacs).evaluateProximityAndDateFromDisasters(
                item.getLatitude(), item.getLongitude(),
                item.getCheckIn(), item.getCheckOut());
        verifyNoMoreInteractions(gdacs);
    }

    @Test
    void getAllHotel_emptyList_doesNotEvaluateDisasters() {
        // Mocked void ownership check succeeds by default.
        when(repository.findAllHotelsByItineraryId(10)).thenReturn(List.of());

        assertTrue(service.getAllHotel(10, 7).isEmpty());

        verifyNoInteractions(gdacs);
    }
}
