-- =========================================================
-- JetStream Airline Reservation System - SQLite Schema
-- Complete database for local/dev use (SQLite)
-- =========================================================

PRAGMA foreign_keys = OFF;

-- Drop existing tables (safe reset for development)
DROP TABLE IF EXISTS cancellations;
DROP TABLE IF EXISTS reserved_seats;
DROP TABLE IF EXISTS tickets;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS fare;
DROP TABLE IF EXISTS fleet_information;
DROP TABLE IF EXISTS flight_information;
DROP TABLE IF EXISTS flights;
DROP TABLE IF EXISTS customer_details;
DROP TABLE IF EXISTS users;

PRAGMA foreign_keys = ON;

-- =======================================================
-- Table: users (Login System - Admin, Staff, Customer)
-- =======================================================
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('admin','staff','customer')),
    full_name TEXT NOT NULL,
    email TEXT,
    phone TEXT,
    created_at TEXT DEFAULT (datetime('now')),
    last_login TEXT
);

-- =======================================================
-- Table: customer_details (Extended Customer Information)
-- =======================================================
CREATE TABLE customer_details (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER UNIQUE,
    cust_code TEXT UNIQUE NOT NULL,
    cust_name TEXT NOT NULL,
    father_name TEXT,
    gender TEXT CHECK (gender IN ('Male','Female','Other')),
    date_of_birth TEXT,
    address TEXT,
    tel_no TEXT,
    profession TEXT,
    security_question TEXT,
    security_answer TEXT,
    concession_type TEXT CHECK (concession_type IN ('None','Student','Senior Citizen','Cancer Patient')),
    concession_percentage NUMERIC DEFAULT 0.00,
    travel_date TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =======================================================
-- Table: fleet_information (Aircraft Details)
-- =======================================================
CREATE TABLE fleet_information (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    aircraft_code TEXT UNIQUE NOT NULL,
    no_aircraft TEXT,
    club_pre_capacity INTEGER,
    eco_capacity INTEGER,
    total_capacity INTEGER,
    engine_type TEXT,
    cruise_speed TEXT,
    air_length TEXT,
    wing_span TEXT
);

-- =======================================================
-- Table: flights (Flight Schedule)
-- =======================================================
CREATE TABLE flights (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    flight_number TEXT UNIQUE NOT NULL,
    flight_name TEXT,
    origin TEXT NOT NULL,
    destination TEXT NOT NULL,
    via TEXT,
    departure_time TEXT NOT NULL,
    arrival_time TEXT NOT NULL,
    travel_date TEXT NOT NULL,
    aircraft_code TEXT,
    status TEXT DEFAULT 'scheduled',
    FOREIGN KEY (aircraft_code) REFERENCES fleet_information(aircraft_code) ON DELETE SET NULL
);

-- =======================================================
-- Table: flight_information (Flight Class Details)
-- =======================================================
CREATE TABLE flight_information (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    flight_id INTEGER NOT NULL,
    f_code TEXT NOT NULL,
    f_name TEXT,
    c_code TEXT NOT NULL CHECK (c_code IN ('ECO','EXE','BUSINESS','FIRST')),
    t_exe_seatno INTEGER DEFAULT 0,
    t_eco_seatno INTEGER DEFAULT 0,
    available_exe_seats INTEGER DEFAULT 0,
    available_eco_seats INTEGER DEFAULT 0,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
);

-- =======================================================
-- Table: seats (Seat Configuration)
-- =======================================================
CREATE TABLE seats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    flight_id INTEGER NOT NULL,
    seat_number TEXT NOT NULL,
    class_code TEXT NOT NULL CHECK (class_code IN ('ECO','EXE','BUSINESS','FIRST')),
    seat_type TEXT CHECK (seat_type IN ('Window','Aisle','Middle')),
    is_available INTEGER DEFAULT 1,
    UNIQUE(flight_id, seat_number),
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
);

-- =======================================================
-- Table: fare (Route Pricing)
-- =======================================================
CREATE TABLE fare (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    route_code TEXT UNIQUE NOT NULL,
    s_place TEXT NOT NULL,
    d_place TEXT NOT NULL,
    via TEXT,
    d_time TEXT,
    a_time TEXT,
    f_code TEXT,
    c_code TEXT CHECK (c_code IN ('ECO','EXE','BUSINESS','FIRST')),
    base_fare NUMERIC NOT NULL
);

-- =======================================================
-- Table: bookings (Reservation Records)
-- =======================================================
CREATE TABLE bookings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pnr_number TEXT UNIQUE NOT NULL,
    customer_id INTEGER NOT NULL,
    flight_id INTEGER NOT NULL,
    seat_id INTEGER,
    booking_date TEXT DEFAULT (datetime('now')),
    travel_date TEXT NOT NULL,
    class_code TEXT NOT NULL,
    seat_preference TEXT,
    status TEXT DEFAULT 'confirmed' CHECK (status IN ('confirmed','waiting','cancelled')),
    base_amount NUMERIC NOT NULL,
    concession_amount NUMERIC DEFAULT 0.00,
    final_amount NUMERIC NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer_details(id) ON DELETE CASCADE,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE,
    FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE SET NULL
);

-- =======================================================
-- Table: tickets (Generated Tickets)
-- =======================================================
CREATE TABLE tickets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_number TEXT UNIQUE NOT NULL,
    pnr_number TEXT NOT NULL,
    booking_id INTEGER NOT NULL,
    passenger_name TEXT NOT NULL,
    flight_number TEXT NOT NULL,
    origin TEXT NOT NULL,
    destination TEXT NOT NULL,
    travel_date TEXT NOT NULL,
    departure_time TEXT NOT NULL,
    seat_number TEXT,
    class_code TEXT,
    fare NUMERIC NOT NULL,
    issue_date TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- =======================================================
-- Table: reserved_seats (Seat Reservation Tracking)
-- =======================================================
CREATE TABLE reserved_seats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    f_code TEXT NOT NULL,
    flight_id INTEGER NOT NULL,
    t_res_eco_seat INTEGER DEFAULT 0,
    t_res_exe_seat INTEGER DEFAULT 0,
    t_date TEXT NOT NULL,
    waiting_no INTEGER DEFAULT 0,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
);

-- =======================================================
-- Table: cancellations (Cancellation Records)
-- =======================================================
CREATE TABLE cancellations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pnr_number TEXT NOT NULL,
    booking_id INTEGER NOT NULL,
    cust_code TEXT,
    class TEXT,
    seat_no TEXT,
    days_left INTEGER,
    hours_left INTEGER,
    basic_amount NUMERIC,
    cancel_amount NUMERIC,
    refund_amount NUMERIC,
    reason TEXT,
    cancellation_date TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- =======================================================
-- Sample Data (small set for development)
-- =======================================================
INSERT INTO users (username, password, role, full_name, email, phone)
VALUES ('admin','admin123','admin','System Administrator','admin@jetstream.com','+26650000001');

INSERT INTO users (username, password, role, full_name, email, phone)
VALUES ('staff','staff123','staff','John Staff','staff@jetstream.com','+26650000002');

INSERT INTO users (username, password, role, full_name, email, phone)
VALUES ('customer','customer123','customer','Thabo Mokoena','thabo@example.com','+26650123456');

INSERT INTO customer_details (user_id, cust_code, cust_name, father_name, gender, date_of_birth, address, tel_no, profession, concession_type, concession_percentage)
VALUES (3,'CUST001','Thabo Mokoena','Peter Mokoena','Male','1995-05-15','123 Main St, Maseru','+26650123456','Engineer','None',0.00);

INSERT INTO fleet_information (aircraft_code, no_aircraft, club_pre_capacity, eco_capacity, total_capacity, engine_type, cruise_speed, air_length, wing_span)
VALUES ('E190','Embraer E190',12,88,100,'GE CF34-10E','830 km/h','36.2m','28.7m');

INSERT INTO flights (flight_number, flight_name, origin, destination, via, departure_time, arrival_time, travel_date, aircraft_code, status)
VALUES ('JS100','JetStream Express','Johannesburg','Maseru',NULL,'2025-11-15 08:30:00','2025-11-15 09:15:00','2025-11-15','E190','scheduled');

INSERT INTO flight_information (flight_id, f_code, f_name, c_code, t_exe_seatno, t_eco_seatno, available_exe_seats, available_eco_seats)
VALUES (1,'JS100','JetStream Express','ECO',0,88,0,88);

INSERT INTO seats (flight_id, seat_number, class_code, seat_type, is_available)
VALUES (1,'4A','ECO','Window',1),(1,'4B','ECO','Middle',1),(1,'4C','ECO','Aisle',1);

INSERT INTO fare (route_code, s_place, d_place, via, d_time, a_time, f_code, c_code, base_fare)
VALUES ('JNB-MSU-ECO','Johannesburg','Maseru',NULL,'08:30:00','09:15:00','JS100','ECO',1200.00);

INSERT INTO bookings (pnr_number, customer_id, flight_id, seat_id, travel_date, class_code, seat_preference, status, base_amount, concession_amount, final_amount)
VALUES ('PNR001',1,1,1,'2025-11-15','ECO','Window','confirmed',1200.00,0.00,1200.00);

UPDATE seats SET is_available = 0 WHERE id = 1;

PRAGMA foreign_keys = ON;

-- End of SQLite schema
