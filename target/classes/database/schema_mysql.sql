-- =========================================================
--  JetStream Airline Database Schema (MySQL)
-- =========================================================

-- Drop existing tables safely (optional, for clean rebuild)
DROP TABLE IF EXISTS cancellations;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS flights;
DROP TABLE IF EXISTS passengers;
DROP TABLE IF EXISTS admins;

-- =========================================================
--  Flights Table
-- =========================================================
CREATE TABLE flights (
    id INT AUTO_INCREMENT PRIMARY KEY,
    flight_number VARCHAR(20) NOT NULL UNIQUE,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    departure_time DATETIME NOT NULL,
    arrival_time DATETIME NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'Scheduled'
) ENGINE=InnoDB;

-- =========================================================
--  Passengers Table
-- =========================================================
CREATE TABLE passengers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(30),
    nationality VARCHAR(50)
) ENGINE=InnoDB;

-- =========================================================
--  Bookings Table
-- =========================================================
CREATE TABLE bookings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    passenger_id INT,
    flight_id INT,
    booking_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'Confirmed',
    FOREIGN KEY (passenger_id) REFERENCES passengers(id) ON DELETE CASCADE,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
--  Cancellations Table
-- =========================================================
CREATE TABLE cancellations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT,
    reason TEXT,
    cancellation_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
--  Admins Table
-- =========================================================
CREATE TABLE admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'Admin'
) ENGINE=InnoDB;

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

