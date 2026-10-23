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
    facility_type VARCHAR(255)
);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 1, 'Sendagaya Elementary School (Tokyo)', 35.6763, 139.7088, 'School'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 1);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 2, 'Sarugaku Elementary School (Tokyo)', 35.6493, 139.7001, 'School'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 2);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 3, 'Wakagi Elementary School (Tokyo)', 35.7725, 139.6738, 'School'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 3);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 4, 'Kikkoman Arena / Citizen General Gymnasium (Chiba)', 35.8569, 139.9079, 'Gymnasium'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 4);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 5, 'Roadside Station Hota Elementary School (Chiba)', 35.1432, 139.8354, 'School'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 5);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 6, 'Nagaragawa International Conference Center (Gifu)', 35.4389, 136.7719, 'Conference Center'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 6);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 7, 'Akasaka Elementary School (Gifu)', 35.3855, 136.5815, 'School'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 7);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 8, 'Cultural and Artistic Exchange Center (Nagano)', 36.3980, 138.2483, 'Community Center'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 8);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 9, 'Kitazawa Junior High School (Tokyo)', 35.6669, 139.6698, 'School'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 9);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 10, 'Ichinomiya General Gymnasium (Aichi)', 35.3341, 136.8123, 'Gymnasium'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 10);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 11, 'Shinagawa Gakuen (Tokyo)', 35.6198, 139.7392, 'School'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 11);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 12, 'Ebisu Social Education Center (Tokyo)', 35.6467, 139.7112, 'Community Center'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 12);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 13, 'Roadside Station Jumonji (Akita)', 39.2941, 140.4852, 'Public Facility'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 13);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 14, 'Shingu Bunka Complex Tantsuru Hall (Wakayama)', 33.7289, 135.9982, 'Community Center'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 14);

INSERT INTO safe_shelter (shelter_id, shelter_name, latitude, longitude, facility_type)
SELECT 15, 'Bajikoen Shelter (Fukushima)', 37.6384, 141.0125, 'Public Facility'
WHERE NOT EXISTS (SELECT 1 FROM safe_shelter WHERE shelter_id = 15);