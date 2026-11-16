-- ==========================================
-- USER 1: Affected by China Wildfire (1033222)
-- Disaster is at 46.4690, 127.9837
-- Attraction is in Shanghai (Safe)
-- ==========================================
INSERT INTO itineraries (itinerary_id, itinerary_name, start_date, end_date, user_id)
SELECT 1, 'China', '2026-10-09', '2026-11-18', 1
WHERE NOT EXISTS (SELECT 1 FROM itineraries WHERE itinerary_id = 1);

INSERT INTO hotels (hotel_id, itinerary_id, hotel_name, latitude, longitude, check_in, check_out)
SELECT 1, 1, 'Parkroyal Harbin Hotel', 46.4690, 127.9837, '2026-10-25 14:00:00', '2026-10-27 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE hotel_id = 1);

INSERT INTO flights (flight_id, itinerary_id, airline, flight_number, departure_airport, arrival_airport, departure_latitude, departure_longitude, arrival_latitude, arrival_longitude, departure_time, arrival_time)
SELECT 1, 1, 'Singapore Airlines', 'SQ123', 'SIN', 'HRB', 1.3502, 103.9940, 46.4690, 127.9837, '2026-10-26 13:14:00', '2026-10-27 01:14:00'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_id = 1);

INSERT INTO attractions (attraction_id, itinerary_id, attraction_name, latitude, longitude, visit_date)
SELECT 1, 1, 'Shanghai Disneyland', 31.14626, 121.65617, '2026-11-01 13:12:00'
WHERE NOT EXISTS (SELECT 1 FROM attractions WHERE attraction_id = 1);


-- ==========================================
-- USER 2 (Lydia): Affected by Mongolia Wildfire (1033256)
-- Disaster is at 46.4431, 114.5047
-- Attraction is in Ulaanbaatar (Safe)
-- ==========================================
INSERT INTO itineraries (itinerary_id, itinerary_name, start_date, end_date, user_id)
SELECT 2, 'Mongolia Steppe Adventure', '2026-10-25', '2026-11-05', 2
WHERE NOT EXISTS (SELECT 1 FROM itineraries WHERE itinerary_id = 2);

INSERT INTO hotels (hotel_id, itinerary_id, hotel_name, latitude, longitude, check_in, check_out)
SELECT 2, 2, 'Eastern Steppe Eco Resort', 46.4431, 114.5047, '2026-10-26 14:00:00', '2026-10-29 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE hotel_id = 2);

INSERT INTO flights (flight_id, itinerary_id, airline, flight_number, departure_airport, arrival_airport, departure_latitude, departure_longitude, arrival_latitude, arrival_longitude, departure_time, arrival_time)
SELECT 2, 2, 'MIAT Mongolian', 'OM202', 'PEK', 'UBN', 40.0799, 116.6031, 46.4431, 114.5047, '2026-10-26 09:00:00', '2026-10-26 11:30:00'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_id = 2);

INSERT INTO attractions (attraction_id, itinerary_id, attraction_name, latitude, longitude, visit_date)
SELECT 2, 2, 'Gorkhi-Terelj National Park (Ulaanbaatar)', 47.8864, 106.9057, '2026-10-27 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM attractions WHERE attraction_id = 2);

-- User 2's Second Itinerary (China)
INSERT INTO itineraries (itinerary_id, itinerary_name, start_date, end_date, user_id)
SELECT 4, 'China', '2026-10-09', '2026-11-18', 2
WHERE NOT EXISTS (SELECT 1 FROM itineraries WHERE itinerary_id = 4);

INSERT INTO hotels (hotel_id, itinerary_id, hotel_name, latitude, longitude, check_in, check_out)
SELECT 4, 4, 'Parkroyal Harbin Hotel', 46.4690, 127.9837, '2026-10-25 14:00:00', '2026-10-27 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE hotel_id = 4);

INSERT INTO flights (flight_id, itinerary_id, airline, flight_number, departure_airport, arrival_airport, departure_latitude, departure_longitude, arrival_latitude, arrival_longitude, departure_time, arrival_time)
SELECT 4, 4, 'Singapore Airlines', 'SQ123', 'SIN', 'HRB', 1.3502, 103.9940, 46.4690, 127.9837, '2026-10-26 13:14:00', '2026-10-27 01:14:00'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_id = 4);

INSERT INTO attractions (attraction_id, itinerary_id, attraction_name, latitude, longitude, visit_date)
SELECT 4, 4, 'Shanghai Disneyland', 31.14626, 121.65617, '2026-11-01 13:12:00'
WHERE NOT EXISTS (SELECT 1 FROM attractions WHERE attraction_id = 4);


-- ==========================================
-- USER 3 (Vincent): Affected by Xinjiang Earthquake (1573936)
-- Disaster is at 40.2886, 77.9297
-- Attraction is in Urumqi (Safe)
-- ==========================================
INSERT INTO itineraries (itinerary_id, itinerary_name, start_date, end_date, user_id)
SELECT 3, 'Xinjiang Heritage Tour', '2026-10-24', '2026-10-30', 3
WHERE NOT EXISTS (SELECT 1 FROM itineraries WHERE itinerary_id = 3);

INSERT INTO hotels (hotel_id, itinerary_id, hotel_name, latitude, longitude, check_in, check_out)
SELECT 3, 3, 'Aksu Grand Hotel', 40.2886, 77.9297, '2026-10-25 15:00:00', '2026-10-27 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE hotel_id = 3);

INSERT INTO flights (flight_id, itinerary_id, airline, flight_number, departure_airport, arrival_airport, departure_latitude, departure_longitude, arrival_latitude, arrival_longitude, departure_time, arrival_time)
SELECT 3, 3, 'China Southern', 'CZ3001', 'PVG', 'URC', 31.1443, 121.8083, 40.2886, 77.9297, '2026-10-26 07:00:00', '2026-10-26 13:00:00'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_id = 3);

INSERT INTO attractions (attraction_id, itinerary_id, attraction_name, latitude, longitude, visit_date)
SELECT 3, 3, 'Xinjiang Regional Museum (Urumqi)', 43.8256, 87.6168, '2026-10-26 16:00:00'
WHERE NOT EXISTS (SELECT 1 FROM attractions WHERE attraction_id = 3);

-- User 3's Second Itinerary (China)
INSERT INTO itineraries (itinerary_id, itinerary_name, start_date, end_date, user_id)
SELECT 5, 'China', '2026-10-09', '2026-11-18', 3
WHERE NOT EXISTS (SELECT 1 FROM itineraries WHERE itinerary_id = 5);

INSERT INTO hotels (hotel_id, itinerary_id, hotel_name, latitude, longitude, check_in, check_out)
SELECT 5, 5, 'Parkroyal Harbin Hotel', 46.4690, 127.9837, '2026-10-25 14:00:00', '2026-10-27 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE hotel_id = 5);

INSERT INTO flights (flight_id, itinerary_id, airline, flight_number, departure_airport, arrival_airport, departure_latitude, departure_longitude, arrival_latitude, arrival_longitude, departure_time, arrival_time)
SELECT 5, 5, 'Singapore Airlines', 'SQ123', 'SIN', 'HRB', 1.3502, 103.9940, 46.4690, 127.9837, '2026-10-26 13:14:00', '2026-10-27 01:14:00'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_id = 5);

INSERT INTO attractions (attraction_id, itinerary_id, attraction_name, latitude, longitude, visit_date)
SELECT 5, 5, 'Shanghai Disneyland', 31.14626, 121.65617, '2026-11-01 13:12:00'
WHERE NOT EXISTS (SELECT 1 FROM attractions WHERE attraction_id = 5);