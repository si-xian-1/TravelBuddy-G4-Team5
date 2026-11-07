package com.TravelBuddy.controller.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.TravelBuddy.model.Attraction;
import com.TravelBuddy.model.Flight;
import com.TravelBuddy.model.Hotel;
import com.TravelBuddy.model.Itinerary;
import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.AttractionRepository;
import com.TravelBuddy.repository.FlightRepository;
import com.TravelBuddy.repository.HotelRepository;
import com.TravelBuddy.repository.ItineraryRepository;
import com.TravelBuddy.repository.TravelerRepository;
import com.TravelBuddy.service.TravelerService;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

/**
 * Integration tests using REST Assured
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TravelerService travelerService;

    @Autowired
    private TravelerRepository travelerRepository;

    @Autowired
    private ItineraryRepository itineraryRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AttractionRepository attractionRepository;

    private String sessionCookie;
    private int sharedUserId;

    @BeforeAll
    void setUpAll() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        // Create shared test user and authenticate session once for tests
        String username = "restassured_shared_user";
        User user = new User();
        user.setUsername(username);
        user.setPassword("Password123!");
        user.setMobileNumber("81234567");
        user.setHasConsentLocation(true);
        user.setEmergencyContactName("Emergency Contact");
        user.setEmergencyContactMobileNumber("91234567");

        travelerService.createUser(user);
        User persisted = travelerRepository.findCredentialsByUsername(username);
        this.sharedUserId = persisted.getUserId();

        String loginPayload = String.format("""
                {
                    "username": "%s",
                    "password": "Password123!"
                }
                """, username);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(loginPayload)
                .when()
                .post("/api/travelers/login");

        this.sessionCookie = response.getCookie("JSESSIONID");
    }

    // Helper Method
    private int createTestItinerary(int userId, String name) {
        Itinerary itinerary = new Itinerary();
        itinerary.setItineraryName(name);
        itinerary.setStartDate(LocalDate.of(2026, 11, 1));
        itinerary.setEndDate(LocalDate.of(2026, 11, 10));
        itinerary.setUserId(userId);
        return itineraryRepository.createItinerary(itinerary);
    }

    @Test
    @DisplayName("Should successfully create a new traveler in database when valid details provided")
    void signUp_shouldSaveTravelerInDatabase_whenValidDetailsProvided() {
        String username = "traveler_signup_" + System.currentTimeMillis();
        String signupPayload = String.format("""
                {
                    "username": "%s",
                    "password": "Password123!",
                    "mobileNumber": "81234567",
                    "hasConsentLocation": true,
                    "emergencyContactName": "Jane Doe",
                    "emergencyContactMobileNumber": "91234567"
                }
                """, username);

        given()
            .contentType(ContentType.JSON)
            .body(signupPayload)
        .when()
            .post("/api/travelers/signup")
        .then()
            .statusCode(200);

        User savedUser = travelerRepository.findCredentialsByUsername(username);
        assertNotNull(savedUser, "User should be persisted in database");
        assertEquals(username, savedUser.getUsername());
        assertEquals("TRAVELER", savedUser.getRole());
    }

    @Test
    @DisplayName("Should successfully delete an existing traveler from database when authenticated")
    void deleteProfile_shouldDeleteUserFromDatabase_whenAuthenticated() {
        String username = "del_traveler_" + System.currentTimeMillis();
        User tempUser = new User();
        tempUser.setUsername(username);
        tempUser.setPassword("Password123!");
        tempUser.setMobileNumber("81234567");
        tempUser.setHasConsentLocation(true);
        travelerService.createUser(tempUser);
        User persisted = travelerRepository.findCredentialsByUsername(username);

        String loginPayload = String.format("""
                {
                    "username": "%s",
                    "password": "Password123!"
                }
                """, username);

        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(loginPayload)
                .when()
                .post("/api/travelers/login");

        String tempSession = loginResponse.getCookie("JSESSIONID");

        given()
            .cookie("JSESSIONID", tempSession)
        .when()
            .delete("/api/travelers/deleteProfile")
        .then()
            .statusCode(200);

        User userAfterDelete = travelerRepository.getUser(persisted.getUserId());
        assertNull(userAfterDelete, "Deleted traveler should no longer exist in database");
    }


    @Test
    @DisplayName("Should successfully create a new itinerary in database when authenticated")
    void createItinerary_shouldSaveItineraryInDatabase_whenAuthenticated() {
        String itineraryPayload = """
                {
                    "itineraryName": "Tokyo Expedition",
                    "startDate": "2026-11-01",
                    "endDate": "2026-11-10"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .cookie("JSESSIONID", sessionCookie)
            .body(itineraryPayload)
        .when()
            .post("/api/itinerary/")
        .then()
            .statusCode(201);

        List<Itinerary> itineraries = itineraryRepository.findAllItinerariesByUserId(sharedUserId);
        assertTrue(itineraries.stream().anyMatch(i -> "Tokyo Expedition".equals(i.getItineraryName())),
                "Created itinerary should exist in database");
    }

    @Test
    @DisplayName("Should successfully delete an existing itinerary from database when authenticated")
    void deleteItinerary_shouldDeleteItineraryFromDatabase_whenItineraryExists() {
        int itineraryId = createTestItinerary(sharedUserId, "Itinerary To Delete");

        given()
            .cookie("JSESSIONID", sessionCookie)
        .when()
            .delete("/api/itinerary/" + itineraryId)
        .then()
            .statusCode(200);

        List<Itinerary> itinerariesAfterDelete = itineraryRepository.findAllItinerariesByUserId(sharedUserId);
        assertTrue(itinerariesAfterDelete.stream().noneMatch(i -> i.getItineraryId() == itineraryId),
                "Deleted itinerary should not be found in user itineraries");
    }

    @Test
    @DisplayName("Should successfully create a new hotel in database when valid payload provided")
    void createHotel_shouldSaveHotelInDatabase_whenValidPayloadProvided() {
        int itineraryId = createTestItinerary(sharedUserId, "Osaka Holiday");
        String hotelPayload = """
                {
                    "hotelName": "Osaka Grand Palace",
                    "latitude": 34.6937,
                    "longitude": 135.5023,
                    "checkIn": "2026-11-01T15:00:00",
                    "checkOut": "2026-11-05T11:00:00"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .cookie("JSESSIONID", sessionCookie)
            .body(hotelPayload)
        .when()
            .post("/api/itinerary/" + itineraryId + "/hotels/")
        .then()
            .statusCode(201);

        List<Hotel> hotels = hotelRepository.findAllHotelsByItineraryId(itineraryId);
        assertFalse(hotels.isEmpty(), "Hotels list should not be empty");
        
        Hotel savedHotel = hotels.get(0);
        assertEquals("Osaka Grand Palace", savedHotel.getHotelName());
        assertEquals(34.6937, savedHotel.getLatitude(), 0.001);
        assertEquals(135.5023, savedHotel.getLongitude(), 0.001);
    }

    @Test
    @DisplayName("Should successfully delete an existing hotel from database when authenticated")
    void deleteHotel_shouldDeleteHotelFromDatabase_whenHotelExists() {
        int itineraryId = createTestItinerary(sharedUserId, "Sapporo Trip");
        Hotel existingHotel = new Hotel();
        existingHotel.setHotelName("Hotel To Delete");
        existingHotel.setLatitude(43.0618);
        existingHotel.setLongitude(141.3545);
        existingHotel.setCheckIn(LocalDateTime.of(2026, 11, 2, 14, 0));
        existingHotel.setCheckOut(LocalDateTime.of(2026, 11, 6, 10, 0));
        Hotel savedHotel = hotelRepository.createHotel(itineraryId, existingHotel);
        int hotelId = savedHotel.getHotelId();

        given()
            .cookie("JSESSIONID", sessionCookie)
        .when()
            .delete("/api/itinerary/" + itineraryId + "/hotels/" + hotelId)
        .then()
            .statusCode(200);

        List<Hotel> hotelsAfterDelete = hotelRepository.findAllHotelsByItineraryId(itineraryId);
        assertTrue(hotelsAfterDelete.isEmpty(), "Hotels list should be empty after deletion");
    }

    @Test
    @DisplayName("Should successfully create a new flight in database when valid payload provided")
    void createFlight_shouldSaveFlightInDatabase_whenValidPayloadProvided() {
        int itineraryId = createTestItinerary(sharedUserId, "Fukuoka Journey");
        String flightPayload = """
                {
                    "airline": "Singapore Airlines",
                    "flightNumber": "SQ656",
                    "departureAirport": "Changi Airport (SIN)",
                    "arrivalAirport": "Fukuoka Airport (FUK)",
                    "departureLatitude": 1.3644,
                    "departureLongitude": 103.9915,
                    "arrivalLatitude": 33.5859,
                    "arrivalLongitude": 130.4507,
                    "departureTime": "2026-11-01T01:20:00",
                    "arrivalTime": "2026-11-01T08:30:00"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .cookie("JSESSIONID", sessionCookie)
            .body(flightPayload)
        .when()
            .post("/api/itinerary/" + itineraryId + "/flights/")
        .then()
            .statusCode(201);

        List<Flight> flights = flightRepository.findAllFlightsByItineraryId(itineraryId);
        assertFalse(flights.isEmpty(), "Flights list should not be empty");
        
        Flight savedFlight = flights.get(0);
        assertEquals("Singapore Airlines", savedFlight.getAirline());
        assertEquals("SQ656", savedFlight.getFlightNumber());
        assertEquals("Changi Airport (SIN)", savedFlight.getDepartureAirport());
        assertEquals("Fukuoka Airport (FUK)", savedFlight.getArrivalAirport());
    }

    @Test
    @DisplayName("Should successfully delete an existing flight from database when authenticated")
    void deleteFlight_shouldDeleteFlightFromDatabase_whenFlightExists() {
        int itineraryId = createTestItinerary(sharedUserId, "Nagoya Voyage");
        Flight existingFlight = new Flight();
        existingFlight.setAirline("Japan Airlines");
        existingFlight.setFlightNumber("JL712");
        existingFlight.setDepartureAirport("Changi Airport (SIN)");
        existingFlight.setArrivalAirport("Chubu Centrair (NGO)");
        existingFlight.setDepartureLatitude(1.3644);
        existingFlight.setDepartureLongitude(103.9915);
        existingFlight.setArrivalLatitude(34.8584);
        existingFlight.setArrivalLongitude(136.8053);
        existingFlight.setDepartureTime(LocalDateTime.of(2026, 11, 2, 2, 15));
        existingFlight.setArrivalTime(LocalDateTime.of(2026, 11, 2, 9, 45));
        Flight savedFlight = flightRepository.createFlight(itineraryId, existingFlight);
        int flightId = savedFlight.getFlightId();

        given()
            .cookie("JSESSIONID", sessionCookie)
        .when()
            .delete("/api/itinerary/" + itineraryId + "/flights/" + flightId)
        .then()
            .statusCode(200);

        List<Flight> flightsAfterDelete = flightRepository.findAllFlightsByItineraryId(itineraryId);
        assertTrue(flightsAfterDelete.isEmpty(), "Flights list should be empty after deletion");
    }

    @Test
    @DisplayName("Should successfully create a new attraction in database when valid payload provided")
    void createAttraction_shouldSaveAttractionInDatabase_whenValidPayloadProvided() {
        int itineraryId = createTestItinerary(sharedUserId, "Tokyo Sightseeing");
        String attractionPayload = """
                {
                    "attractionName": "Tokyo Skytree",
                    "latitude": 35.7101,
                    "longitude": 139.8107,
                    "visitDate": "2026-11-02T10:00:00"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .cookie("JSESSIONID", sessionCookie)
            .body(attractionPayload)
        .when()
            .post("/api/itinerary/" + itineraryId + "/attractions/")
        .then()
            .statusCode(201);

        List<Attraction> attractions = attractionRepository.findAllAttractionsByItineraryId(itineraryId);
        assertFalse(attractions.isEmpty(), "Attractions list should not be empty");
        
        Attraction savedAttraction = attractions.get(0);
        assertEquals("Tokyo Skytree", savedAttraction.getAttractionName());
        assertEquals(35.7101, savedAttraction.getLatitude(), 0.001);
        assertEquals(139.8107, savedAttraction.getLongitude(), 0.001);
    }

    @Test
    @DisplayName("Should successfully delete an existing attraction from database when authenticated")
    void deleteAttraction_shouldDeleteAttractionFromDatabase_whenAttractionExists() {
        int itineraryId = createTestItinerary(sharedUserId, "Kamakura Tour");
        Attraction existingAttraction = new Attraction();
        existingAttraction.setAttractionName("Kotoku-in Great Buddha");
        existingAttraction.setLatitude(35.3167);
        existingAttraction.setLongitude(139.5361);
        existingAttraction.setVisitDate(LocalDateTime.of(2026, 11, 3, 11, 0));
        Attraction savedAttraction = attractionRepository.createAttraction(itineraryId, existingAttraction);
        int attractionId = savedAttraction.getAttractionId();

        given()
            .cookie("JSESSIONID", sessionCookie)
        .when()
            .delete("/api/itinerary/" + itineraryId + "/attractions/" + attractionId)
        .then()
            .statusCode(200);

        List<Attraction> attractionsAfterDelete = attractionRepository.findAllAttractionsByItineraryId(itineraryId);
        assertTrue(attractionsAfterDelete.isEmpty(), "Attractions list should be empty after deletion");
    }

    @Test
    @DisplayName("Should successfully persist and aggregate user, itinerary, hotel, flight, and attraction in database")
    void getSelectedItinerary_shouldVerifyItineraryWithHotelFlightAndAttractionsInDatabase() {
        int itineraryId = createTestItinerary(sharedUserId, "Grand Japan Tour");

        Hotel hotel = new Hotel();
        hotel.setHotelName("Hotel New Otani Tokyo");
        hotel.setLatitude(35.6812);
        hotel.setLongitude(139.7347);
        hotel.setCheckIn(LocalDateTime.of(2026, 11, 1, 15, 0));
        hotel.setCheckOut(LocalDateTime.of(2026, 11, 8, 11, 0));
        hotelRepository.createHotel(itineraryId, hotel);

        Flight flight = new Flight();
        flight.setAirline("All Nippon Airways");
        flight.setFlightNumber("NH802");
        flight.setDepartureAirport("Changi Airport (SIN)");
        flight.setArrivalAirport("Narita Airport (NRT)");
        flight.setDepartureLatitude(1.3644);
        flight.setDepartureLongitude(103.9915);
        flight.setArrivalLatitude(35.7720);
        flight.setArrivalLongitude(140.3929);
        flight.setDepartureTime(LocalDateTime.of(2026, 11, 1, 6, 10));
        flight.setArrivalTime(LocalDateTime.of(2026, 11, 1, 14, 20));
        flightRepository.createFlight(itineraryId, flight);

        Attraction attraction = new Attraction();
        attraction.setAttractionName("Senso-ji Temple");
        attraction.setLatitude(35.7148);
        attraction.setLongitude(139.7967);
        attraction.setVisitDate(LocalDateTime.of(2026, 11, 2, 9, 0));
        attractionRepository.createAttraction(itineraryId, attraction);

        given()
            .cookie("JSESSIONID", sessionCookie)
        .when()
            .get("/api/itinerary/" + itineraryId)
        .then()
            .statusCode(200);

        // Assert (Verify database state for all entities)
        Itinerary aggregated = itineraryRepository.findSelectedItineraryByUserId(itineraryId, sharedUserId);
        assertNotNull(aggregated, "Aggregated itinerary should exist in database");
        assertEquals("Grand Japan Tour", aggregated.getItineraryName());
        assertEquals(sharedUserId, aggregated.getUserId());

        // Assert Hotel in database
        assertEquals(1, aggregated.getHotels().size(), "Should have 1 hotel in database");
        assertEquals("Hotel New Otani Tokyo", aggregated.getHotels().get(0).getHotelName());
        assertEquals(itineraryId, aggregated.getHotels().get(0).getItineraryId());

        // Assert Flight in database
        assertEquals(1, aggregated.getFlights().size(), "Should have 1 flight in database");
        assertEquals("All Nippon Airways", aggregated.getFlights().get(0).getAirline());
        assertEquals("NH802", aggregated.getFlights().get(0).getFlightNumber());
        assertEquals(itineraryId, aggregated.getFlights().get(0).getItineraryId());

        // Assert Attraction in database
        assertEquals(1, aggregated.getAttractions().size(), "Should have 1 attraction in database");
        assertEquals("Senso-ji Temple", aggregated.getAttractions().get(0).getAttractionName());
        assertEquals(itineraryId, aggregated.getAttractions().get(0).getItineraryId());
    }
}
