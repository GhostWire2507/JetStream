-- =========================================================
-- JetStream Airline Reservation System - PostgreSQL Schema
-- Creates and populates schema "JetDB"
-- IMPORTANT: this script WILL DROP and RECREATE the schema objects
-- =========================================================

BEGIN;

-- 1) Create schema and ensure correct search_path for session
CREATE SCHEMA IF NOT EXISTS "JetDB";
-- Grant schema usage to application user (requires appropriate privileges)
-- If the role `jet_user` does not exist, skip or run as a superuser and create/grant as needed.
GRANT USAGE, CREATE ON SCHEMA "JetDB" TO jet_user;
-- Use the JetDB schema for the remainder of this session
SET search_path = "JetDB", public;

-- 2) Drop existing tables inside JetDB (qualified to avoid touching other schemas)
DROP TABLE IF EXISTS "JetDB".cancellations CASCADE;
DROP TABLE IF EXISTS "JetDB".reserved_seats CASCADE;
DROP TABLE IF EXISTS "JetDB".tickets CASCADE;
DROP TABLE IF EXISTS "JetDB".bookings CASCADE;
DROP TABLE IF EXISTS "JetDB".seats CASCADE;
DROP TABLE IF EXISTS "JetDB".fare CASCADE;
DROP TABLE IF EXISTS "JetDB".fleet_information CASCADE;
DROP TABLE IF EXISTS "JetDB".flight_information CASCADE;
DROP TABLE IF EXISTS "JetDB".flights CASCADE;
DROP TABLE IF EXISTS "JetDB".customer_details CASCADE;
DROP TABLE IF EXISTS "JetDB".users CASCADE;

-- =======================================================
-- Table: users (Login System - Admin, Staff, Customer)
-- =======================================================
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('admin', 'staff', 'customer')),
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- =======================================================
-- Table: customer_details (Extended Customer Information)
-- =======================================================
CREATE TABLE IF NOT EXISTS customer_details (
    id SERIAL PRIMARY KEY,
    user_id INT UNIQUE,
    cust_code VARCHAR(20) UNIQUE NOT NULL,
    cust_name VARCHAR(100) NOT NULL,
    father_name VARCHAR(100),
    gender VARCHAR(10) CHECK (gender IN ('Male', 'Female', 'Other')),
    date_of_birth DATE,
    address TEXT,
    tel_no VARCHAR(20),
    profession VARCHAR(50),
    security_question TEXT,
    security_answer TEXT,
    concession_type VARCHAR(30) CHECK (concession_type IN ('None', 'Student', 'Senior Citizen', 'Cancer Patient')),
    concession_percentage DECIMAL(5, 2) DEFAULT 0.00,
    travel_date DATE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =======================================================
-- Table: fleet_information (Aircraft Details)
-- =======================================================
CREATE TABLE IF NOT EXISTS fleet_information (
    id SERIAL PRIMARY KEY,
    aircraft_code VARCHAR(20) UNIQUE NOT NULL,
    no_aircraft VARCHAR(50),
    club_pre_capacity INT,
    eco_capacity INT,
    total_capacity INT,
    engine_type VARCHAR(50),
    cruise_speed VARCHAR(20),
    air_length VARCHAR(20),
    wing_span VARCHAR(20)
);

-- =======================================================
-- Table: flights (Flight Schedule)
-- =======================================================
CREATE TABLE IF NOT EXISTS flights (
    id SERIAL PRIMARY KEY,
    flight_number VARCHAR(10) UNIQUE NOT NULL,
    flight_name VARCHAR(100),
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    via VARCHAR(100),
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    travel_date DATE NOT NULL,
    aircraft_code VARCHAR(20),
    status VARCHAR(20) DEFAULT 'scheduled' CHECK (status IN ('scheduled', 'boarding', 'departed', 'arrived', 'cancelled', 'delayed')),
    FOREIGN KEY (aircraft_code) REFERENCES fleet_information(aircraft_code) ON DELETE SET NULL
);

-- =======================================================
-- Table: flight_information (Flight Class Details)
-- =======================================================
CREATE TABLE IF NOT EXISTS flight_information (
    id SERIAL PRIMARY KEY,
    flight_id INT NOT NULL,
    f_code VARCHAR(20) NOT NULL,
    f_name VARCHAR(100),
    c_code VARCHAR(20) NOT NULL CHECK (c_code IN ('ECO', 'EXE', 'BUSINESS', 'FIRST')),
    t_exe_seatno INT DEFAULT 0,
    t_eco_seatno INT DEFAULT 0,
    available_exe_seats INT DEFAULT 0,
    available_eco_seats INT DEFAULT 0,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
);

-- =======================================================
-- Table: seats (Seat Configuration)
-- =======================================================
CREATE TABLE IF NOT EXISTS seats (
    id SERIAL PRIMARY KEY,
    flight_id INT NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    class_code VARCHAR(20) NOT NULL CHECK (class_code IN ('ECO', 'EXE', 'BUSINESS', 'FIRST')),
    seat_type VARCHAR(20) CHECK (seat_type IN ('Window', 'Aisle', 'Middle')),
    is_available BOOLEAN DEFAULT TRUE,
    UNIQUE(flight_id, seat_number),
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
);

-- =======================================================
-- Table: fare (Route Pricing)
-- =======================================================
CREATE TABLE IF NOT EXISTS fare (
    id SERIAL PRIMARY KEY,
    route_code VARCHAR(20) UNIQUE NOT NULL,
    s_place VARCHAR(100) NOT NULL,
    d_place VARCHAR(100) NOT NULL,
    via VARCHAR(100),
    d_time TIME,
    a_time TIME,
    f_code VARCHAR(20),
    c_code VARCHAR(20) CHECK (c_code IN ('ECO', 'EXE', 'BUSINESS', 'FIRST')),
    base_fare DECIMAL(10, 2) NOT NULL
);

-- =======================================================
-- Table: bookings (Reservation Records)
-- =======================================================
CREATE TABLE IF NOT EXISTS bookings (
    id SERIAL PRIMARY KEY,
    pnr_number VARCHAR(20) UNIQUE NOT NULL,
    customer_id INT NOT NULL,
    flight_id INT NOT NULL,
    seat_id INT,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    travel_date DATE NOT NULL,
    class_code VARCHAR(20) NOT NULL,
    seat_preference VARCHAR(20),
    status VARCHAR(20) DEFAULT 'confirmed' CHECK (status IN ('confirmed', 'waiting', 'cancelled')),
    base_amount DECIMAL(10, 2) NOT NULL,
    concession_amount DECIMAL(10, 2) DEFAULT 0.00,
    final_amount DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer_details(id) ON DELETE CASCADE,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE,
    FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE SET NULL
);

-- =======================================================
-- Table: tickets (Generated Tickets)
-- =======================================================
CREATE TABLE IF NOT EXISTS tickets (
    id SERIAL PRIMARY KEY,
    ticket_number VARCHAR(20) UNIQUE NOT NULL,
    pnr_number VARCHAR(20) NOT NULL,
    booking_id INT NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    flight_number VARCHAR(10) NOT NULL,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    travel_date DATE NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    seat_number VARCHAR(10),
    class_code VARCHAR(20),
    fare DECIMAL(10, 2) NOT NULL,
    issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- =======================================================
-- Table: reserved_seats (Seat Reservation Tracking)
-- =======================================================
CREATE TABLE IF NOT EXISTS reserved_seats (
    id SERIAL PRIMARY KEY,
    f_code VARCHAR(20) NOT NULL,
    flight_id INT NOT NULL,
    t_res_eco_seat INT DEFAULT 0,
    t_res_exe_seat INT DEFAULT 0,
    t_date DATE NOT NULL,
    waiting_no INT DEFAULT 0,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
);

-- =======================================================
-- Table: cancellations (Cancellation Records)
-- =======================================================
CREATE TABLE IF NOT EXISTS cancellations (
    id SERIAL PRIMARY KEY,
    pnr_number VARCHAR(20) NOT NULL,
    booking_id INT NOT NULL,
    cust_code VARCHAR(20),
    class VARCHAR(20),
    seat_no VARCHAR(10),
    days_left INT,
    hours_left INT,
    basic_amount DECIMAL(10, 2),
    cancel_amount DECIMAL(10, 2),
    refund_amount DECIMAL(10, 2),
    reason TEXT,
    cancellation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- =========================================================
--  Sample Data - Users (Login Credentials)
-- =========================================================

INSERT INTO users (username, password, role, full_name, email, phone)
VALUES ('admin', 'admin123', 'admin', 'System Administrator', 'admin@jetstream.com', '+26650000001')
ON CONFLICT DO NOTHING;

INSERT INTO users (username, password, role, full_name, email, phone)
VALUES ('staff', 'staff123', 'staff', 'John Staff', 'staff@jetstream.com', '+26650000002')
ON CONFLICT DO NOTHING;

INSERT INTO users (username, password, role, full_name, email, phone)
VALUES ('customer', 'customer123', 'customer', 'Thabo Mokoena', 'thabo@example.com', '+26650123456')
ON CONFLICT DO NOTHING;

-- =========================================================
--  Sample Data - Customer Details
-- =========================================================
INSERT INTO customer_details (user_id, cust_code, cust_name, father_name, gender, date_of_birth, address, tel_no, profession, concession_type, concession_percentage)
VALUES 
((SELECT id FROM users WHERE username='customer'), 'CUST001','Thabo Mokoena','Peter Mokoena','Male','1995-05-15','123 Main St, Maseru','+26650123456','Engineer','None',0.00)
ON CONFLICT DO NOTHING;

INSERT INTO fleet_information (aircraft_code, no_aircraft, club_pre_capacity, eco_capacity, total_capacity, engine_type, cruise_speed, air_length, wing_span)
VALUES
('B737-800', 'Boeing 737-800', 20, 150, 170, 'CFM56-7B', '850 km/h', '39.5m', '35.8m')
ON CONFLICT DO NOTHING;

-- =========================================================
--  Sample Data - Flights (example)
-- =========================================================
INSERT INTO flights (flight_number, flight_name, origin, destination, via, departure_time, arrival_time, travel_date, aircraft_code, status)
VALUES
('JS100', 'JetStream Express', 'Johannesburg', 'Maseru', NULL, '2025-11-15 08:30:00', '2025-11-15 09:15:00', '2025-11-15', 'E190', 'scheduled')
ON CONFLICT DO NOTHING;

-- You can add more sample INSERTs as needed. Avoid hardcoding internal sequence ids
-- (the above uses lookups for user->id). This makes the script safe to re-run.

COMMIT;