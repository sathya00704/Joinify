Joinify - Event Management System

Overview:
Organizations and individuals often struggle with managing events efficiently—especially when it comes to organizing invitations, tracking RSVPs, sending reminders, and managing attendees. This project aims to build a web-based Event Management System that allows users to create, browse, and RSVP to events with role-based access, using a Java Spring Boot backend and minimal HTML/CSS/JS frontend.

Objective:
To develop a RESTful Event Management System where:
*Organizers can create, update, and manage events.
*Attendees can browse events, RSVP to them, and receive reminders.
*The system should support filtering, status tracking (upcoming/past), and secure access using authentication and role-based authorization.

Key Features:
User Roles:
* Organizer: Can create, edit, and delete events; view RSVPs for their events.
* Attendee: Can view and RSVP to available events.

Event Functionality:
* Create, update, delete events with fields like:
- Title, Description
- Date and Time
- Location
- Max Capacity
* Filter events into Upcoming and Past based on current date.

* RSVP Tracking:
- Attendees can RSVP to events.
- Organizers can view a list of attendees.
* Prevent overbooking (respect max capacity).
* Reminder System:
- Users receive reminders (via email notification) 24 hours before event start.

Technical:
* Backend: Spring Boot, JPA/Hibernate, RESTful API, JWT-based security.
* Frontend: HTML, CSS, and JavaScript
* Database: MySQL
