package com.TravelBuddy.service;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.exception.UnauthorizedUserException;
import com.TravelBuddy.model.*;
import com.TravelBuddy.repository.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {
    @Mock
    private HotelRepository repository;
    @Mock
    private ItineraryService itineraries;
    @Mock
    private GdacsService gdacs;
    @InjectMocks
    private HotelService service;

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
        assertEquals(item, result);
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
        assertEquals(item, result.get(0));
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

    @Test
    void createHotel_sameDateEvenWithLaterTime_isRejectedWithoutSaving() {
        Hotel hotel = validHotel();
        hotel.setCheckOut(hotel.getCheckIn().plusHours(2));

        assertThrows(InvalidInputsException.class, () -> service.createHotel(10, 7, hotel));

        verify(repository, never()).createHotel(anyInt(), any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "-90.001,139.76,false", "90.001,139.76,false",
        "35.68,-180.001,false", "35.68,180.001,false",
        "-90.001,139.76,true", "90.001,139.76,true",
        "35.68,-180.001,true", "35.68,180.001,true"
    })
    void coordinates_outsideGeographicRange_rejectedOnCreateAndUpdate(
            double latitude, double longitude, boolean updating) {
        Hotel hotel = validHotel();
        hotel.setLatitude(latitude);
        hotel.setLongitude(longitude);

        assertThrows(InvalidInputsException.class, () -> {
            if (updating) service.updateHotel(10, 7, 20, hotel);
            else service.createHotel(10, 7, hotel);
        });

        verify(repository, never()).createHotel(anyInt(), any());
        verify(repository, never()).updateHotel(anyInt(), anyInt(), any());
    }

    @Test
    void getAllHotel_disasterFailure_preservesDataAndReportsFailure() {
        Hotel hotel = validHotel();
        when(repository.findAllHotelsByItineraryId(10)).thenReturn(List.of(hotel));
        when(gdacs.evaluateProximityAndDateFromDisasters(
                hotel.getLatitude(), hotel.getLongitude(), hotel.getCheckIn(), hotel.getCheckOut()))
                .thenThrow(new IllegalStateException("Fixture disaster outage"));

        List<Hotel> result = assertDoesNotThrow(() -> service.getAllHotel(10, 7));

        assertEquals(1, result.size());
        assertEquals(hotel.getHotelName(), result.get(0).getHotelName());
        assertEquals("disaster-service failure", result.get(0).getDisasterWarning());
    }
}
