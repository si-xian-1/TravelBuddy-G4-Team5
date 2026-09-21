/*To add script to run table creation*/
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    mobile_number VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'TRAVELER',
    has_consent_location BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS itinerary (
    itinerary_id INT AUTO_INCREMENT PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    date DATETIME NOT NULL,
    type VARCHAR(50) NOT NULL,
    user_id INT NOT NULL,

    CONSTRAINT fk_itinerary_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(user_id) 
        ON DELETE CASCADE
);