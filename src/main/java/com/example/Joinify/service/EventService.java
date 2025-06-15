package com.example.Joinify.service;

import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.User;
import com.example.Joinify.exception.BadRequestException;
import com.example.Joinify.exception.ResourceNotFoundException;
import com.example.Joinify.exception.UnauthorizedException;
import com.example.Joinify.repository.EventRepository;
import com.example.Joinify.repository.RSVPRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
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
        // Validate through exceptions
        validateEventForCreation(event);

        // Set default values
        if (event.getFee() == null) {
            event.setFee(BigDecimal.ZERO);
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
    public Event updateEvent(Long eventId, Event updatedEvent, String organizerUsername) {
        // Check if event exists
        Event existingEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        // Check authorization
        if (!existingEvent.getOrganizer().getUsername().equals(organizerUsername)) {
            throw new UnauthorizedException("You are not authorized to update this event");
        }

        // Validate updated event data
        validateEventForUpdate(updatedEvent, existingEvent);

        // Update fields
        existingEvent.setTitle(updatedEvent.getTitle());
        existingEvent.setDescription(updatedEvent.getDescription());
        existingEvent.setDateTime(updatedEvent.getDateTime());
        existingEvent.setLocation(updatedEvent.getLocation());
        existingEvent.setMaxCapacity(updatedEvent.getMaxCapacity());
        existingEvent.setImageUrl(updatedEvent.getImageUrl());
        existingEvent.setFee(updatedEvent.getFee() != null ? updatedEvent.getFee() : BigDecimal.ZERO);

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

    // Validation methods that throw exceptions
    private void validateEventForCreation(Event event) {
        validateBasicEventData(event);
        validateEventDateTime(event.getDateTime());
        validateEventCapacity(event.getMaxCapacity());
        validateEventFee(event.getFee());
        validateImageUrl(event.getImageUrl());
    }

    private void validateEventForUpdate(Event updatedEvent, Event existingEvent) {
        validateBasicEventData(updatedEvent);
        validateEventDateTime(updatedEvent.getDateTime());
        validateEventCapacity(updatedEvent.getMaxCapacity());
        validateEventFee(updatedEvent.getFee());
        validateImageUrl(updatedEvent.getImageUrl());

        // Additional validation for updates
        validateCapacityNotReducedBelowCurrentRSVPs(updatedEvent.getMaxCapacity(), existingEvent.getId());
    }

    private void validateBasicEventData(Event event) {
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Event title is required");
        }
        if (event.getTitle().trim().length() < 2) {
            throw new BadRequestException("Event title must be at least 2 characters long");
        }
        if (event.getTitle().length() > 100) {
            throw new BadRequestException("Event title cannot exceed 100 characters");
        }

        if (event.getLocation() == null || event.getLocation().trim().isEmpty()) {
            throw new BadRequestException("Event location is required");
        }
        if (event.getLocation().trim().length() < 2) {
            throw new BadRequestException("Event location must be at least 2 characters long");
        }
        if (event.getLocation().length() > 200) {
            throw new BadRequestException("Event location cannot exceed 200 characters");
        }

        if (event.getDescription() != null && event.getDescription().length() > 1000) {
            throw new BadRequestException("Event description cannot exceed 1000 characters");
        }
    }

    private void validateEventDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new BadRequestException("Event date and time is required");
        }
        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Event date must be in the future");
        }
    }

    private void validateEventCapacity(Integer maxCapacity) {
        if (maxCapacity == null || maxCapacity < 1) {
            throw new BadRequestException("Event capacity must be at least 1");
        }
        if (maxCapacity > 10000) {
            throw new BadRequestException("Event capacity cannot exceed 10000");
        }
    }

    private void validateEventFee(BigDecimal fee) {
        if (fee != null) {
            if (fee.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Event fee cannot be negative");
            }
            if (fee.compareTo(new BigDecimal("100000")) > 0) {
                throw new BadRequestException("Event fee cannot exceed Rs. 100000");
            }
            if (fee.scale() > 2) {
                throw new BadRequestException("Event fee cannot have more than 2 decimal places");
            }
        }
    }

    private void validateImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
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
    }

    private void validateCapacityNotReducedBelowCurrentRSVPs(Integer newCapacity, Long eventId) {
        try {
            // This would require RSVP service integration
            // long currentRSVPs = rsvpService.getConfirmedRSVPCount(eventId);
            // if (newCapacity < currentRSVPs) {
            //     throw new BadRequestException("Cannot reduce capacity below current confirmed RSVPs");
            // }
        } catch (Exception e) {
            // Log error but don't fail the validation
            System.err.println("Could not validate RSVP capacity: " + e.getMessage());
        }
    }
}
