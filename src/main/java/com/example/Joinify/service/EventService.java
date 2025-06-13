package com.example.Joinify.service;

import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.User;
import com.example.Joinify.exception.BadRequestException;
import com.example.Joinify.exception.ResourceNotFoundException;
import com.example.Joinify.repository.EventRepository;
import com.example.Joinify.repository.RSVPRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RSVPRepository rsvpRepository;

    // Create or update event
    public Event saveEvent(Event event) {
        // Validate event data
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Event title is required");
        }
        if (event.getDateTime() == null) {
            throw new BadRequestException("Event date and time is required");
        }
        if (event.getMaxCapacity() <= 0) {
            throw new BadRequestException("Event capacity must be greater than 0");
        }
        if (event.getOrganizer() == null) {
            throw new BadRequestException("Event organizer is required");
        }
        if (event.getLocation() == null || event.getLocation().trim().isEmpty()) {
            throw new BadRequestException("Event location is required");
        }

        return eventRepository.save(event);
    }

    // Create new event
    public Event createEvent(String title, String description, LocalDateTime dateTime,
                             String location, int maxCapacity, User organizer) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setDateTime(dateTime);
        event.setLocation(location);
        event.setMaxCapacity(maxCapacity);
        event.setOrganizer(organizer);

        return saveEvent(event);
    }

    // Get event by ID
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
    }

    // Get event by ID (Optional version for backward compatibility)
    public Optional<Event> findEventById(Long id) {
        return eventRepository.findById(id);
    }

    // Delete event by ID
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new ResourceNotFoundException("Event", "id", id);
        }
        eventRepository.deleteById(id);
    }

    // Get all events
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    // Get upcoming events (events after current date/time)
    public List<Event> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents(LocalDateTime.now());
    }

    // Get past events (events before current date/time)
    public List<Event> getPastEvents() {
        return eventRepository.findPastEvents(LocalDateTime.now());
    }

    // Get events by organizer
    public List<Event> getEventsByOrganizer(User organizer) {
        if (organizer == null) {
            throw new BadRequestException("Organizer cannot be null");
        }
        return eventRepository.findByOrganizer(organizer);
    }

    // Get events by organizer ID
    public List<Event> getEventsByOrganizerId(Long organizerId) {
        if (organizerId == null) {
            throw new BadRequestException("Organizer ID cannot be null");
        }
        return eventRepository.findByOrganizerId(organizerId);
    }

    // Get upcoming events by organizer
    public List<Event> getUpcomingEventsByOrganizer(Long organizerId) {
        if (organizerId == null) {
            throw new BadRequestException("Organizer ID cannot be null");
        }
        return eventRepository.findUpcomingEventsByOrganizer(organizerId, LocalDateTime.now());
    }

    // Get past events by organizer
    public List<Event> getPastEventsByOrganizer(Long organizerId) {
        if (organizerId == null) {
            throw new BadRequestException("Organizer ID cannot be null");
        }
        return eventRepository.findPastEventsByOrganizer(organizerId, LocalDateTime.now());
    }

    // Search events by title
    public List<Event> searchEventsByTitle(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BadRequestException("Search keyword cannot be empty");
        }
        return eventRepository.findByTitleContainingIgnoreCase(keyword);
    }

    // Search events by location
    public List<Event> searchEventsByLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new BadRequestException("Location cannot be empty");
        }
        return eventRepository.findByLocationContainingIgnoreCase(location);
    }

    // Get events within date range
    public List<Event> getEventsBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }
        return eventRepository.findEventsBetweenDates(startDate, endDate);
    }

    // Get events with available capacity
    public List<Event> getEventsWithAvailableCapacity() {
        return eventRepository.findEventsWithAvailableCapacity(LocalDateTime.now());
    }

    // Check if event is at capacity
    public boolean isEventAtCapacity(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }

        Event event = getEventById(eventId);
        long confirmedCount = rsvpRepository.countConfirmedRSVPsByEventId(eventId);
        return confirmedCount >= event.getMaxCapacity();
    }

    // Get available spots for an event
    public int getAvailableSpots(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }

        Event event = getEventById(eventId);
        long confirmedCount = rsvpRepository.countConfirmedRSVPsByEventId(eventId);
        return (int) Math.max(0, event.getMaxCapacity() - confirmedCount);
    }

    // Get confirmed attendee count for an event
    public long getConfirmedAttendeeCount(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }
        return rsvpRepository.countConfirmedRSVPsByEventId(eventId);
    }

    // Update event details
    public Event updateEvent(Long eventId, Event updatedEvent) {
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }
        if (updatedEvent == null) {
            throw new BadRequestException("Updated event data cannot be null");
        }

        Event existingEvent = getEventById(eventId);

        // Update fields
        if (updatedEvent.getTitle() != null) {
            existingEvent.setTitle(updatedEvent.getTitle());
        }
        if (updatedEvent.getDescription() != null) {
            existingEvent.setDescription(updatedEvent.getDescription());
        }
        if (updatedEvent.getDateTime() != null) {
            existingEvent.setDateTime(updatedEvent.getDateTime());
        }
        if (updatedEvent.getLocation() != null) {
            existingEvent.setLocation(updatedEvent.getLocation());
        }
        if (updatedEvent.getMaxCapacity() > 0) {
            existingEvent.setMaxCapacity(updatedEvent.getMaxCapacity());
        }

        return eventRepository.save(existingEvent);
    }

    // Count events by organizer
    public long countEventsByOrganizer(Long organizerId) {
        if (organizerId == null) {
            throw new BadRequestException("Organizer ID cannot be null");
        }
        return eventRepository.countByOrganizerId(organizerId);
    }

    // Get total event count
    public long getTotalEventCount() {
        return eventRepository.count();
    }

    public List<Event> getEventsByOrganizer(Long organizerId) {
        return eventRepository.findByOrganizerIdWithOrganizer(organizerId);
    }

    public List<Event> getOrganizerUpcomingEvents(Long organizerId) {
        return eventRepository.findUpcomingEventsByOrganizerWithDetails(organizerId, LocalDateTime.now());
    }

    public List<Event> getOrganizerPastEvents(Long organizerId) {
        return eventRepository.findPastEventsByOrganizerWithDetails(organizerId, LocalDateTime.now());
    }
}
