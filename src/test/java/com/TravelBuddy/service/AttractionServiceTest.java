package com.TravelBuddy.service;

import com.TravelBuddy.exception.InvalidInputsException;
import com.TravelBuddy.exception.UnauthorizedUserException;
import com.TravelBuddy.model.*;
import com.TravelBuddy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AttractionServiceTest {
    private AttractionRepository repository;
    private ItineraryRepository itineraries;
    private GdacsService gdacs;
    private AttractionService service;

    @BeforeEach
    void setUp() {
        repository = mock(AttractionRepository.class);
        itineraries = mock(ItineraryRepository.class);
        gdacs = mock(GdacsService.class);
        service = new AttractionService(itineraries, gdacs, repository);
    }

    private Attraction validAttraction() {
        Attraction item = new Attraction();
        item.setAttractionName("Tokyo Skytree");
        item.setLatitude(35.71);
        item.setLongitude(139.81);
        item.setVisitDate(LocalDateTime.of(2027, 1, 11, 10, 0));
        return item;
    }

    @Test
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
    void createAttraction_notOwner_throwsWithoutSaving() {
        Attraction item = validAttraction();
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(false);

        assertThrows(UnauthorizedUserException.class,
                () -> service.createAttraction(10, 7, item));

        verifyNoInteractions(repository, gdacs);
    }

    @Test
    void createAttraction_zeroCoordinates_throwsWithoutSaving() {
        Attraction item = validAttraction();
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(true);
        item.setLatitude(0.0);
        item.setLongitude(0.0);

        assertThrows(InvalidInputsException.class,
                () -> service.createAttraction(10, 7, item));

        verifyNoInteractions(repository);
    }

    @Test
    void getAllAttraction_addsDisasterWarning() {
        Attraction item = validAttraction();
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(true);
        when(repository.findAllAttractionsByItineraryId(10)).thenReturn(List.of(item));
        when(gdacs.evaluateProximityAndDateFromDisasters(
                item.getLatitude(), item.getLongitude(),
                item.getVisitDate(), item.getVisitDate())).thenReturn("Nearby disaster");

        List<Attraction> result = service.getAllAttraction(10, 7);

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
    void getAllAttraction_emptyList_doesNotEvaluateDisasters() {
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(true);
        when(repository.findAllAttractionsByItineraryId(10)).thenReturn(List.of());

        assertTrue(service.getAllAttraction(10, 7).isEmpty());

        verifyNoInteractions(gdacs);
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
        Attraction item = validAttraction();
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(true);
        item.setLatitude(latitude);
        item.setLongitude(longitude);

        assertThrows(InvalidInputsException.class, () -> {
            if (updating) service.updateAttraction(10, 7, 20, item);
            else service.createAttraction(10, 7, item);
        });

        verify(repository, never()).createAttraction(anyInt(), any());
        verify(repository, never()).updateAttraction(anyInt(), anyInt(), any());
    }

    @Test
    void getAllAttraction_disasterFailure_preservesDataAndReportsFailure() {
        Attraction item = validAttraction();
        when(itineraries.verifyItineraryAndUser(10, 7)).thenReturn(true);
        when(repository.findAllAttractionsByItineraryId(10)).thenReturn(List.of(item));
        when(gdacs.evaluateProximityAndDateFromDisasters(item.getLatitude(), item.getLongitude(), item.getVisitDate(), item.getVisitDate()))
                .thenThrow(new IllegalStateException("Fixture disaster outage"));

        List<Attraction> result = assertDoesNotThrow(() -> service.getAllAttraction(10, 7));

        assertEquals(1, result.size());
        assertEquals(item, result.get(0));
        assertEquals("disaster-service failure", result.get(0).getDisasterWarning());
    }
}
