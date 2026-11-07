# TravelBuddy acceptance tests

## Run

From the project root:

```bash
mvn -Dtest='*ServiceTest,*AcceptanceTest' test
```

Run just the isolated service unit tests:

```bash
mvn -Dtest='*ServiceTest' test
```

Run just the HTTP/database acceptance tests:

```bash
mvn -Dtest='*AcceptanceTest' test
```

The existing Spring context smoke test is outside these commands. These commands do not load application.properties, call external services, or use application MySQL.

## Requirements and coverage

| Approved requirement | Tests |
| --- | --- |
| Itinerary start may equal end; start after end is invalid | ItineraryServiceTest; ItineraryAcceptanceTest.creationAndRequiredFields |
| Hotel check-out DATE must be strictly later than check-in DATE, even if check-out time is later on the same date | HotelServiceTest; HotelAcceptanceTest.datesAndPartialUpdates |
| Flight local arrival timestamps need not be later than local departure timestamps; same-day flights are valid in a one-day itinerary | FlightAcceptanceTest.datesAndPartialUpdates |
| Hotel/flight dates and new or updated attraction visits must be within inclusive itinerary dates | Each item acceptance class: datesAndPartialUpdates |
| Changing itinerary dates must recheck existing hotels and flights and reject without writes if either would be excluded | ItineraryServiceTest; ItineraryAcceptanceTest.updatesRevalidateHotelsAndFlights |
| Existing attractions outside changed itinerary dates do not block the itinerary update and receive no date-range warning | ItineraryAcceptanceTest.updatesRevalidateHotelsAndFlights |
| Partial updates preserve omitted fields, validate merged values, and reject explicitly null or blank required fields | Each acceptance class: requiredFields, datesAndPartialUpdates, crudAndAccess; itinerary update cases |
| Required names, airline, flight number, airports and dates must be present; whitespace names are invalid | Each acceptance class: requiredFields; itinerary creationAndRequiredFields |
| No additional name/flight-number length or format restrictions | Free-text/punctuation and 300+ character cases in requiredFields and creationAndRequiredFields |
| Coordinates must be present, latitude in [-90,90], longitude in [-180,180], and not (0,0); individual zeros and range endpoints are valid | Each item acceptance class: coordinates; isolated coordinate service tests |
| Authenticated owner is authoritative; other travelers receive 403; missing parent/item or item in a different requested itinerary receives 404 | Each acceptance class: crudAndAccess or accessAndDeletion |
| List results belong to the requested owner/itinerary; empty results are JSON arrays | Each acceptance class: crudAndAccess or accessAndDeletion |
| Duplicate items are allowed and persist as distinct records | Each item acceptance class: crudAndAccess |
| Deleting an itinerary deletes all associated item rows and preserves other itineraries | ItineraryAcceptanceTest.accessAndDeletion |
| Disaster warnings use hotel coordinates/stay, flight arrival coordinates/travel times, and attraction coordinates/visit time | Isolated service warning tests; each item acceptance class: warningConsistencyAndFailure |
| Warnings appear on selected-item, item-list and itinerary-detail responses | Each item acceptance class: warningConsistencyAndFailure |
| Disaster monitoring failure retains trip data and displays exactly "Disaster monitoring is currently unavailable. We are doing our best to resolve this." | Isolated service failure tests; each item acceptance class: warningConsistencyAndFailure |
| Repeated reads are stable, stale warnings clear, and equivalent create/update inputs obey the same rules | Each item acceptance class: warningConsistencyAndFailure and shared create/update matrices |

Out-of-range and other invalid-input API requests expect 400. Rejected requests
also compare stored data before and after the operation. Test data uses fixed
dates; there is no past-trip validation rule.

## Test boundaries

- The four files in src/test/java/com/TravelBuddy/service are isolated unit tests. Repositories and external services are mocked.
- The four concrete files in src/test/java/com/TravelBuddy/acceptance run shared
  acceptance scenarios. They use real controllers, Jakarta validation, services,
  JDBC repositories and the production schema.sql.
- TravelAcceptanceSupport gives each dynamic scenario its own temporary H2
  database in MySQL compatibility mode. It closes the database after the case.
- Only disaster evaluation and the source of the authenticated principal are
  substituted in acceptance scenarios. Responses and repository results are not
  mocked to match expected outcomes.
- Standalone MVC supplies a fixed test principal. It verifies application
  ownership rules; it does not verify login, CSRF or the security filter chain.
- H2 exercises SQL scoping, persistence and foreign-key cascades. It is not a
  guarantee of identical behavior on MySQL; MySQL-specific verification remains
  a separate integration task.
- The artificial NOTIFY/IGNORE/ESCALATE JSON schema from the slides is not part
  of the agreed TravelBuddy requirements.

Common item requirements live in ItemAcceptanceContract and are executed
independently by HotelAcceptanceTest, FlightAcceptanceTest and
AttractionAcceptanceTest. Each dynamic test has a descriptive name; failure
messages repeat that name for the Surefire text/XML reports.
