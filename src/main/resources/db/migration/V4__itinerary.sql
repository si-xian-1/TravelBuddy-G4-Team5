INSERT INTO itineraries (itinerary_id, itinerary_name, start_date, end_date, user_id)
SELECT 1, 'China', '2026-10-09', '2026-11-18', 1
WHERE NOT EXISTS (SELECT 1 FROM itineraries WHERE user_id = 1);

INSERT INTO hotels (hotel_id, itinerary_id, hotel_name, latitude, longitude, check_in, check_out)
SELECT 1, 1, 'Parkroyal hotel', '16.3977', '-93.817', '2026-10-25 14:00:00', '2026-10-27 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE itinerary_id = 1);

INSERT INTO flights (flight_id, itinerary_id, airline, flight_number, departure_airport, arrival_airport, departure_latitude, departure_longitude, arrival_latitude, arrival_longitude, departure_time, arrival_time)
SELECT 1, 1, 'Singapore airline', 'SQ123', 'SIN', 'HKG', '1.3502', '103.9940', '46.2304', '115.4737', '2026-10-26 13:14:00', '2026-10-27 01:14:00'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE itinerary_id = 1);

INSERT INTO attractions (attraction_id, itinerary_id, attraction_name, latitude, longitude, visit_date)
SELECT 1, 1, 'Shanghai disneyland', '31.14626', '121.65617', '2026-11-01 13:12:00'
WHERE NOT EXISTS (SELECT 1 FROM attractions WHERE itinerary_id = 1);