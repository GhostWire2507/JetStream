-- =======================================================
-- CLEAN RESET FOR SQLITE
-- =======================================================

PRAGMA foreign_keys = OFF;

DROP TABLE IF EXISTS cancellations;
DROP TABLE IF EXISTS reserved_seats;
DROP TABLE IF EXISTS tickets;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS fare;
DROP TABLE IF EXISTS flight_information;
DROP TABLE IF EXISTS flights;
DROP TABLE IF EXISTS fleet_information;
DROP TABLE IF EXISTS customer_details;
DROP TABLE IF EXISTS users;

PRAGMA foreign_keys = ON;

-- =======================================================
-- TABLES
-- =======================================================

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('admin', 'staff', 'customer')),
    full_name TEXT NOT NULL,
    email TEXT,
    phone TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login DATETIME
);

CREATE TABLE IF NOT EXISTS customer_details (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
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

CREATE TABLE IF NOT EXISTS flights (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    flight_number TEXT UNIQUE NOT NULL,
    flight_name TEXT,
    origin TEXT NOT NULL,
    destination TEXT NOT NULL,
    via TEXT,
    departure_time DATETIME NOT NULL,
    arrival_time DATETIME NOT NULL,
    travel_date DATE NOT NULL,
    aircraft_code TEXT,
    status TEXT DEFAULT 'scheduled' CHECK (status IN ('scheduled', 'boarding', 'departed', 'arrived', 'cancelled', 'delayed')),
    FOREIGN KEY (aircraft_code) REFERENCES fleet_information(aircraft_code) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS flight_information (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
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
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    flight_id INTEGER NOT NULL,
    seat_number TEXT NOT NULL,
    class_code TEXT NOT NULL CHECK (class_code IN ('ECO', 'EXE', 'BUSINESS', 'FIRST')),
    seat_type TEXT CHECK (seat_type IN ('Window', 'Aisle', 'Middle')),
    is_available INTEGER DEFAULT 1,
    UNIQUE(flight_id, seat_number),
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS fare (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
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
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pnr_number TEXT UNIQUE NOT NULL,
    customer_id INTEGER NOT NULL,
    flight_id INTEGER NOT NULL,
    seat_id INTEGER,
    booking_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    travel_date DATE NOT NULL,
    class_code TEXT NOT NULL,
    seat_preference TEXT,
    status TEXT DEFAULT 'confirmed' CHECK (status IN ('confirmed', 'waiting', 'cancelled')),
    base_amount REAL NOT NULL,
    concession_amount REAL DEFAULT 0.00,
    final_amount REAL NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer_details(id) ON DELETE CASCADE,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE,
    FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS tickets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_number TEXT UNIQUE NOT NULL,
    pnr_number TEXT NOT NULL,
    booking_id INTEGER NOT NULL,
    passenger_name TEXT NOT NULL,
    flight_number TEXT NOT NULL,
    origin TEXT NOT NULL,
    destination TEXT NOT NULL,
    travel_date DATE NOT NULL,
    departure_time DATETIME NOT NULL,
    seat_number TEXT,
    class_code TEXT,
    fare REAL NOT NULL,
    issue_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reserved_seats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    f_code TEXT NOT NULL,
    flight_id INTEGER NOT NULL,
    t_res_eco_seat INTEGER DEFAULT 0,
    t_res_exe_seat INTEGER DEFAULT 0,
    t_date DATE NOT NULL,
    waiting_no INTEGER DEFAULT 0,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cancellations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
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
    cancellation_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- =======================================================
-- SAMPLE DATA - USERS
-- =======================================================
INSERT OR IGNORE INTO users (username, password, role, full_name, email, phone)
VALUES 
('admin', 'admin123', 'admin', 'System Administrator', 'admin@jetstream.com', '+26650000001'),
('staff', 'staff123', 'staff', 'John Staff', 'staff@jetstream.com', '+26650000002'),
('customer', 'customer123', 'customer', 'Thabo Mokoena', 'thabo@example.com', '+26650123456'),
('customer2', 'cust234', 'customer', 'Mpho Leballo', 'mpho@example.com', '+26650123457'),
('customer3', 'cust345', 'customer', 'Kabelo Thamae', 'kabelo@example.com', '+26650123458'),
('staff2', 'staff234', 'staff', 'Lerato Staff', 'lerato@jetstream.com', '+26650000003');

-- =======================================================
-- SAMPLE DATA - CUSTOMER DETAILS
-- =======================================================
INSERT OR IGNORE INTO customer_details
(user_id, cust_code, cust_name, father_name, gender, date_of_birth, address, tel_no, profession, concession_type, concession_percentage)
VALUES
((SELECT id FROM users WHERE username='customer'),'CUST001','Thabo Mokoena','Peter Mokoena','Male','1995-05-15','123 Main St, Maseru','+26650123456','Engineer','None',0.00),
((SELECT id FROM users WHERE username='customer2'),'CUST002','Mpho Leballo','Simon Leballo','Female','1998-08-22','45 High St, Maseru','+26650123457','Teacher','Student',10.00),
((SELECT id FROM users WHERE username='customer3'),'CUST003','Kabelo Thamae','Daniel Thamae','Male','1990-12-10','12 Freedom Rd, Maseru','+26650123458','Doctor','Senior Citizen',15.00),
(NULL,'CUST004','Lindiwe Nkosi','Joseph Nkosi','Female','1992-03-18','78 Church St, Maseru','+26650234567','Nurse','None',0.00),
(NULL,'CUST005','Rethabile Moshoeshoe','Samuel Moshoeshoe','Male','1988-07-25','56 Kingsway Rd, Maseru','+26650345678','Accountant','Cancer Patient',20.00);

INSERT OR IGNORE INTO fleet_information
(aircraft_code, no_aircraft, club_pre_capacity, eco_capacity, total_capacity, engine_type, cruise_speed, air_length, wing_span)
VALUES
('B737-800', 'Boeing 737-800', 20, 150, 170, 'CFM56-7B', '850 km/h', '39.5m', '35.8m'),
('E190', 'Embraer 190', 12, 80, 92, 'CF34-10E', '820 km/h', '36.2m', '28.7m'),
('A320', 'Airbus A320', 24, 140, 164, 'CFM56-5B', '830 km/h', '37.6m', '34.1m'),
('B787-9', 'Boeing 787-9 Dreamliner', 30, 220, 250, 'GEnx-1B', '910 km/h', '62.8m', '60.1m'),
('CRJ900', 'Bombardier CRJ900', 8, 68, 76, 'CF34-8C5', '780 km/h', '36.4m', '24.9m'),
('B737-700', 'Boeing 737-700', 18, 130, 148, 'CFM56-7B', '840 km/h', '33.6m', '34.3m');  -- <-- added

-- =======================================================
-- SAMPLE DATA - FLIGHTS
-- =======================================================
INSERT OR IGNORE INTO flights
(flight_number, flight_name, origin, destination, via, departure_time, arrival_time, travel_date, aircraft_code, status)
VALUES
('JS100', 'JetStream Express', 'Johannesburg', 'Maseru', NULL, '2025-11-15 08:30:00', '2025-11-15 09:15:00', '2025-11-15', 'B737-800', 'scheduled'),
('JS101', 'JetStream Connect', 'Maseru', 'Bloemfontein', NULL, '2025-11-16 10:00:00', '2025-11-16 11:00:00', '2025-11-16', 'E190', 'scheduled'),
('JS102', 'JetStream Link', 'Johannesburg', 'Maseru', 'Bloemfontein', '2025-11-17 09:00:00', '2025-11-17 10:45:00', '2025-11-17', 'A320', 'scheduled'),
('JS103', 'JetStream Premium', 'Cape Town', 'Maseru', NULL, '2025-11-18 14:00:00', '2025-11-18 16:30:00', '2025-11-18', 'B787-9', 'boarding'),
('JS104', 'JetStream Shuttle', 'Maseru', 'Durban', NULL, '2025-11-19 07:00:00', '2025-11-19 08:45:00', '2025-11-19', 'CRJ900', 'departed'),
('JS105', 'JetStream Regional', 'Maseru', 'Johannesburg', NULL, '2025-11-20 12:00:00', '2025-11-20 12:45:00', '2025-11-20', 'E190', 'scheduled'),
('JS106', 'JetStream Domestic', 'Bloemfontein', 'Maseru', NULL, '2025-11-21 06:30:00', '2025-11-21 07:15:00', '2025-11-21', 'B737-700', 'scheduled'),
('JS107', 'JetStream Direct', 'Johannesburg', 'Durban', NULL, '2025-11-22 09:00:00', '2025-11-22 10:30:00', '2025-11-22', 'A320', 'scheduled');

-- =======================================================
-- SAMPLE DATA - FLIGHT INFORMATION
-- =======================================================
INSERT OR IGNORE INTO flight_information
(flight_id, f_code, f_name, c_code, t_exe_seatno, t_eco_seatno, available_exe_seats, available_eco_seats)
VALUES
(1, 'JS100', 'JetStream Express', 'ECO', 20, 150, 20, 150),
(1, 'JS100', 'JetStream Express', 'EXE', 20, 150, 20, 150),
(2, 'JS101', 'JetStream Connect', 'ECO', 12, 80, 12, 80),
(2, 'JS101', 'JetStream Connect', 'EXE', 12, 80, 12, 80),
(3, 'JS102', 'JetStream Link', 'ECO', 24, 140, 24, 140),
(3, 'JS102', 'JetStream Link', 'EXE', 24, 140, 24, 140),
(4, 'JS103', 'JetStream Premium', 'ECO', 30, 220, 30, 220),
(4, 'JS103', 'JetStream Premium', 'EXE', 30, 220, 30, 220),
(5, 'JS104', 'JetStream Shuttle', 'ECO', 8, 68, 8, 68),
(5, 'JS104', 'JetStream Shuttle', 'EXE', 8, 68, 8, 68),
(6, 'JS105', 'JetStream Regional', 'ECO', 12, 80, 12, 80),
(6, 'JS105', 'JetStream Regional', 'EXE', 12, 80, 12, 80),
(7, 'JS106', 'JetStream Night', 'ECO', 24, 140, 24, 140),
(7, 'JS106', 'JetStream Night', 'EXE', 24, 140, 24, 140);

-- =======================================================
-- SAMPLE DATA - SEATS
-- =======================================================
-- EXAMPLES: Using recursive CTEs for seats (works in SQLite)
WITH RECURSIVE cnt(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM cnt WHERE n < 150)
INSERT OR IGNORE INTO seats (flight_id, seat_number, class_code, seat_type, is_available)
SELECT 1, printf('E%03d', n), 'ECO', CASE WHEN n%6=1 OR n%6=0 THEN 'Window' WHEN n%6=2 OR n%6=5 THEN 'Aisle' ELSE 'Middle' END, 1
FROM cnt;

WITH RECURSIVE cnt(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM cnt WHERE n < 20)
INSERT OR IGNORE INTO seats (flight_id, seat_number, class_code, seat_type, is_available)
SELECT 1, printf('X%03d', n), 'EXE', CASE WHEN n%4=1 OR n%4=0 THEN 'Window' WHEN n%4=2 THEN 'Aisle' ELSE 'Middle' END, 1
FROM cnt;

-- Additional flights JS101-JS104 seats can be generated similarly using recursive CTEs

-- =======================================================
-- TRIGGERS
-- =======================================================
-- Update seat availability after booking
CREATE TRIGGER IF NOT EXISTS update_seat_availability_on_booking
AFTER INSERT ON bookings
WHEN NEW.seat_id IS NOT NULL
BEGIN
    UPDATE seats SET is_available = 0 WHERE id = NEW.seat_id;
END;

-- Update seat availability on cancellation
CREATE TRIGGER IF NOT EXISTS update_seat_availability_on_cancel
AFTER UPDATE ON bookings
WHEN NEW.status='cancelled' AND OLD.status!='cancelled' AND NEW.seat_id IS NOT NULL
BEGIN
    UPDATE seats SET is_available = 1 WHERE id = NEW.seat_id;
END;

-- Create cancellation record on booking cancellation
CREATE TRIGGER IF NOT EXISTS create_cancellation_record
AFTER UPDATE ON bookings
WHEN NEW.status='cancelled' AND OLD.status!='cancelled'
BEGIN
    INSERT INTO cancellations (pnr_number, booking_id, cust_code, class, seat_no, basic_amount, cancel_amount, refund_amount, reason)
    SELECT NEW.pnr_number, NEW.id, cd.cust_code, NEW.class_code, s.seat_number, NEW.final_amount, NEW.final_amount*0.2, NEW.final_amount*0.8, 'Booking cancelled'
    FROM customer_details cd JOIN seats s ON s.id = NEW.seat_id WHERE cd.id = NEW.customer_id;
END;
