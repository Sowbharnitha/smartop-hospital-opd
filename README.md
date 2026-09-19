# SmartOP – Hospital OPD Queue Management System

SmartOP is a full-stack Hospital Outpatient Department (OPD) queue management web application designed to digitize patient appointments, eliminate waiting room chaos, and provide real-time visibility into consultation queues.

The system addresses the common problem of patients waiting blindly in hospital waiting halls by calculating exact **queue token numbers**, **current queue positions**, and the **number of patients ahead**, while notifying patients when their doctor starts consultation.

---

## 1. Technology Stack (Zero-Framework Architecture)

Strictly constructed using Core Java and Native Web Standards without external frameworks (no Spring, Spring Boot, Hibernate, Node.js, Express, React, Angular, Bootstrap, or Tailwind):

- **Frontend**:
  - **HTML5**: Semantic document structure
  - **CSS3**: Custom clinical design system, flexbox, CSS grids, glassmorphism status badges, responsive media queries
  - **Vanilla JavaScript**: Fetch API, token management, dynamic DOM manipulation, polling engine
- **Backend**:
  - **Core Java (JDK 21)**: Built-in `com.sun.net.httpserver.HttpServer`
  - **Java OOP**: Encapsulated models, DAO patterns, service business logic
  - **Java Collections**: `List`, `Map`, `ConcurrentHashMap` for session caching
  - **JDBC**: Direct MySQL database transactions using `PreparedStatement` and `ResultSet`
  - **Cryptography**: Native Java `MessageDigest` SHA-256 with application salting
  - **Zero-Dependency JSON**: Self-contained recursive-descent JSON parser and serializer (`JsonUtil.java`)
- **Database**:
  - **MySQL 8.0**: Relational database with foreign keys, indexes, and unique constraints
  - **Connector**: Official MySQL Connector/J (`mysql-connector-j.jar` in `backend/lib/`)

---

## 2. System Architecture

```
+-------------------------------------------------------------------------------+
|                            Web Browser (Client)                               |
|  - HTML5 / CSS3 Healthcare UI                                                 |
|  - Vanilla JavaScript (api.js, auth.js, patient.js, doctor.js, admin.js)       |
|  - Polling Engine (every 5 seconds)                                           |
+-------------------------------------------------------------------------------+
                                      |
                     REST-style HTTP API & Static Assets
                                      v
+-------------------------------------------------------------------------------+
|                     Core Java HTTP Server (Port 8080)                         |
|  - SmartOPServer (com.sun.net.httpserver.HttpServer)                          |
|  - StaticFileHandler (Serves index.html, CSS, JS directly)                    |
|  - REST Handlers: AuthHandler, DepartmentHandler, DoctorHandler,              |
|                   AppointmentHandler, QueueHandler, AdminHandler              |
+-------------------------------------------------------------------------------+
                                      |
                     Business Logic & Services (Core Java)
|  - AuthService (Salted SHA-256 password hashing & validation)                 |
|  - AppointmentService (Validation, doctor schedule, queue coordination)       |
|  - QueueService (Doctor-specific daily sequential queue: A-001, A-002, etc.)  |
|  - DoctorService & AdminService (CRUD operations, KPI aggregations)           |
+-------------------------------------------------------------------------------+
                                      |
                           Data Access Layer (JDBC)
|  - DatabaseConnection (Thread-safe singleton connection manager)              |
|  - UserDAO, PatientDAO, DoctorDAO, DepartmentDAO, AppointmentDAO, QueueDAO    |
+-------------------------------------------------------------------------------+
                                      |
                                    JDBC
                                      v
+-------------------------------------------------------------------------------+
|                          MySQL Database (smartop_db)                          |
|  - users, departments, doctors, patients, appointments, queue                 |
+-------------------------------------------------------------------------------+
```

---

## 3. Project Structure

```
smartop/
├── database.sql               # Complete MySQL schema & seed data
├── build.bat                  # Root Windows build script
├── run.bat                    # Root Windows run script (starts server & opens browser)
├── README.md                  # Comprehensive technical documentation
│
├── backend/
│   ├── config.properties      # Server port & MySQL credentials
│   ├── build.bat              # Backend compilation script
│   ├── run.bat                # Backend execution script
│   ├── lib/
│   │   └── mysql-connector-j.jar # Official MySQL JDBC Driver
│   └── src/
│       ├── config/
│       │   └── DatabaseConfig.java
│       ├── db/
│       │   └── DatabaseConnection.java
│       ├── model/
│       │   ├── User.java
│       │   ├── Patient.java
│       │   ├── Doctor.java
│       │   ├── Department.java
│       │   ├── Appointment.java
│       │   └── QueueEntry.java
│       ├── dao/
│       │   ├── UserDAO.java
│       │   ├── PatientDAO.java
│       │   ├── DoctorDAO.java
│       │   ├── DepartmentDAO.java
│       │   ├── AppointmentDAO.java
│       │   └── QueueDAO.java
│       ├── service/
│       │   ├── AuthService.java
│       │   ├── DepartmentService.java
│       │   ├── DoctorService.java
│       │   ├── AppointmentService.java
│       │   ├── QueueService.java
│       │   └── AdminService.java
│       ├── handler/
│       │   ├── BaseHandler.java
│       │   ├── AuthHandler.java
│       │   ├── DepartmentHandler.java
│       │   ├── DoctorHandler.java
│       │   ├── AppointmentHandler.java
│       │   ├── QueueHandler.java
│       │   ├── AdminHandler.java
│       │   └── StaticFileHandler.java
│       ├── util/
│       │   ├── JsonUtil.java
│       │   ├── PasswordUtil.java
│       │   └── QueueNumberGenerator.java
│       └── server/
│           └── SmartOPServer.java
│
└── frontend/
    ├── index.html             # Landing page with hero, live preview, role portals
    ├── patient/
    │   ├── login.html         # Patient login portal
    │   ├── register.html      # Patient registration with validation
    │   ├── dashboard.html     # Real-time token focus card, history table
    │   ├── appointment.html   # Dynamic department -> doctor booking form
    │   └── queue.html         # Full-screen live OPD waiting room monitor
    ├── doctor/
    │   ├── login.html         # Doctor login portal
    │   └── dashboard.html     # Call next patient, consultation console, queue table
    ├── admin/
    │   ├── login.html         # Administrator login portal
    │   └── dashboard.html     # System statistics, Doctor CRUD, Dept CRUD, Live queues
    ├── css/
    │   ├── style.css          # Healthcare design system & utility classes
    │   ├── login.css          # Authentication card layouts & autofill helper
    │   ├── dashboard.css      # Sidebar layout, cards, badges, modals, data tables
    │   └── responsive.css     # Media queries for mobile, tablet, desktop
    └── js/
        ├── api.js             # Centralized fetch wrapper & toast notifications
        ├── auth.js            # Login, registration, role validation, session check
        ├── patient.js         # Patient dashboard, appointment booking flow
        ├── doctor.js          # Doctor queue actions & consultation state machine
        ├── admin.js           # Admin stats, doctor/dept CRUD modals, queue monitor
        └── queue.js           # 5-second polling engine & audio-visual chime cues
```

---

## 4. Database Setup (MySQL)

### Step 1: Verify MySQL Service
Ensure your MySQL Server service (`MySQL80`) is running on port `3306`.

### Step 2: Import Schema and Sample Data
Open PowerShell or Command Prompt in the project directory:

```powershell
Get-Content database.sql | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -proot@123
```
*(Or in MySQL Command Line Client: `SOURCE database.sql;`)*

This creates `smartop_db` with:
- 6 normalized tables (`users`, `departments`, `doctors`, `patients`, `appointments`, `queue`)
- 7 departments (Cardiology, Pediatrics, General Medicine, Orthopedics, Dermatology, ENT, Neurology)
- 5 specialist doctors with mapped credentials
- 10 registered patients with medical profiles
- Active OPD appointments and queue entries for today's date

---

## 5. Configuration

Edit `backend/config.properties` if your local MySQL settings differ:

```properties
server.port=8080
db.url=jdbc:mysql://localhost:3306/smartop_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.username=root
db.password=root@123
```

---

## 6. How to Run the Application

### Option A: Using One-Click Batch Scripts (Windows)
1. **Compile Backend**:
   Double click `build.bat` or run:
   ```cmd
   build.bat
   ```
2. **Start Server & Launch Application**:
   Double click `run.bat` or run:
   ```cmd
   run.bat
   ```
   This starts the server on port `8080` and opens `http://localhost:8080/` in your browser.

### Option B: Manual Command Line Execution
```powershell
# 1. Compile
cd backend
javac -encoding UTF-8 -cp "lib/*" -d bin src/config/*.java src/db/*.java src/util/*.java src/model/*.java src/dao/*.java src/service/*.java src/handler/*.java src/server/*.java

# 2. Run Server
java -cp "bin;lib/*" server.SmartOPServer
```

Then open your browser to: **`http://localhost:8080/`**

---

## 7. Demo Credentials

Quick "Fill Demo" buttons are provided on all login screens for one-click testing:

| Role | Portal URL | Demo Email | Password | Details |
| :--- | :--- | :--- | :--- | :--- |
| **Patient** | `/patient/login.html` | `patient@smartop.com` | `patient123` | Sowbharnitha (Token `A-004` in Cardiology) |
| **Doctor** | `/doctor/login.html` | `doctor@smartop.com` | `doctor123` | Dr. Arun Kumar (Cardiology Department) |
| **Doctor 2**| `/doctor/login.html` | `priya.sharma@smartop.com` | `doctor123` | Dr. Priya Sharma (Pediatrics Department) |
| **Admin** | `/admin/login.html` | `admin@smartop.com` | `admin123` | Hospital Administrator |

---

## 8. Queue Logic & Business Rules

### A. Queue Token Generation
- **Prefix**: Extracted from the doctor's name after title stripping (e.g. `Dr. Arun Kumar` -> `A`, `Dr. Priya Sharma` -> `P`, `Dr. Karthik Raja` -> `K`).
- **Sequence**: Database query `COALESCE(MAX(queue_position), 0) + 1` for the specified `doctor_id` and `queue_date`.
- **Format**: `String.format("%s-%03d", prefix, sequenceNumber)` producing clean tokens like `A-001`, `A-002`, `A-004`.
- **Concurrency & Duplicate Prevention**: Calculated inside an atomic database transaction with `UNIQUE (doctor_id, queue_date, queue_number)`.

### B. Queue Position & "Patients Ahead" Calculation
- **Queue Position**: The sequential arrival index for the day (e.g., Position #4).
- **Patients Ahead**: The live count of all patients assigned to the same doctor on the same date whose `queue_position < this_patient_position` AND whose status is active:
  $$\text{Patients Ahead} = \text{COUNT}(queue\_position < N \text{ AND } status \in (\text{'WAITING'}, \text{'CALLED'}, \text{'IN\_CONSULTATION'}))$$
- As earlier patients complete consultations or are skipped, the "Patients Ahead" count automatically decreases in real-time.

### C. Doctor Consultation State Machine
1. **WAITING**: Patient has arrived / registered.
2. **CALLED**: Doctor clicks "Call Next Patient". Web Audio announcement chime plays.
3. **IN_CONSULTATION**: Doctor clicks "Start Consultation". Associated appointment transitions to `IN_PROGRESS`.
4. **COMPLETED**: Doctor clicks "Complete Consultation". Associated appointment transitions to `COMPLETED`. Next waiting patient becomes eligible.
5. **SKIPPED**: If patient does not show up, doctor marks token as `SKIPPED`.

---

## 9. Real-Time Polling Implementation

Because external WebSocket dependencies are avoided, real-time synchronization is achieved via clean **client-side polling**:
- The client JavaScript (`queue.js` and `patient.js`) requests `GET /api/queue/patient/{id}` every **5,000 milliseconds (5s)**.
- If the patient status transitions (e.g. from `WAITING` to `CALLED`), the UI dynamically updates the badge, applies a glowing notification border, and triggers an synthesized Web Audio tone.
- Doctor and Admin dashboards poll `/api/doctors/{id}/dashboard` and `/api/admin/queues` every 5 seconds to reflect newly booked appointments and status changes across all rooms.

---

## 10. REST API Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticates user and returns role profile + session token |
| `POST` | `/api/auth/register` | Registers new patient with phone/email validation |
| `POST` | `/api/auth/logout` | Invalidates active session |
| `GET` | `/api/auth/me` | Validates session Bearer token |
| `GET` | `/api/departments` | Returns list of active departments (or all with `?all=true`) |
| `POST` | `/api/departments` | Admin creates clinical department |
| `PUT` | `/api/departments/{id}` | Admin updates department |
| `DELETE`| `/api/departments/{id}` | Admin deactivates department |
| `GET` | `/api/doctors` | Returns all doctors (or filtered by `?departmentId={id}`) |
| `GET` | `/api/doctors/{id}` | Returns doctor details |
| `GET` | `/api/doctors/{id}/dashboard` | Returns doctor's today queue, active patient, KPI counts |
| `POST` | `/api/doctors` | Admin creates new doctor profile with user credentials |
| `PUT` | `/api/doctors/{id}` | Admin updates doctor details |
| `DELETE`| `/api/doctors/{id}` | Admin deactivates doctor profile |
| `POST` | `/api/appointments` | Books appointment & generates queue token in MySQL |
| `GET` | `/api/appointments/patient/{id}` | Returns patient appointment history |
| `GET` | `/api/appointments/doctor/{id}` | Returns doctor appointments for specified date |
| `GET` | `/api/queue?doctorId={id}&date={date}` | Returns today's queue list for doctor |
| `GET` | `/api/queue/patient/{id}` | Returns active queue token and "patients ahead" for patient |
| `POST` | `/api/queue/doctor/{id}/call-next` | Doctor calls earliest waiting patient |
| `PUT` | `/api/queue/{id}/start` | Transitions patient status to `IN_CONSULTATION` |
| `PUT` | `/api/queue/{id}/complete` | Transitions patient status to `COMPLETED` |
| `PUT` | `/api/queue/{id}/skip` | Transitions patient status to `SKIPPED` |
| `GET` | `/api/admin/statistics` | Returns aggregated statistics across all clinics |
| `GET` | `/api/admin/queues` | Returns live active queue overview across all doctors |

---

## 11. Screenshots Placeholder

*(Place browser screenshots of the Landing Page, Patient Dashboard, Doctor Consultation Room, and Admin Monitoring Hub here when compiling project reports).*

---

## 12. Known Limitations & Future Enhancements

- **SMS / WhatsApp Integration**: In production, queue alerts can be delivered via SMS when the patient has 2 turns ahead.
- **Multi-Counter Support**: Can be extended to support lab test collection, pharmacy dispensary, and billing counters.
- **Hardware Integration**: Display screens in waiting halls can run the `/patient/queue.html` view in kiosk mode with an external token display bell.

---

## 13. Public Cloud Deployment Guide (One Unified Shareable HTTPS URL)

SmartOP is designed with a **Unified Architecture**: the Core Java HTTP server serves both the **REST API** (`/api/*`) and the **Frontend Web Application** (`/`, `/patient/*`, `/doctor/*`, `/admin/*`) from the same port.

When deployed to a cloud container, you receive **ONE single shareable public HTTPS URL** (e.g. `https://smartop-production.up.railway.app` or `https://smartop.onrender.com`).

---

### Option 1: Railway Deployment (Recommended – All-In-One Platform)

Railway can host both the **Core Java Backend (via Dockerfile)** and the **Managed MySQL Database** on the same dashboard with automatic private networking.

#### Step 1: Push your Code to GitHub
Run these commands from your local `smartop` folder:
```bash
git add .
git commit -m "Prepare SmartOP for cloud deployment with Docker and env config"
git push origin main
```

#### Step 2: Create a New Project on Railway
1. Go to [railway.com](https://railway.com) and log in with your GitHub account.
2. Click **New Project** → **Provision MySQL**.
   - Railway immediately provisions an active Cloud MySQL database.
3. Click on the MySQL card in Railway, go to the **Variables** tab to view your credentials (`MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE`).

#### Step 3: Import `database.sql` into Railway MySQL
1. In the Railway MySQL card, click on the **Connect** tab to view the public connection command.
2. Run this command in your local terminal to import all tables and sample data into Railway:
   ```bash
   mysql -h <MYSQLHOST> -u <MYSQLUSER> -p<MYSQLPASSWORD> -P <MYSQLPORT> <MYSQLDATABASE> < database.sql
   ```
   *(Or connect via MySQL Workbench / DBeaver using the Railway host, port, user, and password, then paste and execute `database.sql`).*

#### Step 4: Deploy the SmartOP Java Application
1. In the same Railway project, click **+ New Service** → **GitHub Repo**.
2. Select your `smartop-hospital-opd` repository.
3. Railway automatically detects the `Dockerfile`, compiles the Core Java code, and packages the frontend.
4. Go to the newly created service → **Variables** tab → click **Add Reference Variable**:
   - Reference `DATABASE_URL` from your MySQL service.
   *(Or add individual variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` referencing the MySQL service).*
5. Go to the **Settings** tab → under **Networking**, click **Generate Domain**.
6. Railway generates your public HTTPS URL (e.g. `https://smartop-production.up.railway.app`).

**That's it!** Anyone can open this single URL on their phone or laptop, register as a patient, book an appointment, view their queue token, and doctors can manage consultations live online.

---

### Option 2: Render + Aiven for MySQL (100% Free Forever)

Render provides a free containerized Web Service, and Aiven provides a permanently free managed MySQL database without requiring a credit card.

#### Step 1: Create Free Cloud MySQL on Aiven
1. Sign up at [aiven.io](https://aiven.io).
2. Click **Create Service** → select **MySQL** → choose the **Free Plan**.
3. Once active, copy the **Host**, **Port**, **User**, **Password**, and **Database Name** (`defaultdb`).
4. Connect using MySQL Workbench or command line and run `database.sql`.

#### Step 2: Deploy on Render
1. Sign up at [render.com](https://render.com).
2. Click **New +** → **Web Service**.
3. Connect your GitHub repository `smartop-hospital-opd`.
4. Choose **Docker** as the runtime environment.
5. In the **Environment Variables** section, add:
   - `DB_HOST`: `<Your Aiven Host>`
   - `DB_PORT`: `<Your Aiven Port>`
   - `DB_NAME`: `defaultdb`
   - `DB_USER`: `avnadmin`
   - `DB_PASSWORD`: `<Your Aiven Password>`
6. Click **Create Web Service**.
7. Render builds the Docker image and gives you your public HTTPS URL:
   `https://smartop.onrender.com`

---

### How to Update the Live Application After Future GitHub Commits
Both Railway and Render feature **Continuous Deployment**:
Whenever you make changes to Java, HTML, CSS, or JavaScript:
```bash
git add .
git commit -m "Add new hospital feature"
git push origin main
```
The cloud platform automatically rebuilds the Docker container and updates your live URL in under 2 minutes with zero downtime.

