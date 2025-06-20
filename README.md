**Joinify - Event Management System**

**For video demo and screenshots**
Please access from here: https://drive.google.com/drive/folders/1U3r9rAX_chI6jDnSxrEufttw2a2Bq4Ne?usp=sharing

## **Overview:**
Organizations and individuals often struggle with managing events efficiently—especially when it comes to organizing invitations, tracking RSVPs, sending reminders, and managing attendees. This project aims to build a web-based Event Management System that allows users to create, browse, and RSVP to events with role-based access, using a Java Spring Boot backend and minimal HTML/CSS/JS frontend.

**Objective:**
To develop a RESTful Event Management System where:

  * Organizers can create, update, and manage events.
  * Attendees can browse events, RSVP to them, and receive reminders.
  * The system should support filtering, status tracking (upcoming/past), and secure access using authentication and role-based authorization.

**Technical Stack:**
* **Backend**: Spring Boot, JPA/Hibernate, RESTful API, JWT-based security.
* **Frontend**: HTML, CSS, and JavaScript
* **Database**: MySQL

## **Key Features:**
**User Roles:**
  * Organizer: Can create, edit, and delete events; view RSVPs for their events, download (CSV) list of attendees, send reminders
  * Attendee: Can view and RSVP to available events.

**Event Functionality:**
* Create, update, delete events with fields like:
  - Title, Description
  - Date and Time
  - Location
  - Max Capacity
  - Banner Image
  - Fee
* Filter events into Upcoming and Past based on current date.
* RSVP Tracking:
  - Attendees can RSVP to events.
  - Organizers can view a list of attendees.
  - Organizers can also download the list of attendees
* Prevent overbooking (respect max capacity).
* Reminder System:
  - Users receive reminders via email notifications sent by Organizers.

## **Backend REST APIs (Spring Boot)**
**Authentication Endpoints**

POST /api/auth/register          - User registration
POST /api/auth/login             - User authentication  
GET  /api/auth/check-username/{username} - Check username availability
GET  /api/auth/check-email/{email}       - Check email availability

**Event Management Endpoints**

GET    /api/events               - Get all events
POST   /api/events               - Create new event
GET    /api/events/{id}          - Get event by ID
PUT    /api/events/{id}          - Update event
DELETE /api/events/{id}          - Delete event
GET    /api/events/upcoming      - Get upcoming events
GET    /api/events/past          - Get past events
GET    /api/events/search        - Search events by title/location
GET    /api/events/organizer/{id} - Get events by organizer
POST   /api/events/{id}/reminder - Send event reminder

**RSVP Management Endpoints**

POST   /api/rsvp/{eventId}       - Create RSVP
PUT    /api/rsvp/{eventId}       - Update RSVP status
DELETE /api/rsvp/{eventId}       - Cancel RSVP
GET    /api/rsvp/event/{eventId} - Get RSVPs for event
GET    /api/rsvp/user/{userId}   - Get RSVPs for user
GET    /api/rsvp/event/{eventId}/count - Get RSVP count

**User Management Endpoints**

GET    /api/users               - Get all users
GET    /api/users/{id}          - Get user by ID
PUT    /api/users/{id}          - Update user
DELETE /api/users/{id}          - Delete user
GET    /api/users/stats         - Get user statistics
GET    /api/users/organizers    - Get all organizers
GET    /api/users/attendees     - Get all attendees

## **Database Model - Tables and Fields**

### **Users Table**
- id: BIGINT (Primary Key)
- username: VARCHAR(20) (Unique)
- email: VARCHAR(255) (Unique)
- password: VARCHAR(255)
- role: ENUM('ORGANIZER', 'ATTENDEE')

### **Event Table**
- id: BIGINT (Primary Key)
- title: VARCHAR(100)
- description: VARCHAR(500)
- date_time: DATETIME
- location: VARCHAR(100)
- max_capacity: INT
- image_url: VARCHAR(255)
- fee: DECIMAL(10,2)
- registration_status: ENUM('OPEN', 'CLOSED', 'SUSPENDED')
- organizer_id: BIGINT (Foreign Key)

### **RSVP Table**
- id: BIGINT (Primary Key)
- user_id: BIGINT (Foreign Key)
- event_id: BIGINT (Foreign Key)
- status: ENUM('PENDING', 'CONFIRMED', 'CANCELLED')
- rsvp_date: DATETIME
