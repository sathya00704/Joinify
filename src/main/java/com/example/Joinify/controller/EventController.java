package com.example.Joinify.controller;

import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.User;
import com.example.Joinify.exception.ResourceNotFoundException;
import com.example.Joinify.service.EventService;
import com.example.Joinify.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserService userService;

    // Response class for event capacity
    public static class EventCapacityResponse {
        public final int maxCapacity;
        public final long confirmedAttendees;
        public final int availableSpots;
        public final boolean atCapacity;

        public EventCapacityResponse(int maxCapacity, long confirmedAttendees, int availableSpots, boolean atCapacity) {
            this.maxCapacity = maxCapacity;
            this.confirmedAttendees = confirmedAttendees;
            this.availableSpots = availableSpots;
            this.atCapacity = atCapacity;
        }
    }

    // Get all events (public access)
    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        List<Event> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    // Get upcoming events (public access)
    @GetMapping("/upcoming")
    public ResponseEntity<List<Event>> getUpcomingEvents() {
        List<Event> events = eventService.getUpcomingEvents();
        return ResponseEntity.ok(events);
    }

    // Get past events (public access)
    @GetMapping("/past")
    public ResponseEntity<List<Event>> getPastEvents() {
        List<Event> events = eventService.getPastEvents();
        return ResponseEntity.ok(events);
    }

    // Get events with available capacity
    @GetMapping("/available")
    public ResponseEntity<List<Event>> getEventsWithAvailableCapacity() {
        List<Event> events = eventService.getEventsWithAvailableCapacity();
        return ResponseEntity.ok(events);
    }

    // Get event by ID
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        try {
            Event event = eventService.getEventById(id);
            return ResponseEntity.ok(event);
        } catch (ResourceNotFoundException e) {
            throw e;
        }
    }

    // Search events by title
    @GetMapping("/search/title")
    public ResponseEntity<List<Event>> searchEventsByTitle(@RequestParam String keyword) {
        try {
            List<Event> events = eventService.searchEventsByTitle(keyword);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Search events by location
    @GetMapping("/search/location")
    public ResponseEntity<List<Event>> searchEventsByLocation(@RequestParam String location) {
        try {
            List<Event> events = eventService.searchEventsByLocation(location);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Get events between dates
    @GetMapping("/date-range")
    public ResponseEntity<List<Event>> getEventsBetweenDates(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            List<Event> events = eventService.getEventsBetweenDates(start, end);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Create new event (Organizers only)
    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Event> createEvent(@Valid @RequestBody Event event, Authentication authentication) {
        try {
            // Get current user as organizer
            String username = authentication.getName();
            Optional<User> organizer = userService.getUserByUsername(username);

            if (organizer.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            event.setOrganizer(organizer.get());
            Event savedEvent = eventService.saveEvent(event);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedEvent);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Update event (Organizers only - own events)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id,
                                             @Valid @RequestBody Event updatedEvent,
                                             Authentication authentication) {
        try {
            // Check if event exists and get it
            Event existingEvent = eventService.getEventById(id);
            String username = authentication.getName();

            // Check if current user is the organizer of this event
            if (!existingEvent.getOrganizer().getUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Event savedEvent = eventService.updateEvent(id, updatedEvent);
            return ResponseEntity.ok(savedEvent);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Delete event (Organizers only - own events)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id, Authentication authentication) {
        try {
            // Check if event exists and get it
            Event existingEvent = eventService.getEventById(id);
            String username = authentication.getName();

            // Check if current user is the organizer of this event
            if (!existingEvent.getOrganizer().getUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            eventService.deleteEvent(id);
            return ResponseEntity.noContent().build();

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get events by organizer
    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<List<Event>> getEventsByOrganizer(@PathVariable Long organizerId) {
        try {
            List<Event> events = eventService.getEventsByOrganizerId(organizerId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Get my events (current organizer)
    @GetMapping("/my-events")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<Event>> getMyEvents(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> organizer = userService.getUserByUsername(username);

        if (organizer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Event> events = eventService.getEventsByOrganizerId(organizer.get().getId());
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Get upcoming events by organizer
    @GetMapping("/my-events/upcoming")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<Event>> getMyUpcomingEvents(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> organizer = userService.getUserByUsername(username);

        if (organizer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Event> events = eventService.getUpcomingEventsByOrganizer(organizer.get().getId());
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Get past events by organizer
    @GetMapping("/my-events/past")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<Event>> getMyPastEvents(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> organizer = userService.getUserByUsername(username);

        if (organizer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Event> events = eventService.getPastEventsByOrganizer(organizer.get().getId());
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Get event capacity info
    @GetMapping("/{id}/capacity")
    public ResponseEntity<EventCapacityResponse> getEventCapacityInfo(@PathVariable Long id) {
        try {
            Event event = eventService.getEventById(id);
            long confirmedCount = eventService.getConfirmedAttendeeCount(id);
            int availableSpots = eventService.getAvailableSpots(id);
            boolean isAtCapacity = eventService.isEventAtCapacity(id);

            EventCapacityResponse response = new EventCapacityResponse(
                    event.getMaxCapacity(),
                    confirmedCount,
                    availableSpots,
                    isAtCapacity
            );

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
