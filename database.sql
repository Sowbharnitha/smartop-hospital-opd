-- =======================================================
-- SmartOP: Hospital OPD Queue Management System
-- Database Schema & Sample Data Script
-- Compatible with Local MySQL & Cloud MySQL (Railway, Aiven, TiDB, Render)
-- =======================================================

-- Create database if running on local server (safe on cloud if permissions exist)
CREATE DATABASE IF NOT EXISTS smartop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Safely drop existing tables in reverse dependency order before recreating
DROP TABLE IF EXISTS queue;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS doctors;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS users;

-- -------------------------------------------------------
-- Table: users
-- Core credentials and basic contact details for all roles
-- -------------------------------------------------------
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('PATIENT', 'DOCTOR', 'ADMIN') NOT NULL,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role)
) ENGINE=InnoDB;

-- -------------------------------------------------------
-- Table: departments
-- Hospital clinical departments
-- -------------------------------------------------------
CREATE TABLE departments (
    department_id INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    INDEX idx_dept_status (status)
) ENGINE=InnoDB;

-- -------------------------------------------------------
-- Table: doctors
-- Doctor profiles linked to user and department
-- -------------------------------------------------------
CREATE TABLE doctors (
    doctor_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    department_id INT NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    availability VARCHAR(100) DEFAULT '9:00 AM - 1:00 PM, 2:00 PM - 5:00 PM',
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(department_id),
    INDEX idx_doc_dept (department_id),
    INDEX idx_doc_status (status)
) ENGINE=InnoDB;

-- -------------------------------------------------------
-- Table: patients
-- Patient medical profile linked to user
-- -------------------------------------------------------
CREATE TABLE patients (
    patient_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    date_of_birth DATE NOT NULL,
    gender ENUM('MALE', 'FEMALE', 'OTHER') NOT NULL,
    address TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -------------------------------------------------------
-- Table: appointments
-- OPD appointment bookings
-- -------------------------------------------------------
CREATE TABLE appointments (
    appointment_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    department_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(department_id),
    INDEX idx_app_doctor_date (doctor_id, appointment_date),
    INDEX idx_app_patient (patient_id),
    INDEX idx_app_status (status)
) ENGINE=InnoDB;

-- -------------------------------------------------------
-- Table: queue
-- Real-time OPD queue tracking per doctor per date
-- -------------------------------------------------------
CREATE TABLE queue (
    queue_id INT AUTO_INCREMENT PRIMARY KEY,
    appointment_id INT NOT NULL UNIQUE,
    doctor_id INT NOT NULL,
    patient_id INT NOT NULL,
    queue_number VARCHAR(20) NOT NULL,
    queue_position INT NOT NULL,
    queue_date DATE NOT NULL,
    status ENUM('WAITING', 'CALLED', 'IN_CONSULTATION', 'COMPLETED', 'SKIPPED', 'CANCELLED') DEFAULT 'WAITING',
    called_at TIMESTAMP NULL,
    consultation_started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id) ON DELETE CASCADE,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE,
    INDEX idx_queue_lookup (doctor_id, queue_date, status),
    INDEX idx_queue_patient (patient_id, queue_date),
    UNIQUE KEY uq_doc_date_qnum (doctor_id, queue_date, queue_number)
) ENGINE=InnoDB;

-- =======================================================
-- SAMPLE DATA INSERTION
-- Password hashes (SHA-256 with salt 'SmartOP@2026'):
-- admin123  -> 696b6b888e7ea34ee28aa40d529f381f5a71a62ef07715edec7270ee56efd572
-- doctor123 -> db47621481aacb44a82407d7e0f2be7a2274df4e0101423d6f3517a9f24ca934
-- patient123-> e00ddb5ad73e7827cb27749a436b5c052ff31d2695fad8e8bf90e8c593e7a5fa
-- =======================================================

-- 1. Departments
INSERT INTO departments (department_id, department_name, description, status) VALUES
(1, 'Cardiology', 'Heart health, ECG, echocardiograms, and cardiovascular interventions', 'ACTIVE'),
(2, 'General Medicine', 'Primary care, diagnostic assessments, adult healthcare and preventative medicine', 'ACTIVE'),
(3, 'Pediatrics', 'Comprehensive child care, neonatal monitoring, vaccinations and development', 'ACTIVE'),
(4, 'Orthopedics', 'Bone, joint, musculoskeletal trauma, spine care and sports injuries', 'ACTIVE'),
(5, 'Dermatology', 'Skin disorders, allergies, cosmetic dermatology, and dermatopathology', 'ACTIVE'),
(6, 'ENT', 'Ear, Nose, Throat conditions, hearing loss and sinus treatment', 'ACTIVE'),
(7, 'Neurology', 'Disorders of the nervous system, brain, spinal cord and nerves', 'ACTIVE');

-- 2. Users (Admin, 5 Doctors, 10 Patients)
-- Admin
INSERT INTO users (user_id, name, email, password, role, phone) VALUES
(1, 'Hospital Administrator', 'admin@smartop.com', '696b6b888e7ea34ee28aa40d529f381f5a71a62ef07715edec7270ee56efd572', 'ADMIN', '9876543200');

-- 5 Doctors
INSERT INTO users (user_id, name, email, password, role, phone) VALUES
(2, 'Dr. Arun Kumar', 'doctor@smartop.com', 'db47621481aacb44a82407d7e0f2be7a2274df4e0101423d6f3517a9f24ca934', 'DOCTOR', '9876543201'),
(3, 'Dr. Priya Sharma', 'priya.sharma@smartop.com', 'db47621481aacb44a82407d7e0f2be7a2274df4e0101423d6f3517a9f24ca934', 'DOCTOR', '9876543202'),
(4, 'Dr. Karthik Raja', 'karthik.raja@smartop.com', 'db47621481aacb44a82407d7e0f2be7a2274df4e0101423d6f3517a9f24ca934', 'DOCTOR', '9876543203'),
(5, 'Dr. Meena Sundaram', 'meena.sundaram@smartop.com', 'db47621481aacb44a82407d7e0f2be7a2274df4e0101423d6f3517a9f24ca934', 'DOCTOR', '9876543204'),
(6, 'Dr. Rajesh Varma', 'rajesh.varma@smartop.com', 'db47621481aacb44a82407d7e0f2be7a2274df4e0101423d6f3517a9f24ca934', 'DOCTOR', '9876543205');

-- 10 Patients
INSERT INTO users (user_id, name, email, password, role, phone) VALUES
(7, 'Sowbharnitha', 'patient@smartop.com', 'e00ddb5ad73e7827cb27749a436b5c052ff31d2695fad8e8bf90e8c593e7a5fa', 'PATIENT', '9876543210'),
(8, 'Rahul Sharma', 'rahul.sharma@example.com', 'e00ddb5ad73e7827cb27749a436b5c052ff31d2695fad8e8bf90e8c593e7a5fa', 'PATIENT', '9876543211'),
(9, 'Priya Nair', 'priya.nair@example.com', 'e00ddb5ad73e7827cb27749a436b5c052ff31d2695fad8e8bf90e8c593e7a5fa', 'PATIENT', '9876543212'),
(10, 'Karthik Venkat', 'karthik.v@example.com', 'e00ddb5ad73e7827cb27749a436b5c052ff31d2695fad8e8bf90e8c593e7a5fa', 'PATIENT', '9876543213'),
(11, 'Meena Krishnan', 'meena.k@example.com', 'e00ddb5ad73e7827cb27749a436b5c052ff31d2695fad8e8bf90e8c593e7a5fa', 'PATIENT', '9876543214'),
(12, 'Arun Prakash', 'arun.p@example.com', 'e00ddb5ad73e7827cb27749a436b5c052ff31d2695fad8e8bf90e8c593e7a5fa', 'PATIENT', '9876543215'),
(13, 'Deepa Raman', 'deepa.r@example.com', 'e00ddb5ad73e7827cb27749a436b5c052ff31d2695fad8e8bf90e8c593e7a5fa', 'PATIENT', '9876543216'),
(14, 'Suresh Kumar', 'suresh.k@example.com', 'e00ddb5ad73e7827cb27749a436b5c052ff31d2695fad8e8bf90e8c593e7a5fa', 'PATIENT', '9876543217'),
(15, 'Ananya Das', 'ananya.d@example.com', 'e00ddb5ad73e7827cb27749a436b5c052ff31d2695fad8e8bf90e8c593e7a5fa', 'PATIENT', '9876543218'),
(16, 'Vikram Patel', 'vikram.p@example.com', 'e00ddb5ad73e7827cb27749a436b5c052ff31d2695fad8e8bf90e8c593e7a5fa', 'PATIENT', '9876543219');

-- 3. Doctors Table Details
INSERT INTO doctors (doctor_id, user_id, department_id, specialization, availability, status) VALUES
(1, 2, 1, 'Senior Interventional Cardiologist', '09:00 AM - 01:00 PM', 'ACTIVE'),
(2, 3, 3, 'Consultant Pediatrician & Neonatologist', '10:00 AM - 02:00 PM', 'ACTIVE'),
(3, 4, 2, 'Physician & Diabetologist', '09:00 AM - 04:00 PM', 'ACTIVE'),
(4, 5, 5, 'Dermatologist & Cosmetologist', '11:00 AM - 03:00 PM', 'ACTIVE'),
(5, 6, 4, 'Joint Replacement & Spine Specialist', '08:30 AM - 12:30 PM', 'ACTIVE');

-- 4. Patients Table Details
INSERT INTO patients (patient_id, user_id, date_of_birth, gender, address) VALUES
(1, 7, '2001-05-14', 'FEMALE', 'Flat 402, Green Meadows, Anna Nagar, Chennai'),
(2, 8, '1995-08-22', 'MALE', '12 Cross Cut Road, Gandhipuram, Coimbatore'),
(3, 9, '1998-11-03', 'FEMALE', '56 Lake View Street, T Nagar, Chennai'),
(4, 10, '1990-02-18', 'MALE', '89 Alagappa Colony, Madurai'),
(5, 11, '1988-07-29', 'FEMALE', '23 Temple Ring Road, Trichy'),
(6, 12, '1985-04-12', 'MALE', '44 Hill View Lane, Salem'),
(7, 13, '1992-09-09', 'FEMALE', '17 Heritage Enclave, Vellore'),
(8, 14, '1979-12-30', 'MALE', '78 Central Avenue, Erode'),
(9, 15, '2003-01-25', 'FEMALE', '93 Beach Boulevard, Pondicherry'),
(10, 16, '1987-06-15', 'MALE', '61 North Bypass Road, Tirunelveli');

-- 5. Appointments for Today (CURDATE())
-- Appointments for Dr. Arun (Doctor 1, Cardiology)
INSERT INTO appointments (appointment_id, patient_id, doctor_id, department_id, appointment_date, appointment_time, status) VALUES
(1, 2, 1, 1, CURDATE(), '09:00:00', 'COMPLETED'),
(2, 3, 1, 1, CURDATE(), '09:30:00', 'COMPLETED'),
(3, 4, 1, 1, CURDATE(), '10:00:00', 'IN_PROGRESS'),
(4, 1, 1, 1, CURDATE(), '10:30:00', 'SCHEDULED'),
(5, 5, 1, 1, CURDATE(), '11:00:00', 'SCHEDULED'),
-- Appointments for Dr. Priya Sharma (Doctor 2, Pediatrics)
(6, 6, 2, 3, CURDATE(), '10:00:00', 'IN_PROGRESS'),
(7, 7, 2, 3, CURDATE(), '10:30:00', 'SCHEDULED'),
-- Appointment for Dr. Karthik Raja (Doctor 3, General Medicine)
(8, 8, 3, 2, CURDATE(), '09:30:00', 'SCHEDULED');

-- 6. Queue entries for Today's Appointments
INSERT INTO queue (queue_id, appointment_id, doctor_id, patient_id, queue_number, queue_position, queue_date, status, called_at, consultation_started_at, completed_at) VALUES
(1, 1, 1, 2, 'A-001', 1, CURDATE(), 'COMPLETED', DATE_SUB(NOW(), INTERVAL 45 MINUTE), DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(2, 2, 1, 3, 'A-002', 2, CURDATE(), 'COMPLETED', DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 18 MINUTE), DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(3, 3, 1, 4, 'A-003', 3, CURDATE(), 'IN_CONSULTATION', DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 3 MINUTE), NULL),
(4, 4, 1, 1, 'A-004', 4, CURDATE(), 'WAITING', NULL, NULL, NULL),
(5, 5, 1, 5, 'A-005', 5, CURDATE(), 'WAITING', NULL, NULL, NULL),
(6, 6, 2, 6, 'P-001', 1, CURDATE(), 'IN_CONSULTATION', DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 8 MINUTE), NULL),
(7, 7, 2, 7, 'P-002', 2, CURDATE(), 'WAITING', NULL, NULL, NULL),
(8, 8, 3, 8, 'K-001', 1, CURDATE(), 'WAITING', NULL, NULL, NULL);
