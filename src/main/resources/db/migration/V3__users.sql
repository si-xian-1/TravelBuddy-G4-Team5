INSERT INTO users (user_id, username, mobile_number, password, role, has_consent_location)
SELECT 1, 'albert', '12345678', 'ee18a012738cb81980e1018f9edac258334c5236a203560f6b6e219d9ff7149fb3029b8c325faf8c6c4994beb69e3924', 'TRAVELER', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_id = 1);