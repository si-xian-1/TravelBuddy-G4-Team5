package com.TravelBuddy.acceptance;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItineraryAcceptanceTest extends TravelAcceptanceSupport {
    @TestFactory
    Stream<DynamicTest> creationAndRequiredFields() {
        List<DynamicTest> cases = new ArrayList<>();
        cases.add(scenario("creation uses authenticated owner despite spoofed owner", h -> {
            Map<String, Object> body = itineraryPayload(); body.put("userId", 8);
            h.expect("POST", "/api/itinerary/", body, 201);
            assertEquals(1, h.jdbc.queryForObject("SELECT COUNT(*) FROM itineraries WHERE itinerary_id NOT IN (10,11,12) AND user_id = 7", Integer.class));
        }));
        cases.add(scenario("same-day itinerary is valid", h -> {
            Map<String, Object> body = itineraryPayload(); body.put("endDate", body.get("startDate"));
            h.expect("POST", "/api/itinerary/", body, 201);
        }));
        cases.add(scenario("itinerary name has no extra length or format restriction", h -> {
            Map<String, Object> body = itineraryPayload();
            body.put("itineraryName", "free text / punctuation ! " + "x".repeat(300));
            h.expect("POST", "/api/itinerary/", body, 201);
        }));
        for (String method : List.of("POST", "PUT")) {
            cases.add(scenario(method + " reversed itinerary dates return 400 without changes", h -> {
                Map<String, Object> body = itineraryPayload(); body.put("startDate", "2027-01-16");
                h.reject(method, method.equals("POST") ? "/api/itinerary/" : "/api/itinerary/10", body, 400);
            }));
        }
        for (String field : List.of("itineraryName", "startDate", "endDate")) {
            for (String invalid : field.equals("itineraryName") ? List.of("omitted", "null", "empty", "whitespace") : List.of("omitted", "null")) {
                for (String method : List.of("POST", "PUT")) {
                    if (method.equals("PUT") && invalid.equals("omitted")) continue;
                    cases.add(scenario(method + " rejects " + invalid + " required " + field, h -> {
                        Map<String, Object> body = itineraryPayload();
                        if (invalid.equals("omitted")) body.remove(field);
                        else body.put(field, invalid.equals("null") ? null : invalid.equals("empty") ? "" : "   ");
                        h.reject(method, method.equals("POST") ? "/api/itinerary/" : "/api/itinerary/10", body, 400);
                    }));
                }
            }
        }
        return cases.stream();
    }

    @TestFactory
    Stream<DynamicTest> accessAndDeletion() {
        List<DynamicTest> cases = new ArrayList<>();
        cases.add(scenario("list returns only authenticated traveler's itineraries", h -> {
            List<Map<String, Object>> result = h.array(h.expect("GET", "/api/itinerary/all", null, 200));
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(row -> row.get("userId").equals(7)));
        }));
        cases.add(scenario("empty itinerary list is a JSON array", h -> {
            h.jdbc.update("DELETE FROM itineraries WHERE user_id = 7");
            assertTrue(h.array(h.expect("GET", "/api/itinerary/all", null, 200)).isEmpty());
            verifyNoInteractions(h.gdacs);
        }));
        cases.add(scenario("selected itinerary contains requested related records", h -> {
            Map<String, Object> result = h.json(h.expect("GET", "/api/itinerary/10", null, 200));
            assertEquals(10, result.get("itineraryId"));
            assertEquals(7, result.get("userId"));
            for (Item item : Item.values()) assertEquals(1, ((List<?>) result.get(item.table)).size());
        }));
        for (String method : List.of("GET", "PUT", "DELETE")) {
            cases.add(scenario(method + " missing itinerary returns 404", h ->
                    h.reject(method, "/api/itinerary/999", method.equals("PUT") ? itineraryPayload() : null, 404)));
            cases.add(scenario(method + " another traveler's itinerary returns 403", h ->
                    h.reject(method, "/api/itinerary/12", method.equals("PUT") ? itineraryPayload() : null, 403)));
        }
        cases.add(scenario("deleting itinerary cascades all items and preserves other trips", h -> {
            Map<String, List<Map<String, Object>>> before = h.snapshot();
            h.expect("DELETE", "/api/itinerary/10", null, 200);
            assertEquals(0, h.jdbc.queryForObject("SELECT COUNT(*) FROM itineraries WHERE itinerary_id = 10", Integer.class));
            for (Item item : Item.values()) {
                assertEquals(0, h.jdbc.queryForObject("SELECT COUNT(*) FROM " + item.table + " WHERE itinerary_id = 10", Integer.class));
                List<Map<String, Object>> expected = before.get(item.table).stream()
                        .filter(row -> ((Number) row.get("ITINERARY_ID")).intValue() != 10).toList();
                assertEquals(expected, h.jdbc.queryForList("SELECT * FROM " + item.table + " ORDER BY " + item.idColumn));
            }
            assertEquals(before.get("itineraries").stream()
                    .filter(row -> ((Number) row.get("ITINERARY_ID")).intValue() != 10).toList(),
                    h.jdbc.queryForList("SELECT * FROM itineraries ORDER BY itinerary_id"));
        }));
        return cases.stream();
    }

    @TestFactory
    Stream<DynamicTest> updatesRevalidateHotelsAndFlights() {
        List<DynamicTest> cases = new ArrayList<>();
        cases.add(scenario("name-only update preserves dates and owner", h -> {
            Map<String, Object> expected = new LinkedHashMap<>(h.jdbc.queryForMap("SELECT * FROM itineraries WHERE itinerary_id = 10"));
            expected.put("ITINERARY_NAME", "Renamed");
            h.expect("PUT", "/api/itinerary/10", Map.of("itineraryName", "Renamed"), 200);
            assertEquals(expected, h.jdbc.queryForMap("SELECT * FROM itineraries WHERE itinerary_id = 10"));
        }));
        cases.add(scenario("valid partial date extension preserves omitted values", h -> {
            h.expect("PUT", "/api/itinerary/10", Map.of("endDate", "2027-01-16"), 200);
            Map<String, Object> saved = h.jdbc.queryForMap("SELECT * FROM itineraries WHERE itinerary_id = 10");
            assertEquals(java.sql.Date.valueOf("2027-01-16"), saved.get("END_DATE"));
            assertEquals(java.sql.Date.valueOf("2027-01-10"), saved.get("START_DATE"));
            assertEquals("Test Trip", saved.get("ITINERARY_NAME"));
        }));
        for (Item item : List.of(Item.HOTEL, Item.FLIGHT)) {
            for (String boundary : List.of("startDate", "endDate")) {
                for (boolean partial : List.of(false, true)) {
                    cases.add(scenario((partial ? "partial" : "full") + " date change excluding " + item + " " + boundary + " rejects atomically", h -> {
                        for (Item other : Item.values()) if (other != item)
                            h.jdbc.update("DELETE FROM " + other.table + " WHERE itinerary_id = 10");
                        Map<String, Object> body = partial ? new LinkedHashMap<>() : itineraryPayload();
                        body.put(boundary, boundary.equals("startDate") ? "2027-01-11" : "2027-01-14");
                        h.reject("PUT", "/api/itinerary/10", body, 400);
                    }));
                }
            }
        }
        cases.add(scenario("existing attraction outside new dates does not block update or add range warning", h -> {
            h.jdbc.update("DELETE FROM hotels WHERE itinerary_id = 10");
            h.jdbc.update("DELETE FROM flights WHERE itinerary_id = 10");
            Map<String, Object> before = h.row(Item.ATTRACTION, 20);
            Map<String, Object> body = itineraryPayload(); body.put("startDate", "2027-01-12");
            h.expect("PUT", "/api/itinerary/10", body, 200);
            assertEquals(before, h.row(Item.ATTRACTION, 20));
            Map<String, Object> result = h.json(h.expect("GET", "/api/itinerary/10", null, 200));
            @SuppressWarnings("unchecked")
            Map<String, Object> attraction = ((List<Map<String, Object>>) result.get("attractions")).get(0);
            assertNull(attraction.get("disasterWarning"));
        }));
        cases.add(scenario("another itinerary's items do not block date changes", h -> {
            for (Item item : Item.values()) h.jdbc.update("DELETE FROM " + item.table + " WHERE itinerary_id = 10");
            Map<String, Object> body = itineraryPayload(); body.put("endDate", "2027-01-10");
            h.expect("PUT", "/api/itinerary/10", body, 200);
            assertEquals(java.sql.Date.valueOf("2027-01-10"),
                    h.jdbc.queryForObject("SELECT end_date FROM itineraries WHERE itinerary_id = 10", java.sql.Date.class));
        }));
        return cases.stream();
    }
}
