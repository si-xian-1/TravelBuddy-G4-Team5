package com.TravelBuddy.acceptance;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Shared requirements are run independently for hotel, flight and attraction. */
abstract class ItemAcceptanceContract extends TravelAcceptanceSupport {
    abstract Item item();

    @TestFactory
    Stream<DynamicTest> crudAndAccess() {
        Item item = item();
        List<DynamicTest> cases = new ArrayList<>();
        cases.add(scenario("valid creation persists values and ignores spoofed itinerary ID", h -> {
            Map<String, Object> body = item.payload();
            body.put("itineraryId", 12);
            Map<String, Object> response = h.json(h.expect("POST", item.root(10) + "/", body, 201));
            int id = ((Number) response.get(item == Item.HOTEL ? "hotelId" : item == Item.FLIGHT ? "flightId" : "attractionId")).intValue();
            Map<String, Object> saved = h.row(item, id);
            assertEquals(10, ((Number) saved.get("ITINERARY_ID")).intValue());
            assertEquals(body.get(item.nameField), saved.get(item.nameColumn.toUpperCase(Locale.ROOT)));
        }));
        cases.add(scenario("duplicates are allowed and create distinct records", h -> {
            int before = h.jdbc.queryForObject("SELECT COUNT(*) FROM " + item.table, Integer.class);
            h.expect("POST", item.root(10) + "/", item.payload(), 201);
            h.expect("POST", item.root(10) + "/", item.payload(), 201);
            assertEquals(before + 2, h.jdbc.queryForObject("SELECT COUNT(*) FROM " + item.table, Integer.class));
        }));
        cases.add(scenario("selected item returns the requested record", h -> {
            Map<String, Object> result = h.json(h.expect("GET", item.selected(10, 20), null, 200));
            assertEquals(item.payload().get(item.nameField), result.get(item.nameField));
            assertEquals(10, result.get("itineraryId"));
        }));
        cases.add(scenario("list contains only the selected itinerary's records", h -> {
            List<Map<String, Object>> result = h.array(h.expect("GET", item.root(10) + "/all", null, 200));
            assertEquals(1, result.size());
            assertEquals(10, result.get(0).get("itineraryId"));
        }));
        cases.add(scenario("empty list returns valid JSON array and does not query disasters", h -> {
            h.jdbc.update("DELETE FROM " + item.table + " WHERE itinerary_id = 10");
            assertTrue(h.array(h.expect("GET", item.root(10) + "/all", null, 200)).isEmpty());
            verifyNoInteractions(h.gdacs);
        }));
        cases.add(scenario("full update persists changed values", h -> {
            Map<String, Object> body = item.payload();
            body.put(item.nameField, "Changed");
            h.expect("PUT", item.selected(10, 20), body, 200);
            assertEquals("Changed", h.row(item, 20).get(item.nameColumn.toUpperCase(Locale.ROOT)));
        }));
        cases.add(scenario("partial update preserves all omitted values", h -> {
            Map<String, Object> expected = new LinkedHashMap<>(h.row(item, 20));
            expected.put(item.nameColumn.toUpperCase(Locale.ROOT), "Changed only");
            h.expect("PUT", item.selected(10, 20), Map.of(item.nameField, "Changed only"), 200);
            assertEquals(expected, h.row(item, 20));
        }));
        cases.add(scenario("delete removes only the selected record", h -> {
            Map<String, Object> other = h.row(item, 21);
            h.expect("DELETE", item.selected(10, 20), null, 200);
            assertEquals(0, h.jdbc.queryForObject("SELECT COUNT(*) FROM " + item.table + " WHERE " + item.idColumn + " = 20", Integer.class));
            assertEquals(other, h.row(item, 21));
        }));
        for (String method : List.of("GET", "PUT", "DELETE")) {
            cases.add(scenario(method + " missing item returns 404 without changes", h ->
                    h.reject(method, item.selected(10, 999), method.equals("PUT") ? item.payload() : null, 404)));
            cases.add(scenario(method + " item in another owned itinerary returns 404", h ->
                    h.reject(method, item.selected(10, 21), method.equals("PUT") ? item.payload() : null, 404)));
            cases.add(scenario(method + " another traveler's itinerary returns 403", h ->
                    h.reject(method, item.selected(12, 22), method.equals("PUT") ? item.payload() : null, 403)));
        }
        for (String method : List.of("GET", "POST")) {
            cases.add(scenario(method + " another traveler's collection returns 403", h ->
                    h.reject(method, item.root(12) + (method.equals("GET") ? "/all" : "/"),
                            method.equals("POST") ? item.payload() : null, 403)));
        }
        for (String method : List.of("GET", "POST", "PUT", "DELETE")) {
            cases.add(scenario(method + " missing parent itinerary returns 404", h -> {
                String url = item.root(999) + (method.equals("GET") ? "/all" : method.equals("POST") ? "/" : "/20");
                h.reject(method, url, List.of("POST", "PUT").contains(method) ? item.payload() : null, 404);
            }));
        }
        return cases.stream();
    }

    @TestFactory
    Stream<DynamicTest> requiredFields() {
        Item item = item();
        List<DynamicTest> cases = new ArrayList<>();
        for (String field : item.required) {
            for (String invalid : item.dates.contains(field) ? List.of("omitted", "null") : List.of("omitted", "null", "empty", "whitespace")) {
                cases.add(scenario("create rejects " + invalid + " required " + field, h -> {
                    Map<String, Object> body = item.payload();
                    if (invalid.equals("omitted")) body.remove(field);
                    else body.put(field, invalid.equals("null") ? null : invalid.equals("empty") ? "" : "   ");
                    h.reject("POST", item.root(10) + "/", body, 400);
                }));
                if (!invalid.equals("omitted")) {
                    cases.add(scenario("update rejects explicitly " + invalid + " required " + field, h -> {
                        Map<String, Object> body = item.payload();
                        body.put(field, invalid.equals("null") ? null : invalid.equals("empty") ? "" : "   ");
                        h.reject("PUT", item.selected(10, 20), body, 400);
                    }));
                }
            }
        }
        for (String field : item.required) {
            if (item.dates.contains(field)) continue;
            cases.add(scenario("no extra length or format restriction on " + field, h -> {
                Map<String, Object> body = item.payload();
                body.put(field, "free text / punctuation ! " + "x".repeat(300));
                h.expect("POST", item.root(10) + "/", body, 201);
            }));
        }
        return cases.stream();
    }

    @TestFactory
    Stream<DynamicTest> coordinates() {
        Item item = item();
        List<DynamicTest> cases = new ArrayList<>();
        for (String field : item.coordinates) {
            double limit = field.toLowerCase(Locale.ROOT).contains("latitude") ? 90 : 180;
            for (String method : List.of("POST", "PUT")) {
                for (Object value : Arrays.asList(null, -limit - 0.001, limit + 0.001)) {
                    cases.add(scenario(method + " rejects " + field + "=" + value, h -> {
                        Map<String, Object> body = item.payload(); body.put(field, value);
                        h.reject(method, method.equals("POST") ? item.root(10) + "/" : item.selected(10, 20), body, 400);
                    }));
                }
                if (method.equals("POST")) {
                    cases.add(scenario("create rejects omitted coordinate " + field, h -> {
                        Map<String, Object> body = item.payload(); body.remove(field);
                        h.reject("POST", item.root(10) + "/", body, 400);
                    }));
                }
                for (double value : new double[] {-limit, 0, limit}) {
                    cases.add(scenario(method + " accepts coordinate boundary " + field + "=" + value, h -> {
                        Map<String, Object> body = item.payload(); body.put(field, value);
                        h.expect(method, method.equals("POST") ? item.root(10) + "/" : item.selected(10, 20), body,
                                method.equals("POST") ? 201 : 200);
                    }));
                }
            }
        }
        for (int pair = 0; pair < item.coordinates.size(); pair += 2) {
            String latitude = item.coordinates.get(pair), longitude = item.coordinates.get(pair + 1);
            for (String method : List.of("POST", "PUT")) {
                cases.add(scenario(method + " rejects zero pair " + latitude + "/" + longitude, h -> {
                    Map<String, Object> body = item.payload(); body.put(latitude, 0); body.put(longitude, 0);
                    h.reject(method, method.equals("POST") ? item.root(10) + "/" : item.selected(10, 20), body, 400);
                }));
            }
        }
        return cases.stream();
    }

    @TestFactory
    Stream<DynamicTest> datesAndPartialUpdates() {
        Item item = item();
        List<DynamicTest> cases = new ArrayList<>();
        for (String field : item.dates) {
            for (String outside : List.of("2027-01-09T23:59:59", "2027-01-16T00:00:00")) {
                for (String method : List.of("POST", "PUT")) {
                    cases.add(scenario(method + " rejects out-of-itinerary " + field + "=" + outside, h -> {
                        Map<String, Object> body = item.payload(); body.put(field, outside);
                        h.reject(method, method.equals("POST") ? item.root(10) + "/" : item.selected(10, 20), body, 400);
                    }));
                }
            }
        }
        if (item == Item.HOTEL) {
            for (String method : List.of("POST", "PUT")) {
                for (String checkout : List.of("2027-01-12T18:00:00", "2027-01-12T10:00:00", "2027-01-11T18:00:00")) {
                    cases.add(scenario(method + " hotel checkout date must be strictly later: " + checkout, h -> {
                        Map<String, Object> body = item.payload();
                        body.put("checkIn", "2027-01-12T10:00:00"); body.put("checkOut", checkout);
                        h.reject(method, method.equals("POST") ? item.root(10) + "/" : item.selected(10, 20), body, 400);
                    }));
                }
            }
            cases.add(scenario("one-day itinerary cannot contain a hotel stay", h -> {
                h.jdbc.update("UPDATE itineraries SET end_date = start_date WHERE itinerary_id = 10");
                Map<String, Object> body = item.payload();
                body.put("checkOut", "2027-01-10T18:00:00");
                h.reject("POST", item.root(10) + "/", body, 400);
            }));
            cases.add(scenario("partial hotel update validates against stored check-in date", h ->
                    h.reject("PUT", item.selected(10, 20), Map.of("checkOut", "2027-01-10T23:00:00"), 400)));
        } else if (item == Item.FLIGHT) {
            for (String method : List.of("POST", "PUT")) {
                for (String arrival : List.of("2027-01-10T01:00:00", "2027-01-10T23:00:00", "2027-01-10T23:59:00")) {
                    cases.add(scenario(method + " accepts same-date local flight times: " + arrival, h -> {
                        h.jdbc.update("UPDATE itineraries SET end_date = start_date WHERE itinerary_id = 10");
                        Map<String, Object> body = item.payload(); body.put("arrivalTime", arrival);
                        h.expect(method, method.equals("POST") ? item.root(10) + "/" : item.selected(10, 20), body,
                                method.equals("POST") ? 201 : 200);
                    }));
                }
                cases.add(scenario(method + " accepts arrival local date before departure local date", h -> {
                    Map<String, Object> body = item.payload();
                    body.put("departureTime", "2027-01-11T01:00:00"); body.put("arrivalTime", "2027-01-10T23:00:00");
                    h.expect(method, method.equals("POST") ? item.root(10) + "/" : item.selected(10, 20), body,
                            method.equals("POST") ? 201 : 200);
                }));
            }
        } else {
            for (String boundary : List.of("2027-01-10T00:00:00", "2027-01-15T23:59:59")) {
                cases.add(scenario("attraction accepts inclusive boundary " + boundary, h -> {
                    Map<String, Object> body = item.payload(); body.put("visitDate", boundary);
                    h.expect("POST", item.root(10) + "/", body, 201);
                    h.expect("PUT", item.selected(10, 20), body, 200);
                }));
            }
        }
        // Hotel/flight default payload already touches both itinerary date boundaries.
        cases.add(scenario("partial date update accepts a valid value and preserves other fields", h -> {
            String field = item.dates.get(item.dates.size() - 1);
            String changed = item == Item.HOTEL ? "2027-01-14T11:00:00" :
                    item == Item.FLIGHT ? "2027-01-12T01:00:00" : "2027-01-12T10:00:00";
            Map<String, Object> expected = new LinkedHashMap<>(h.row(item, 20));
            expected.put(column(field).toUpperCase(Locale.ROOT), java.sql.Timestamp.valueOf(LocalDateTime.parse(changed)));
            h.expect("PUT", item.selected(10, 20), Map.of(field, changed), 200);
            assertEquals(expected, h.row(item, 20));
        }));
        return cases.stream();
    }

    @TestFactory
    Stream<DynamicTest> warningConsistencyAndFailure() {
        Item item = item();
        List<DynamicTest> cases = new ArrayList<>();
        for (String view : List.of("selected", "list", "itinerary")) {
            cases.add(scenario(view + " warning uses the agreed coordinates and time range", h -> {
                Map<String, Object> payload = item.payload();
                String lat = item == Item.FLIGHT ? "arrivalLatitude" : "latitude";
                String lon = item == Item.FLIGHT ? "arrivalLongitude" : "longitude";
                LocalDateTime start = LocalDateTime.parse(payload.get(item.dates.get(0)).toString());
                LocalDateTime end = LocalDateTime.parse(payload.get(item.dates.get(item.dates.size() - 1)).toString());
                when(h.gdacs.evaluateProximityAndDateFromDisasters(
                        ((Number) payload.get(lat)).doubleValue(), ((Number) payload.get(lon)).doubleValue(), start, end))
                        .thenReturn("Nearby disaster");
                assertEquals("Nearby disaster", warning(h, item, view));
                verify(h.gdacs).evaluateProximityAndDateFromDisasters(
                        ((Number) payload.get(lat)).doubleValue(), ((Number) payload.get(lon)).doubleValue(), start, end);
            }));
            cases.add(scenario(view + " disaster failure preserves trip data and reports failure", h -> {
                when(h.gdacs.evaluateProximityAndDateFromDisasters(anyDouble(), anyDouble(), any(), any()))
                        .thenThrow(new IllegalStateException("Fixture disaster outage"));
                Map<String, List<Map<String, Object>>> before = h.snapshot();
                assertEquals("disaster-service failure", warning(h, item, view));
                assertEquals(before, h.snapshot());
            }));
            cases.add(scenario(view + " repeated equivalent reads are stable and stale warnings clear", h -> {
                when(h.gdacs.evaluateProximityAndDateFromDisasters(anyDouble(), anyDouble(), any(), any()))
                        .thenReturn("Nearby disaster");
                assertEquals("Nearby disaster", warning(h, item, view));
                assertEquals("Nearby disaster", warning(h, item, view));
                when(h.gdacs.evaluateProximityAndDateFromDisasters(anyDouble(), anyDouble(), any(), any()))
                        .thenReturn(null);
                assertNull(warning(h, item, view));
            }));
        }
        return cases.stream();
    }

    @SuppressWarnings("unchecked")
    private Object warning(Harness h, Item item, String view) throws Exception {
        Map<String, Object> record;
        if (view.equals("selected")) record = h.json(h.expect("GET", item.selected(10, 20), null, 200));
        else if (view.equals("list")) record = h.array(h.expect("GET", item.root(10) + "/all", null, 200)).get(0);
        else {
            Map<String, Object> itinerary = h.json(h.expect("GET", "/api/itinerary/10", null, 200));
            record = ((List<Map<String, Object>>) itinerary.get(item.table)).get(0);
        }
        assertEquals(item.payload().get(item.nameField), record.get(item.nameField));
        assertEquals(10, record.get("itineraryId"));
        return record.get("disasterWarning");
    }
}
