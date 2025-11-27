-- =========================================================
--  JetStream Airline Database Schema (PostgreSQL)
-- =========================================================

-- Drop existing tables safely (optional, for clean rebuild)
DROP TABLE IF EXISTS cancellations CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS flights CASCADE;
DROP TABLE IF EXISTS passengers CASCADE;
DROP TABLE IF EXISTS admins CASCADE;

-- =========================================================
--  Flights Table
-- =========================================================
CREATE TABLE flights (
    id SERIAL PRIMARY KEY,
    flight_number VARCHAR(20) NOT NULL UNIQUE,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'Scheduled'
);

-- =========================================================
--  Passengers Table
-- =========================================================
CREATE TABLE passengers (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(30),
    nationality VARCHAR(50)
);

-- =========================================================
--  Bookings Table
-- =========================================================
CREATE TABLE bookings (
    id SERIAL PRIMARY KEY,
    passenger_id INT,
    flight_id INT,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'Confirmed',
    FOREIGN KEY (passenger_id) REFERENCES passengers(id) ON DELETE CASCADE,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
);

-- =========================================================
--  Cancellations Table
-- =========================================================
CREATE TABLE cancellations (
    id SERIAL PRIMARY KEY,
    booking_id INT,
    reason TEXT,
    cancellation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- =========================================================
--  Admins Table
-- =========================================================
CREATE TABLE admins (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'Admin'
);

-- =========================================================
--  Sample Data 
-- =========================================================
INSERT INTO flights (flight_number, origin, destination, departure_time, arrival_time, price)
VALUES 
('JS100', 'Johannesburg', 'Maseru', '2025-11-15 08:30:00', '2025-11-15 09:15:00', 1200.00),
('JS101', 'Maseru', 'Cape Town', '2025-11-16 10:00:00', '2025-11-16 12:15:00', 2200.00),
('JS102', 'Durban', 'Maseru', '2025-11-17 14:00:00', '2025-11-17 15:30:00', 1600.00);

INSERT INTO passengers (full_name, email, phone, nationality)
VALUES 
('Thabo Mokoena', 'thabo@example.com', '+26650123456', 'Lesotho'),
('Lerato Ndlovu', 'lerato@example.com', '+26658123456', 'South Africa');

INSERT INTO bookings (passenger_id, flight_id)
VALUES 
(1, 1),
(2, 2);

