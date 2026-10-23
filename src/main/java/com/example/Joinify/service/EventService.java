package com.example.Joinify.service;

import com.example.Joinify.dto.EventRequest;
import com.example.Joinify.dto.EventResponse;
import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.RegistrationStatus;
import com.example.Joinify.entity.User;
import com.example.Joinify.exception.BadRequestException;
import com.example.Joinify.exception.ResourceNotFoundException;
import com.example.Joinify.exception.UnauthorizedException;
import com.example.Joinify.mapper.DtoMapper;
import com.example.Joinify.repository.EventRepository;
import com.example.Joinify.repository.RSVPRepository;
import com.example.Joinify.exception.ForbiddenException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RSVPRepository rsvpRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    // ===================== Create / update / delete =====================

    public EventResponse createEvent(EventRequest request, String organizerUsername) {
        User organizer = getUserByUsername(organizerUsername);
        validateImageUrl(request.imageUrl());

        Event event = new Event();
        DtoMapper.copyToEntity(request, event);
        event.setOrganizer(organizer);

        return DtoMapper.toEventResponse(eventRepository.save(event));
    }

    public EventResponse updateEvent(Long eventId, EventRequest request, String organizerUsername) {
        // Lock the row so no RSVP can slip in between the count check below and the save
        eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        Event event = getOwnedEvent(eventId, organizerUsername, "You are not authorized to update this event");
        validateImageUrl(request.imageUrl());

        long confirmedCount = rsvpRepository.countConfirmedRSVPsByEventId(eventId);
        if (request.maxCapacity() < confirmedCount) {
            throw new BadRequestException(
                    "Capacity cannot be lower than the current " + confirmedCount + " confirmed RSVPs");
        }

        DtoMapper.copyToEntity(request, event);
        Event savedEvent = eventRepository.save(event);

        notifyAttendeesOfUpdate(savedEvent);
        return DtoMapper.toEventResponse(savedEvent);
    }

    public void deleteEvent(Long eventId, String organizerUsername) {
        Event event = getOwnedEvent(eventId, organizerUsername, "You can only delete your own events");
        eventRepository.delete(event);
    }

    // ===================== Read (returns DTOs) =====================

    public EventResponse getEvent(Long id) {
        return DtoMapper.toEventResponse(getEventById(id));
    }

    public List<EventResponse> getAllEvents() {
        return DtoMapper.toEventResponses(eventRepository.findAllEventsWithOrganizer());
    }

    public List<EventResponse> getUpcomingEvents() {
        return DtoMapper.toEventResponses(eventRepository.findUpcomingEventsWithOrganizer(LocalDateTime.now()));
    }

    public List<EventResponse> getPastEvents() {
        return DtoMapper.toEventResponses(eventRepository.findPastEventsWithOrganizer(LocalDateTime.now()));
    }

    public List<EventResponse> getEventsWithAvailableCapacity() {
        return DtoMapper.toEventResponses(
                eventRepository.findEventsWithAvailableCapacityAndOrganizer(LocalDateTime.now()));
    }

    public List<EventResponse> searchEventsByTitle(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BadRequestException("Search keyword cannot be empty");
        }
        return DtoMapper.toEventResponses(
                eventRepository.findByTitleContainingIgnoreCaseWithOrganizer(keyword.trim()));
    }

    public List<EventResponse> searchEventsByLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new BadRequestException("Location cannot be empty");
        }
        return DtoMapper.toEventResponses(
                eventRepository.findByLocationContainingIgnoreCaseWithOrganizer(location.trim()));
    }

    public List<EventResponse> getEventsBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }
        return DtoMapper.toEventResponses(eventRepository.findEventsBetweenDatesWithOrganizer(startDate, endDate));
    }

    public List<EventResponse> getEventsByOrganizerId(Long organizerId) {
        return DtoMapper.toEventResponses(eventRepository.findByOrganizerIdWithOrganizer(organizerId));
    }

    public List<EventResponse> getMyEvents(String organizerUsername) {
        Long organizerId = getUserByUsername(organizerUsername).getId();
        return DtoMapper.toEventResponses(eventRepository.findByOrganizerIdWithOrganizer(organizerId));
    }

    public List<EventResponse> getMyUpcomingEvents(String organizerUsername) {
        Long organizerId = getUserByUsername(organizerUsername).getId();
        return DtoMapper.toEventResponses(
                eventRepository.findUpcomingEventsByOrganizerWithOrganizer(organizerId, LocalDateTime.now()));
    }

    public List<EventResponse> getMyPastEvents(String organizerUsername) {
        Long organizerId = getUserByUsername(organizerUsername).getId();
        return DtoMapper.toEventResponses(
                eventRepository.findPastEventsByOrganizerWithOrganizer(organizerId, LocalDateTime.now()));
    }

    // ===================== Internal helpers (return entities) =====================

    // Used inside services (e.g. RSVPService). Controllers should use getEvent() instead.
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
    }

    // Also used by RSVPService for organizer-only views
    public Event getOwnedEvent(Long eventId, String organizerUsername, String notOwnerMessage) {
        Event event = getEventById(eventId);
        if (!event.getOrganizer().getUsername().equals(organizerUsername)) {
            throw new ForbiddenException(notOwnerMessage);
        }
        return event;
    }

    private User getUserByUsername(String username) {
        return userService.getUserByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private void notifyAttendeesOfUpdate(Event event) {
        try {
            List<String> attendeeEmails = rsvpRepository.findConfirmedAttendeesByEventId(event.getId())
                    .stream()
                    .map(User::getEmail)
                    .collect(Collectors.toList());

            if (!attendeeEmails.isEmpty()) {
                emailService.sendEventUpdateNotification(event, attendeeEmails);
            }
        } catch (Exception e) {
            // Don't fail the update if email sending fails
            System.err.println("Failed to send event update notifications: " + e.getMessage());
        }
    }

    // Business rule that annotations can't express: must look like an image link
    private void validateImageUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            String protocol = url.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new BadRequestException("Image URL must use HTTP or HTTPS protocol");
            }

            String path = imageUrl.toLowerCase();
            boolean hasImageExtension = path.contains(".jpg") || path.contains(".jpeg") ||
                    path.contains(".png") || path.contains(".gif") ||
                    path.contains(".bmp") || path.contains(".webp") ||
                    path.contains(".svg");

            boolean isImageHost = path.contains("imgur.com") || path.contains("unsplash.com") ||
                    path.contains("pexels.com") || path.contains("cloudinary.com") ||
                    path.contains("amazonaws.com") || path.contains("googleusercontent.com");

            if (!hasImageExtension && !isImageHost) {
                throw new BadRequestException("Please enter a valid image URL");
            }
        } catch (MalformedURLException e) {
            throw new BadRequestException("Please enter a valid image URL");
        }
    }

    // ===================== Capacity (unchanged) =====================

    public boolean isEventAtCapacity(Long eventId) {
        Event event = getEventById(eventId);
        long confirmedCount = rsvpRepository.countConfirmedRSVPsByEventId(eventId);
        return confirmedCount >= event.getMaxCapacity();
    }

    public int getAvailableSpots(Long eventId) {
        Event event = getEventById(eventId);
        long confirmedCount = rsvpRepository.countConfirmedRSVPsByEventId(eventId);
        return (int) Math.max(0, event.getMaxCapacity() - confirmedCount);
    }

    public int getConfirmedAttendeeCount(Long eventId) {
        getEventById(eventId); // throws 404 if the event doesn't exist
        return (int) rsvpRepository.countConfirmedRSVPsByEventId(eventId);
    }

    // ===================== Reminders (unchanged) =====================

    public String sendEventReminder(Long eventId, String customMessage, String organizerUsername) {
        Event event = getOwnedEvent(eventId, organizerUsername, "You can only send reminders for your own events");

        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot send reminders for past events");
        }

        List<User> confirmedAttendees = rsvpRepository.findConfirmedAttendeesByEventId(eventId);
        if (confirmedAttendees.isEmpty()) {
            throw new BadRequestException("No confirmed attendees to send reminders to");
        }

        if (customMessage != null && customMessage.length() > 500) {
            throw new BadRequestException("Custom message cannot exceed 500 characters");
        }

        List<String> attendeeEmails = confirmedAttendees.stream()
                .map(User::getEmail)
                .collect(Collectors.toList());

        emailService.sendManualEventReminder(event, attendeeEmails, customMessage);
        return "Reminder sent to " + attendeeEmails.size() + " attendees successfully";
    }

    public void validateReminderPermission(Long eventId, String username) {
        Event event = getOwnedEvent(eventId, username, "You can only send reminders for your own events");
        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot send reminders for past events");
        }
    }

    // ===================== Registration open/close (unchanged) =====================

    public Event closeEventRegistration(Long eventId, String organizerUsername) {
        Event event = getOwnedEvent(eventId, organizerUsername, "You can only manage registration for your own events");
        if (event.getRegistrationStatus() == RegistrationStatus.CLOSED) {
            throw new BadRequestException("Registration is already closed for this event");
        }
        event.setRegistrationStatus(RegistrationStatus.CLOSED);
        return eventRepository.save(event);
    }

    public Event openEventRegistration(Long eventId, String organizerUsername) {
        Event event = getOwnedEvent(eventId, organizerUsername, "You can only manage registration for your own events");
        if (event.getRegistrationStatus() == RegistrationStatus.OPEN) {
            throw new BadRequestException("Registration is already open for this event");
        }
        event.setRegistrationStatus(RegistrationStatus.OPEN);
        return eventRepository.save(event);
    }

    public boolean isRegistrationOpen(Long eventId) {
        Event event = getEventById(eventId);
        return event.getRegistrationStatus() == RegistrationStatus.OPEN &&
                event.getDateTime().isAfter(LocalDateTime.now());
    }
}