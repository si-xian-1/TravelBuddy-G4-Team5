INSERT INTO users (user_id, username, mobile_number, password, role, has_consent_location)
SELECT 1, 'albert', '12345678', 'ee18a012738cb81980e1018f9edac258334c5236a203560f6b6e219d9ff7149fb3029b8c325faf8c6c4994beb69e3924', 'TRAVELER', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_id = 1);

INSERT INTO users (user_id, username, mobile_number, password, role, has_consent_location)
SELECT 2, 'lydia', '11112222', 'c554f004502e900984a0c890f374e64d007a9f0f9414993a1c1858ef4fc5ef1be74a819a4ab3b86a3f113cc448d0f551', 'TRAVELER', true
WHERE NOT EXISTS (SELECT 2 FROM users WHERE user_id = 2);

INSERT INTO users (user_id, username, mobile_number, password, role, has_consent_location)
SELECT 3, 'vincent', '33334444', '85b269056bd2dda4ac3addfe94910071dbc8bd709622cfe396bbdb4412f08a6a4eb72d075681b76c7fefb109dc191978', 'TRAVELER', true
WHERE NOT EXISTS (SELECT 3 FROM users WHERE user_id = 3);