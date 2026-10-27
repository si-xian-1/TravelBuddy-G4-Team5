/*To add script to run table creation*/
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    mobile_number VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'TRAVELER',
    has_consent_location BOOLEAN NOT NULL DEFAULT FALSE,
    emergency_contact_name VARCHAR(50),
    emergency_contact_number VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS itineraries (
    itinerary_id INT AUTO_INCREMENT PRIMARY KEY,
    itinerary_name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    user_id INT NOT NULL,

    CONSTRAINT fk_itineraries_user
        FOREIGN KEY (user_id) 
        REFERENCES users(user_id) 
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS hotels (
    hotel_id INT AUTO_INCREMENT PRIMARY KEY,
    itinerary_id INT NOT NULL,
    hotel_name VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    check_in DATETIME NOT NULL,
    check_out DATETIME NOT NULL,

    CONSTRAINT fk_hotels_itinerary
        FOREIGN KEY (itinerary_id)
        REFERENCES itineraries(itinerary_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS flights (
    flight_id INT AUTO_INCREMENT PRIMARY KEY,
    itinerary_id INT NOT NULL,
    airline VARCHAR(255),
    flight_number VARCHAR(50),
    departure_airport VARCHAR(100) NOT NULL,
    arrival_airport VARCHAR(100) NOT NULL,
    departure_latitude DOUBLE NOT NULL,
    departure_longitude DOUBLE NOT NULL,
    arrival_latitude DOUBLE NOT NULL,
    arrival_longitude DOUBLE NOT NULL,
    departure_time DATETIME NOT NULL,
    arrival_time DATETIME NOT NULL,

    CONSTRAINT fk_flights_itinerary
        FOREIGN KEY (itinerary_id)
        REFERENCES itineraries(itinerary_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS attractions (
    attraction_id INT AUTO_INCREMENT PRIMARY KEY,
    itinerary_id INT NOT NULL,
    attraction_name VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    visit_date DATETIME NOT NULL,

    CONSTRAINT fk_attractions_itinerary
        FOREIGN KEY (itinerary_id)
        REFERENCES itineraries(itinerary_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS safe_shelter (
    shelter_id INT PRIMARY KEY,
    shelter_name VARCHAR(255),
    latitude DOUBLE,
    longitude DOUBLE,
    facility_type VARCHAR(255),
    country VARCHAR(255)
);

-- Mexico (Chiapas Inland Earthquake - Event 1574133)
INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 1, 'Proteccion Civil Chiapas Headquarters (Tuxtla)', 16.7456, -93.1611, 'Civil Defense Center', 'Mexico'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 1);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 2, 'Poliforum Chiapas Assembly Hall', 16.7580, -93.0845, 'Civic Auditorium', 'Mexico'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 2);

-- Argentina (Jujuy Andes Earthquake - Event 1574130)
INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 3, 'Centro de Atencion Vecinal San Jose (Jujuy)', -24.1858, -65.2971, 'Community Center', 'Argentina'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 3);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 4, 'Estadio 23 de Agosto Evacuation Zone', -24.2023, -65.2941, 'Sports Stadium', 'Argentina'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 4);

-- Chile (Tarapaca Inland Earthquake - Event 1574007)
INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 5, 'Municipalidad de Iquique Emergency Hub (COE)', -20.2141, -70.1517, 'Municipal Hall', 'Chile'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 5);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 6, 'Estadio Tierra de Campeones Safe Zone', -20.2458, -70.1384, 'Sports Stadium', 'Chile'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 6);

-- Tajikistan (Rasht Pamir Earthquake - Event 1573997)
INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 7, 'Dushanbe Central Stadium Assembly Base', 38.5878, 68.7758, 'Sports Arena', 'Tajikistan'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 7);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 8, 'Kokhi Somon Regional Relief Hub', 38.5833, 68.7667, 'Exhibition Hall', 'Tajikistan'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 8);

-- China (Xinjiang Earthquake - Event 1573936)
INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 9, 'Aksu City Cultural and Sports Center', 41.1688, 80.2634, 'Indoor Sports Complex', 'China'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 9);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 10, 'Aksu No. 1 Senior High Safe Ground', 41.1821, 80.2915, 'School', 'China'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 10);

-- Indonesia (Flores Island Earthquake - Event 1573911)
INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 11, 'Ende Regency Civic Hall (East Nusa Tenggara)', -8.8400, 121.6500, 'Civic Center', 'Indonesia'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 11);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 12, 'RSUD Ende Regional Hospital Complex', -8.8485, 121.6562, 'Hospital Ground', 'Indonesia'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 12);

-- Australia (Barkly / Northern Territory Bushfires - Events 1033244, 1033217)
INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 13, 'Tennant Creek Civic Centre Assembly Hall', -19.6483, 134.1904, 'Community Hall', 'Australia'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 13);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 14, 'Katherine Sports and Recreation Complex', -14.4644, 132.2635, 'Sports Complex', 'Australia'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 14);

-- Mongolia (Eastern Steppe Wildfires - Event 1033256)
INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 15, 'Baruun-Urt Emergency Management Agency Center', 46.6806, 113.2806, 'Emergency Services Base', 'Mongolia'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 15);

-- Botswana (Kalahari Basin Bushfires - Event 1033228)
INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 16, 'Ghanzi Civic Community Centre', -21.6967, 21.6467, 'Civic Center', 'Botswana'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 16);

-- Brazil (Para / Amazon Basin Forest Fires - Event 1033218)
INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 17, 'Ginasio Municipal de Santarem (Para)', -2.4431, -54.7083, 'Municipal Gymnasium', 'Brazil'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 17);

-- Burkina Faso (Sahel Regional Fires - Events 1033212, 1033232)
INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 18, 'Maison des Jeunes et de la Culture (Kaya)', 13.0917, -1.0844, 'Community Center', 'Burkina Faso'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 18);

-- Japan (Tokyo Urban Disruption Support)
INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 19, 'Sendagaya Elementary School (Tokyo)', 35.6763, 139.7088, 'School', 'Japan'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 19);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type, country)
SELECT 20, 'Tokyo Metropolitan Gymnasium Safe Area', 35.6797, 139.7122, 'Sports Arena', 'Japan'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 20);