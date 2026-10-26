**Joinify - Event Management System**

**Overview:**
Organizations and individuals often struggle with managing events efficiently, especially when it comes to organizing invitations, tracking RSVPs, sending reminders, and managing attendees. Joinify is a web-based Event Management System that lets users create, browse, and RSVP to events with role-based access, using a Java Spring Boot backend and a minimal HTML/CSS/JS frontend.

**Objective:**
To develop a RESTful Event Management System where:

  * Organizers can create, update, and manage events.
  * Attendees can browse events, RSVP to them, and receive reminders.
  * The system supports filtering, status tracking (upcoming/past), and secure access using authentication and role-based authorization.

**Key Features:**

**User Roles:**
  * Organizer: Can create, edit, and delete events; open/close registration; view attendees and export them to Excel; send reminders.
  * Attendee: Can browse and search upcoming events, RSVP, cancel an RSVP, and view their event history.

**Event Functionality:**
* Create, update, delete events with fields like:
  - Title, Description
  - Date and Time
  - Location
  - Max Capacity
  - Banner image URL and Fee
* Filter events into Upcoming and Past based on current date.
* Open or close registration for an event.
* RSVP Tracking:
  - Attendees can RSVP to events and cancel their RSVP.
  - Organizers can view the attendee list of their own events and export it as an Excel file.
* Prevent overbooking:
  - Max capacity is enforced, including when two people book the last spot at the same time (row lock on the event).
  - One RSVP per user per event (unique constraint).
  - Capacity cannot be reduced below the current number of confirmed RSVPs.
* Email Notifications:
  - Attendees are notified when an event they registered for is updated.
  - Automatic reminder email 24 hours before the event starts.
  - Organizers can send a manual reminder with a custom message.
  - The organizer is informed if an email could not be delivered to some attendees.
  - Emails are sent in the background, after the change is saved.

**Security:**
* JWT-based authentication with role-based authorization (Organizer / Attendee).
* Organizers can only manage and view attendees of their own events.
* Request/response DTOs, so passwords and internal fields are never returned by the API.
* Input validation with clear error messages, and consistent JSON error responses.
* Secrets (database, mail, JWT) are kept out of the code.
* CORS restricted to the app's own origin; user content is escaped in the frontend (XSS protection).

**Technical:**
* **Backend**: Java 17, Spring Boot 3.5, Spring Security, JWT, Spring Data JPA/Hibernate, RESTful API
* **Email**: Spring Mail (Gmail SMTP) with Thymeleaf templates, @Async + scheduled jobs
* **API Docs**: Swagger UI (springdoc-openapi)
* **Frontend**: HTML, CSS, and JavaScript (Chart.js for charts, ExcelJS for Excel export)
* **Database**: MySQL

**Running Locally:**
1. Create the database in MySQL:
   `CREATE DATABASE event_management_db;`
2. Copy `src/main/resources/application-secrets.properties.example` to `application-secrets.properties` (same folder) and fill in your MySQL password, a JWT secret, and Gmail address + app password. This file is ignored by Git.
3. Run the app: `mvnw.cmd spring-boot:run` (Windows) or `./mvnw spring-boot:run`
4. Open the app at `http://localhost:8080`
5. API documentation: `http://localhost:8080/swagger-ui.html`
