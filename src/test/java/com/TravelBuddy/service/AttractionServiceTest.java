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
        assertSame(item, result);
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
        assertSame(item, result.get(0));
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
}
