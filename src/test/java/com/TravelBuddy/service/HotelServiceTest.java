package com.TravelBuddy.service;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.exception.UnauthorizedUserException;
import com.TravelBuddy.model.Hotel;
import com.TravelBuddy.repository.HotelRepository;
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
    @DisplayName("Successfully creates hotel when valid inputs and owner are provided")
    void createHotel_validInput_setsItineraryAndSaves() {
        // Arrange
        Hotel item = validHotel();
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
    @DisplayName("Throws UnauthorizedUserException when user does not own itinerary")
    void createHotel_notOwner_throwsWithoutSaving() {
        // Arrange
        Hotel item = validHotel();
        doThrow(new UnauthorizedUserException("Access denied"))
                .when(itineraries).checkItineraryBelongToUser(10, 7);

        // Act & Assert
        assertThrows(UnauthorizedUserException.class,
                () -> service.createHotel(10, 7, item));
        verifyNoInteractions(repository, gdacs);
    }

    @Test
    @DisplayName("Throws InvalidInputsException when check-in is after check-out")
    void createHotel_checkInAfterCheckOut_throwsWithoutSaving() {
        // Arrange
        Hotel item = validHotel();
        item.setCheckIn(item.getCheckOut().plusHours(1));

        // Act & Assert
        assertThrows(InvalidInputsException.class, () -> service.createHotel(10, 7, item));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Throws InvalidInputsException when coordinates are (0,0)")
    void createHotel_zeroCoordinates_throwsWithoutSaving() {
        // Arrange
        Hotel item = validHotel();
        item.setLatitude(0.0);
        item.setLongitude(0.0);

        // Act & Assert
        assertThrows(InvalidInputsException.class,
                () -> service.createHotel(10, 7, item));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Populates disaster warning when nearby disaster is detected")
    void getAllHotel_addsDisasterWarning() {
        // Arrange
        Hotel item = validHotel();
        when(repository.findAllHotelsByItineraryId(10)).thenReturn(List.of(item));
        when(gdacs.evaluateProximityAndDateFromDisasters(
                item.getLatitude(), item.getLongitude(),
                item.getCheckIn(), item.getCheckOut())).thenReturn("Nearby disaster");

        // Act
        List<Hotel> result = service.getAllHotel(10, 7);

        // Assert
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
    @DisplayName("Does not evaluate disasters when itinerary has no hotels")
    void getAllHotel_emptyList_doesNotEvaluateDisasters() {
        // Arrange
        when(repository.findAllHotelsByItineraryId(10)).thenReturn(List.of());

        // Act
        List<Hotel> result = service.getAllHotel(10, 7);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(gdacs);
    }

    @Test
    @DisplayName("Throws InvalidInputsException when check-in and check-out are on same date even if times differ")
    void createHotel_sameDateEvenWithLaterTime_isRejectedWithoutSaving() {
        // Arrange
        Hotel hotel = validHotel();
        hotel.setCheckOut(hotel.getCheckIn().plusHours(2));

        // Act & Assert
        assertThrows(InvalidInputsException.class, () -> service.createHotel(10, 7, hotel));
        verify(repository, never()).createHotel(anyInt(), any());
    }

    @ParameterizedTest(name = "Latitude {0}, Longitude {1}")
    @DisplayName("Throws InvalidInputsException when creating hotel with coordinates outside valid geographic range")
    @CsvSource({
        "-90.001, 139.76",
        "90.001, 139.76",
        "35.68, -180.001",
        "35.68, 180.001"
    })
    void createHotel_coordinatesOutsideGeographicRange_throwsInvalidInputsException(
            double latitude, double longitude) {
        // Arrange
        Hotel hotel = validHotel();
        hotel.setLatitude(latitude);
        hotel.setLongitude(longitude);

        // Act & Assert
        assertThrows(InvalidInputsException.class, () -> service.createHotel(10, 7, hotel));
        verify(repository, never()).createHotel(anyInt(), any());
    }

    @ParameterizedTest(name = "Latitude {0}, Longitude {1}")
    @DisplayName("Throws InvalidInputsException when updating hotel with coordinates outside valid geographic range")
    @CsvSource({
        "-90.001, 139.76",
        "90.001, 139.76",
        "35.68, -180.001",
        "35.68, 180.001"
    })
    void updateHotel_coordinatesOutsideGeographicRange_throwsInvalidInputsException(
            double latitude, double longitude) {
        // Arrange
        Hotel hotel = validHotel();
        hotel.setLatitude(latitude);
        hotel.setLongitude(longitude);

        // Act & Assert
        assertThrows(InvalidInputsException.class, () -> service.updateHotel(10, 7, 20, hotel));
        verify(repository, never()).updateHotel(anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("Sets friendly fallback warning when disaster monitoring service fails")
    void getAllHotel_disasterFailure_preservesDataAndReportsFailure() {
        // Arrange
        Hotel hotel = validHotel();
        when(repository.findAllHotelsByItineraryId(10)).thenReturn(List.of(hotel));
        when(gdacs.evaluateProximityAndDateFromDisasters(
                hotel.getLatitude(), hotel.getLongitude(), hotel.getCheckIn(), hotel.getCheckOut()))
                .thenThrow(new IllegalStateException("Fixture disaster outage"));

        // Act
        List<Hotel> result = assertDoesNotThrow(() -> service.getAllHotel(10, 7));

        // Assert
        assertEquals(1, result.size());
        assertEquals(hotel.getHotelName(), result.get(0).getHotelName());
        assertEquals("Disaster monitoring is currently unavailable. We are doing our best to resolve this.", result.get(0).getDisasterWarning());
    }
}
