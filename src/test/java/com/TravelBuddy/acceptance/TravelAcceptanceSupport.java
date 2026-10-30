package com.TravelBuddy.acceptance;

import com.TravelBuddy.controller.api.*;
import com.TravelBuddy.exception.RestExceptionHandler;
import com.TravelBuddy.model.User;
import com.TravelBuddy.repository.*;
import com.TravelBuddy.service.*;
import org.junit.jupiter.api.DynamicTest;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.*;
import tools.jackson.databind.json.JsonMapper;

import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Real controllers, validation, services, repositories and production schema.
 * Only the external disaster service and authenticated-principal source are substituted.
 * Every dynamic case owns a fresh H2 database; application.properties/MySQL are never loaded.
 */
abstract class TravelAcceptanceSupport {
    enum Item {
        HOTEL("hotels", "hotel_id", "hotelName", "hotel_name",
                List.of("hotelName", "checkIn", "checkOut"),
                List.of("latitude", "longitude"), List.of("checkIn", "checkOut")),
        FLIGHT("flights", "flight_id", "flightNumber", "flight_number",
                List.of("airline", "flightNumber", "departureAirport", "arrivalAirport", "departureTime", "arrivalTime"),
                List.of("departureLatitude", "departureLongitude", "arrivalLatitude", "arrivalLongitude"),
                List.of("departureTime", "arrivalTime")),
        ATTRACTION("attractions", "attraction_id", "attractionName", "attraction_name",
                List.of("attractionName", "visitDate"),
                List.of("latitude", "longitude"), List.of("visitDate"));

        final String table, idColumn, nameField, nameColumn;
        final List<String> required, coordinates, dates;
        Item(String table, String idColumn, String nameField, String nameColumn,
             List<String> required, List<String> coordinates, List<String> dates) {
            this.table = table;
            this.idColumn = idColumn;
            this.nameField = nameField;
            this.nameColumn = nameColumn;
            this.required = required;
            this.coordinates = coordinates;
            this.dates = dates;
        }
        String root(int itinerary) { return "/api/itinerary/" + itinerary + "/" + table; }
        String selected(int itinerary, int id) { return root(itinerary) + "/" + id; }
        Map<String, Object> payload() {
            Map<String, Object> result = new LinkedHashMap<>();
            switch (this) {
                case HOTEL -> {
                    result.put("hotelName", "Test Hotel");
                    result.put("latitude", 35.68); result.put("longitude", 139.76);
                    result.put("checkIn", "2027-01-10T15:00:00");
                    result.put("checkOut", "2027-01-15T11:00:00");
                }
                case FLIGHT -> {
                    result.put("airline", "Test Airline"); result.put("flightNumber", "test flight");
                    result.put("departureAirport", "Airport A"); result.put("arrivalAirport", "Airport B");
                    result.put("departureLatitude", 1.36); result.put("departureLongitude", 103.99);
                    result.put("arrivalLatitude", 35.55); result.put("arrivalLongitude", 139.78);
                    result.put("departureTime", "2027-01-10T23:00:00");
                    result.put("arrivalTime", "2027-01-15T01:00:00");
                }
                case ATTRACTION -> {
                    result.put("attractionName", "Test Attraction");
                    result.put("latitude", 35.71); result.put("longitude", 139.81);
                    result.put("visitDate", "2027-01-11T10:00:00");
                }
            }
            return result;
        }
    }

    @FunctionalInterface
    interface Scenario { void run(Harness h) throws Exception; }

    static DynamicTest scenario(String name, Scenario scenario) {
        return DynamicTest.dynamicTest(name, () -> {
            try (Harness h = new Harness()) {
                scenario.run(h);
            } catch (AssertionError failure) {
                throw new AssertionError(name + "\n" + failure.getMessage(), failure);
            }
        });
    }

    static String column(String camel) {
        return camel.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT);
    }

    static Map<String, Object> itineraryPayload() {
        return new LinkedHashMap<>(Map.of("itineraryName", "Test Trip",
                "startDate", "2027-01-10", "endDate", "2027-01-15"));
    }

    static class Harness implements AutoCloseable {
        final SingleConnectionDataSource dataSource;
        final JdbcTemplate jdbc;
        final GdacsService gdacs = mock(GdacsService.class);
        final JsonMapper mapper = JsonMapper.builder().build();
        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        final User user = new User();
        final MockMvc mvc;

        Harness() throws Exception {
            dataSource = new SingleConnectionDataSource(DriverManager.getConnection(
                    "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", ""), true);
            jdbc = new JdbcTemplate(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
            jdbc.update("INSERT INTO users (user_id, username, mobile_number, password) VALUES (7, 'fixture_a', '0', 'unused'), (8, 'fixture_b', '0', 'unused')");
            jdbc.update("INSERT INTO itineraries (itinerary_id, itinerary_name, start_date, end_date, user_id) VALUES "
                    + "(10, 'Test Trip', '2027-01-10', '2027-01-15', 7),"
                    + "(11, 'Second Trip', '2027-01-10', '2027-01-15', 7),"
                    + "(12, 'Other Trip', '2027-01-10', '2027-01-15', 8)");
            for (Item item : Item.values()) {
                insert(item, 20, 10);
                insert(item, 21, 11);
                insert(item, 22, 12);
            }

            HotelRepository hotels = new HotelRepository(jdbc);
            FlightRepository flights = new FlightRepository(jdbc);
            AttractionRepository attractions = new AttractionRepository(jdbc);
            ItineraryRepository itineraries = new ItineraryRepository(jdbc, hotels, attractions);
            ItineraryService itineraryService = new ItineraryService(itineraries, new TravelerRepository(jdbc), gdacs);
            user.setUserId(7);
            validator.afterPropertiesSet();
            mvc = MockMvcBuilders.standaloneSetup(
                    new ItineraryController(itineraryService),
                    new HotelController(new HotelService(hotels, itineraryService, gdacs)),
                    new FlightController(new FlightService(flights, itineraryService, gdacs)),
                    new AttractionController(new AttractionService(itineraries, gdacs, attractions)))
                    .setControllerAdvice(new RestExceptionHandler())
                    .setValidator(validator)
                    .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                        public boolean supportsParameter(MethodParameter parameter) {
                            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                        }
                        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                NativeWebRequest request, WebDataBinderFactory binderFactory) {
                            return user;
                        }
                    }).build();
        }

        void insert(Item item, int id, int itinerary) {
            Map<String, Object> payload = item.payload();
            List<String> columns = new ArrayList<>(List.of(item.idColumn, "itinerary_id"));
            List<Object> values = new ArrayList<>(List.of(id, itinerary));
            payload.forEach((field, value) -> {
                columns.add(column(field));
                values.add(item.dates.contains(field) ? LocalDateTime.parse(value.toString()) : value);
            });
            jdbc.update("INSERT INTO " + item.table + " (" + String.join(",", columns) + ") VALUES ("
                    + String.join(",", Collections.nCopies(values.size(), "?")) + ")", values.toArray());
        }

        MvcResult call(String method, String url, Map<String, Object> body) {
            return assertDoesNotThrow(() -> mvc.perform(request(HttpMethod.valueOf(method), url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body == null ? "" : mapper.writeValueAsString(body))).andReturn(),
                    method + " " + url + " must produce an HTTP response, not an unhandled exception");
        }

        MvcResult expect(String method, String url, Map<String, Object> body, int expectedStatus) {
            MvcResult result = call(method, url, body);
            assertEquals(expectedStatus, result.getResponse().getStatus(),
                    () -> method + " " + url + ": " + new String(result.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8));
            return result;
        }

        void reject(String method, String url, Map<String, Object> body, int expectedStatus) {
            Map<String, List<Map<String, Object>>> before = snapshot();
            MvcResult result = call(method, url, body);
            assertAll("Rejected requests must return the agreed status and leave every table unchanged",
                    () -> assertEquals(expectedStatus, result.getResponse().getStatus()),
                    () -> assertTrue(before.equals(snapshot()), "Rejected request changed persisted data"));
        }

        Map<String, List<Map<String, Object>>> snapshot() {
            Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
            result.put("itineraries", jdbc.queryForList("SELECT * FROM itineraries ORDER BY itinerary_id"));
            for (Item item : Item.values()) {
                result.put(item.table, jdbc.queryForList("SELECT * FROM " + item.table + " ORDER BY " + item.idColumn));
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> json(MvcResult result) throws Exception {
            return mapper.readValue(result.getResponse().getContentAsString(), Map.class);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> array(MvcResult result) throws Exception {
            return mapper.readValue(result.getResponse().getContentAsString(), List.class);
        }

        Map<String, Object> row(Item item, int id) {
            return jdbc.queryForMap("SELECT * FROM " + item.table + " WHERE " + item.idColumn + " = ?", id);
        }

        @Override public void close() {
            validator.close();
            dataSource.destroy();
        }
    }
}
