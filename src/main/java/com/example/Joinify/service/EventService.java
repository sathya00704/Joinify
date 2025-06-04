package com.example.Joinify.service;

import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.User;
import com.example.Joinify.entity.RSVPStatus;
import com.example.Joinify.repository.EventRepository;
import com.example.Joinify.repository.RSVPRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RSVPRepository rsvpRepository;

    // Create or update event
    public Event saveEvent(Event event) {
        // Validate event data
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Event title is required");
        }
        if (event.getDateTime() == null) {
            throw new IllegalArgumentException("Event date and time is required");
        }
        if (event.getMaxCapacity() <= 0) {
            throw new IllegalArgumentException("Event capacity must be greater than 0");
        }
        if (event.getOrganizer() == null) {
            throw new IllegalArgumentException("Event organizer is required");
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
    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    // Delete event by ID
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new IllegalArgumentException("Event not found");
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
        return eventRepository.findByOrganizer(organizer);
    }

    // Get events by organizer ID
    public List<Event> getEventsByOrganizerId(Long organizerId) {
        return eventRepository.findByOrganizerId(organizerId);
    }

    // Get upcoming events by organizer
    public List<Event> getUpcomingEventsByOrganizer(Long organizerId) {
        return eventRepository.findUpcomingEventsByOrganizer(organizerId, LocalDateTime.now());
    }

    // Get past events by organizer
    public List<Event> getPastEventsByOrganizer(Long organizerId) {
        return eventRepository.findPastEventsByOrganizer(organizerId, LocalDateTime.now());
    }

    // Search events by title
    public List<Event> searchEventsByTitle(String keyword) {
        return eventRepository.findByTitleContainingIgnoreCase(keyword);
    }

    // Search events by location
    public List<Event> searchEventsByLocation(String location) {
        return eventRepository.findByLocationContainingIgnoreCase(location);
    }

    // Get events within date range
    public List<Event> getEventsBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        return eventRepository.findEventsBetweenDates(startDate, endDate);
    }

    // Get events with available capacity
    public List<Event> getEventsWithAvailableCapacity() {
        return eventRepository.findEventsWithAvailableCapacity(LocalDateTime.now());
    }

    // Check if event is at capacity
    public boolean isEventAtCapacity(Long eventId) {
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            return false;
        }

        Event event = eventOpt.get();
        long confirmedCount = rsvpRepository.countConfirmedRSVPsByEventId(eventId);
        return confirmedCount >= event.getMaxCapacity();
    }

    // Get available spots for an event
    public int getAvailableSpots(Long eventId) {
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            return 0;
        }

        Event event = eventOpt.get();
        long confirmedCount = rsvpRepository.countConfirmedRSVPsByEventId(eventId);
        return (int) Math.max(0, event.getMaxCapacity() - confirmedCount);
    }

    // Get confirmed attendee count for an event
    public long getConfirmedAttendeeCount(Long eventId) {
        return rsvpRepository.countConfirmedRSVPsByEventId(eventId);
    }

    // Update event details
    public Event updateEvent(Long eventId, Event updatedEvent) {
        Optional<Event> existingEventOpt = eventRepository.findById(eventId);
        if (existingEventOpt.isEmpty()) {
            throw new IllegalArgumentException("Event not found");
        }

        Event existingEvent = existingEventOpt.get();
        existingEvent.setTitle(updatedEvent.getTitle());
        existingEvent.setDescription(updatedEvent.getDescription());
        existingEvent.setDateTime(updatedEvent.getDateTime());
        existingEvent.setLocation(updatedEvent.getLocation());
        existingEvent.setMaxCapacity(updatedEvent.getMaxCapacity());

        return eventRepository.save(existingEvent);
    }

    // Count events by organizer
    public long countEventsByOrganizer(Long organizerId) {
        return eventRepository.countByOrganizerId(organizerId);
    }

    // Get total event count
    public long getTotalEventCount() {
        return eventRepository.count();
    }
}
