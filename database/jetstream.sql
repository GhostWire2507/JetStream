-- =======================================================
-- CLEAN RESET FOR POSTGRES
-- =======================================================

DROP TABLE IF EXISTS cancellations CASCADE;
DROP TABLE IF EXISTS reserved_seats CASCADE;
DROP TABLE IF EXISTS tickets CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS seats CASCADE;
DROP TABLE IF EXISTS fare CASCADE;
DROP TABLE IF EXISTS flight_information CASCADE;
DROP TABLE IF EXISTS flights CASCADE;
DROP TABLE IF EXISTS fleet_information CASCADE;
DROP TABLE IF EXISTS customer_details CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =======================================================
-- TABLES
-- =======================================================

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('admin', 'staff', 'customer')),
    full_name TEXT NOT NULL,
    email TEXT,
    phone TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customer_details (
    id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE,
    cust_code TEXT UNIQUE NOT NULL,
    cust_name TEXT NOT NULL,
    father_name TEXT,
    gender TEXT CHECK (gender IN ('Male', 'Female', 'Other')),
    date_of_birth DATE,
    address TEXT,
    tel_no TEXT,
    profession TEXT,
    concession_type TEXT CHECK (concession_type IN ('None', 'Student', 'Senior Citizen', 'Cancer Patient')),
    concession_percentage REAL DEFAULT 0.00,
    travel_date DATE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS fleet_information (
    id SERIAL PRIMARY KEY,
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

CREATE TABLE IF NOT EXISTS flights (
    id SERIAL PRIMARY KEY,
    flight_number TEXT UNIQUE NOT NULL,
    flight_name TEXT,
    origin TEXT NOT NULL,
    destination TEXT NOT NULL,
    via TEXT,
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    travel_date DATE NOT NULL,
    aircraft_code TEXT,
    status TEXT DEFAULT 'scheduled'
        CHECK (status IN ('scheduled', 'boarding', 'departed', 'arrived', 'cancelled', 'delayed')),
    FOREIGN KEY (aircraft_code) REFERENCES fleet_information(aircraft_code) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS flight_information (
    id SERIAL PRIMARY KEY,
    flight_id INTEGER NOT NULL,
    f_code TEXT NOT NULL,
    f_name TEXT,
    c_code TEXT NOT NULL CHECK (c_code IN ('ECO', 'EXE', 'BUSINESS', 'FIRST')),
    t_exe_seatno INTEGER DEFAULT 0,
    t_eco_seatno INTEGER DEFAULT 0,
    available_exe_seats INTEGER DEFAULT 0,
    available_eco_seats INTEGER DEFAULT 0,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS seats (
    id SERIAL PRIMARY KEY,
    flight_id INTEGER NOT NULL,
    seat_number TEXT NOT NULL,
    class_code TEXT NOT NULL CHECK (class_code IN ('ECO', 'EXE', 'BUSINESS', 'FIRST')),
    seat_type TEXT CHECK (seat_type IN ('Window', 'Aisle', 'Middle')),
    is_available BOOLEAN DEFAULT TRUE,
    UNIQUE(flight_id, seat_number),
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS fare (
    id SERIAL PRIMARY KEY,
    route_code TEXT UNIQUE NOT NULL,
    s_place TEXT NOT NULL,
    d_place TEXT NOT NULL,
    via TEXT,
    d_time TEXT,
    a_time TEXT,
    f_code TEXT,
    c_code TEXT CHECK (c_code IN ('ECO', 'EXE', 'BUSINESS', 'FIRST')),
    base_fare REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS bookings (
    id SERIAL PRIMARY KEY,
    pnr_number TEXT UNIQUE NOT NULL,
    customer_id INTEGER NOT NULL,
    flight_id INTEGER NOT NULL,
    seat_id INTEGER,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    travel_date DATE NOT NULL,
    class_code TEXT NOT NULL,
    seat_preference TEXT,
    status TEXT DEFAULT 'confirmed'
        CHECK (status IN ('confirmed', 'waiting', 'cancelled')),
    base_amount REAL NOT NULL,
    concession_amount REAL DEFAULT 0.00,
    final_amount REAL NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer_details(id) ON DELETE CASCADE,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE,
    FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS tickets (
    id SERIAL PRIMARY KEY,
    ticket_number TEXT UNIQUE NOT NULL,
    pnr_number TEXT NOT NULL,
    booking_id INTEGER NOT NULL,
    passenger_name TEXT NOT NULL,
    flight_number TEXT NOT NULL,
    origin TEXT NOT NULL,
    destination TEXT NOT NULL,
    travel_date DATE NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    seat_number TEXT,
    class_code TEXT,
    fare REAL NOT NULL,
    issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reserved_seats (
    id SERIAL PRIMARY KEY,
    f_code TEXT NOT NULL,
    flight_id INTEGER NOT NULL,
    t_res_eco_seat INTEGER DEFAULT 0,
    t_res_exe_seat INTEGER DEFAULT 0,
    t_date DATE NOT NULL,
    waiting_no INTEGER DEFAULT 0,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cancellations (
    id SERIAL PRIMARY KEY,
    pnr_number TEXT NOT NULL,
    booking_id INTEGER NOT NULL,
    cust_code TEXT,
    class TEXT,
    seat_no TEXT,
    days_left INTEGER,
    hours_left INTEGER,
    basic_amount REAL,
    cancel_amount REAL,
    refund_amount REAL,
    reason TEXT,
    cancellation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- =======================================================
-- SAMPLE DATA - USERS
-- =======================================================
INSERT INTO users (username, password, role, full_name, email, phone)
VALUES 
('admin', 'admin123', 'admin', 'System Administrator', 'admin@jetstream.com', '+26650000001'),
('staff', 'staff123', 'staff', 'John Staff', 'staff@jetstream.com', '+26650000002'),
('customer', 'customer123', 'customer', 'Thabo Mokoena', 'thabo@example.com', '+26650123456'),
('customer2', 'cust234', 'customer', 'Mpho Leballo', 'mpho@example.com', '+26650123457'),
('customer3', 'cust345', 'customer', 'Kabelo Thamae', 'kabelo@example.com', '+26650123458'),
('staff2', 'staff234', 'staff', 'Lerato Staff', 'lerato@jetstream.com', '+26650000003')
ON CONFLICT DO NOTHING;

-- =======================================================
-- SAMPLE DATA - CUSTOMER DETAILS
-- =======================================================
INSERT INTO customer_details
(user_id, cust_code, cust_name, father_name, gender, date_of_birth, address, tel_no, profession, concession_type, concession_percentage)
VALUES
((SELECT id FROM users WHERE username='customer'),'CUST001','Thabo Mokoena','Peter Mokoena','Male','1995-05-15','123 Main St, Maseru','+26650123456','Engineer','None',0.00),
((SELECT id FROM users WHERE username='customer2'),'CUST002','Mpho Leballo','Simon Leballo','Female','1998-08-22','45 High St, Maseru','+26650123457','Teacher','Student',10.00),
((SELECT id FROM users WHERE username='customer3'),'CUST003','Kabelo Thamae','Daniel Thamae','Male','1990-12-10','12 Freedom Rd, Maseru','+26650123458','Doctor','Senior Citizen',15.00),
(NULL,'CUST004','Lindiwe Nkosi','Joseph Nkosi','Female','1992-03-18','78 Church St, Maseru','+26650234567','Nurse','None',0.00)
ON CONFLICT DO NOTHING;

-- =======================================================
-- SAMPLE DATA - FLEET INFORMATION
-- =======================================================
INSERT INTO fleet_information
(aircraft_code, no_aircraft, club_pre_capacity, eco_capacity, total_capacity, engine_type, cruise_speed, air_length, wing_span)
VALUES
('B737-800', 'Boeing 737-800', 20, 150, 170, 'CFM56-7B', '850 km/h', '39.5m', '35.8m'),
('E190', 'Embraer 190', 12, 80, 92, 'CF34-10E', '820 km/h', '36.2m', '28.7m'),
('A320', 'Airbus A320', 24, 140, 164, 'CFM56-5B', '830 km/h', '37.6m', '34.1m'),
('B787-9', 'Boeing 787-9 Dreamliner', 30, 220, 250, 'GEnx-1B', '910 km/h', '62.8m', '60.1m')
ON CONFLICT DO NOTHING;

-- =======================================================
-- SAMPLE DATA - FLIGHTS
-- =======================================================
INSERT INTO flights
(flight_number, flight_name, origin, destination, via, departure_time, arrival_time, travel_date, aircraft_code, status)
VALUES
('JS100', 'JetStream Express', 'Johannesburg', 'Maseru', NULL, '2025-11-15 08:30:00', '2025-11-15 09:15:00', '2025-11-15', 'B737-800', 'scheduled'),
('JS101', 'JetStream Connect', 'Maseru', 'Bloemfontein', NULL, '2025-11-16 10:00:00', '2025-11-16 11:00:00', '2025-11-16', 'E190', 'scheduled'),
('JS102', 'JetStream Link', 'Johannesburg', 'Maseru', 'Bloemfontein', '2025-11-17 09:00:00', '2025-11-17 10:45:00', '2025-11-17', 'A320', 'scheduled'),
('JS103', 'JetStream Premium', 'Cape Town', 'Maseru', NULL, '2025-11-18 14:00:00', '2025-11-18 16:30:00', '2025-11-18', 'B787-9', 'boarding')
ON CONFLICT DO NOTHING;

-- =======================================================
-- SAMPLE DATA - FLIGHT INFORMATION
-- =======================================================
INSERT INTO flight_information
(flight_id, f_code, f_name, c_code, t_exe_seatno, t_eco_seatno, available_exe_seats, available_eco_seats)
VALUES
(1, 'JS100', 'JetStream Express', 'EXE', 20, 150, 20, 150),
(1, 'JS100', 'JetStream Express', 'ECO', 20, 150, 20, 150),
(2, 'JS101', 'JetStream Connect', 'EXE', 12, 80, 12, 80),
(3, 'JS102', 'JetStream Link', 'EXE', 24, 140, 24, 140)
ON CONFLICT DO NOTHING;

-- =======================================================
-- SAMPLE DATA - SEATS
-- =======================================================
INSERT INTO seats (flight_id, seat_number, class_code, seat_type)
VALUES
(1, '1A', 'EXE', 'Window'),
(1, '1B', 'EXE', 'Aisle'),
(1, '12A', 'ECO', 'Window'),
(2, '2A', 'EXE', 'Window')
ON CONFLICT DO NOTHING;

-- =======================================================
-- SAMPLE DATA - FARE
-- =======================================================
INSERT INTO fare
(route_code, s_place, d_place, via, d_time, a_time, f_code, c_code, base_fare)
VALUES
('JHB-MSU', 'Johannesburg', 'Maseru', NULL, '08:30', '09:15', 'JS100', 'ECO', 850),
('MSU-BFN', 'Maseru', 'Bloemfontein', NULL, '10:00', '11:00', 'JS101', 'ECO', 620),
('JHB-MSU-BFN', 'Johannesburg', 'Maseru', 'Bloemfontein', '09:00', '10:45', 'JS102', 'ECO', 990),
('CPT-MSU', 'Cape Town', 'Maseru', NULL, '14:00', '16:30', 'JS103', 'ECO', 1400)
ON CONFLICT DO NOTHING;

-- =======================================================
-- SAMPLE DATA - BOOKINGS
-- =======================================================
INSERT INTO bookings
(pnr_number, customer_id, flight_id, seat_id, travel_date, class_code, seat_preference, status, base_amount, concession_amount, final_amount)
VALUES
('PNR001', 1, 1, 1, '2025-11-15', 'EXE', 'Window', 'confirmed', 2000, 0, 2000),
('PNR002', 2, 2, 4, '2025-11-16', 'EXE', 'Window', 'confirmed', 1500, 150, 1350),
('PNR003', 3, 3, NULL, '2025-11-17', 'ECO', 'Aisle', 'waiting', 900, 135, 765),
('PNR004', 4, 4, NULL, '2025-11-18', 'ECO', NULL, 'confirmed', 1400, 0, 1400)
ON CONFLICT DO NOTHING;

-- =======================================================
-- SAMPLE DATA - TICKETS
-- =======================================================
INSERT INTO tickets
(ticket_number, pnr_number, booking_id, passenger_name, flight_number, origin, destination, travel_date, departure_time, seat_number, class_code, fare)
VALUES
('TIC001', 'PNR001', 1, 'Thabo Mokoena', 'JS100', 'Johannesburg', 'Maseru', '2025-11-15', '2025-11-15 08:30:00', '1A', 'EXE', 2000),
('TIC002', 'PNR002', 2, 'Mpho Leballo', 'JS101', 'Maseru', 'Bloemfontein', '2025-11-16', '2025-11-16 10:00:00', '2A', 'EXE', 1350),
('TIC003', 'PNR003', 3, 'Kabelo Thamae', 'JS102', 'Johannesburg', 'Maseru', '2025-11-17', '2025-11-17 09:00:00', NULL, 'ECO', 765)
ON CONFLICT DO NOTHING;

-- =======================================================
-- SAMPLE DATA - RESERVED SEATS
-- =======================================================
INSERT INTO reserved_seats
(f_code, flight_id, t_res_eco_seat, t_res_exe_seat, t_date, waiting_no)
VALUES
('JS100', 1, 2, 1, '2025-11-15', 0),
('JS101', 2, 1, 0, '2025-11-16', 1),
('JS102', 3, 3, 1, '2025-11-17', 2),
('JS103', 4, 0, 2, '2025-11-18', 0)
ON CONFLICT DO NOTHING;

-- =======================================================
-- SAMPLE DATA - CANCELLATIONS
-- =======================================================
INSERT INTO cancellations
(pnr_number, booking_id, cust_code, class, seat_no, days_left, hours_left, basic_amount, cancel_amount, refund_amount, reason)
VALUES
('PNR003', 3, 'CUST003', 'ECO', NULL, 3, 12, 900, 100, 800, 'Personal reasons')
ON CONFLICT DO NOTHING;
