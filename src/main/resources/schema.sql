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