package com.TravelBuddy.service;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.exception.UnauthorizedUserException;
import com.TravelBuddy.model.Attraction;
import com.TravelBuddy.repository.AttractionRepository;
import com.TravelBuddy.repository.ItineraryRepository;
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
class AttractionServiceTest {
    @Mock
    private AttractionRepository repository;
    
    @Mock
    private ItineraryRepository itineraries;

    @Mock
    private GdacsService gdacs;

    @InjectMocks
    private AttractionService service;

    private Attraction validAttraction() {
        Attraction item = new Attraction();
        item.setAttractionName("Tokyo Skytree");
        item.setLatitude(35.71);
        item.setLongitude(139.81);
        item.setVisitDate(LocalDateTime.of(2027, 1, 11, 10, 0));
        return item;
    }

    @Test
    @DisplayName("Successfully creates attraction when valid inputs and owner are provided")
    void createAttraction_validInput_setsItineraryAndSaves() {
        // Arrange
        Attraction item = validAttraction();
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(true);
        when(repository.createAttraction(10, item)).thenReturn(item);

        // Act
        Attraction result = service.createAttraction(10, 7, item);

        // Assert
        assertEquals(item, result);
        assertEquals(Integer.valueOf(10), item.getItineraryId());
        verify(itineraries).verifyItineraryAndUser(10, 7);
        verify(repository).createAttraction(10, item);
    }

    @Test
    @DisplayName("Throws UnauthorizedUserException when user does not own itinerary")
    void createAttraction_notOwner_throwsWithoutSaving() {
        // Arrange
        Attraction item = validAttraction();
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(false);

        // Act & Assert
        assertThrows(UnauthorizedUserException.class,
                () -> service.createAttraction(10, 7, item));
        verifyNoInteractions(repository, gdacs);
    }

    @Test
    @DisplayName("Throws InvalidInputsException when coordinates are (0,0)")
    void createAttraction_zeroCoordinates_throwsWithoutSaving() {
        // Arrange
        Attraction item = validAttraction();
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(true);
        item.setLatitude(0.0);
        item.setLongitude(0.0);

        // Act & Assert
        assertThrows(InvalidInputsException.class,
                () -> service.createAttraction(10, 7, item));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Populates disaster warning when nearby disaster is detected")
    void getAllAttraction_addsDisasterWarning() {
        // Arrange
        Attraction item = validAttraction();
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(true);
        when(repository.findAllAttractionsByItineraryId(10)).thenReturn(List.of(item));
        when(gdacs.evaluateProximityAndDateFromDisasters(
                item.getLatitude(), item.getLongitude(),
                item.getVisitDate(), item.getVisitDate())).thenReturn("Nearby disaster");

        // Act
        List<Attraction> result = service.getAllAttraction(10, 7);

        // Assert
        assertEquals(1, result.size());
        assertEquals(item, result.get(0));
        assertEquals("Nearby disaster", result.get(0).getDisasterWarning());
        verify(itineraries).verifyItineraryAndUser(10, 7);
        verify(gdacs).evaluateProximityAndDateFromDisasters(
                item.getLatitude(), item.getLongitude(),
                item.getVisitDate(), item.getVisitDate());
        verifyNoMoreInteractions(gdacs);
    }

    @Test
    @DisplayName("Does not evaluate disasters when itinerary has no attractions")
    void getAllAttraction_emptyList_doesNotEvaluateDisasters() {
        // Arrange
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(true);
        when(repository.findAllAttractionsByItineraryId(10)).thenReturn(List.of());

        // Act
        List<Attraction> result = service.getAllAttraction(10, 7);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(gdacs);
    }

    @ParameterizedTest(name = "Latitude {0}, Longitude {1}")
    @DisplayName("Throws InvalidInputsException when creating attraction with coordinates outside valid geographic range")
    @CsvSource({
        "-90.001, 139.76",
        "90.001, 139.76",
        "35.68, -180.001",
        "35.68, 180.001"
    })
    void createAttraction_coordinatesOutsideGeographicRange_throwsInvalidInputsException(
            double latitude, double longitude) {
        // Arrange
        Attraction item = validAttraction();
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(true);
        item.setLatitude(latitude);
        item.setLongitude(longitude);

        // Act & Assert
        assertThrows(InvalidInputsException.class, () -> service.createAttraction(10, 7, item));
        verify(repository, never()).createAttraction(anyInt(), any());
    }

    @ParameterizedTest(name = "Latitude {0}, Longitude {1}")
    @DisplayName("Throws InvalidInputsException when updating attraction with coordinates outside valid geographic range")
    @CsvSource({
        "-90.001, 139.76",
        "90.001, 139.76",
        "35.68, -180.001",
        "35.68, 180.001"
    })
    void updateAttraction_coordinatesOutsideGeographicRange_throwsInvalidInputsException(
            double latitude, double longitude) {
        // Arrange
        Attraction item = validAttraction();
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(true);
        item.setLatitude(latitude);
        item.setLongitude(longitude);

        // Act & Assert
        assertThrows(InvalidInputsException.class, () -> service.updateAttraction(10, 7, 20, item));
        verify(repository, never()).updateAttraction(anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("Sets friendly fallback warning when disaster monitoring service fails")
    void getAllAttraction_disasterFailure_preservesDataAndReportsFailure() {
        // Arrange
        Attraction item = validAttraction();
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(true);
        when(repository.findAllAttractionsByItineraryId(10)).thenReturn(List.of(item));
        when(gdacs.evaluateProximityAndDateFromDisasters(item.getLatitude(), item.getLongitude(), item.getVisitDate(), item.getVisitDate()))
                .thenThrow(new IllegalStateException("Fixture disaster outage"));

        // Act
        List<Attraction> result = assertDoesNotThrow(() -> service.getAllAttraction(10, 7));

        // Assert
        assertEquals(1, result.size());
        assertEquals(item, result.get(0));
        assertEquals("Disaster monitoring is currently unavailable. We are doing our best to resolve this.", result.get(0).getDisasterWarning());
    }
}
