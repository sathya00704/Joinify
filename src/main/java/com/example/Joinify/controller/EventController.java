package com.example.Joinify.controller;

import com.example.Joinify.dto.EventRequest;
import com.example.Joinify.dto.EventResponse;
import com.example.Joinify.entity.Event;
import com.example.Joinify.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    @Autowired
    private EventService eventService;

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

    // ===================== Public listing =====================

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents() {
        return ResponseEntity.ok(eventService.getUpcomingEvents());
    }

    @GetMapping("/past")
    public ResponseEntity<List<EventResponse>> getPastEvents() {
        return ResponseEntity.ok(eventService.getPastEvents());
    }

    @GetMapping("/available")
    public ResponseEntity<List<EventResponse>> getEventsWithAvailableCapacity() {
        return ResponseEntity.ok(eventService.getEventsWithAvailableCapacity());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    @GetMapping("/search/title")
    public ResponseEntity<List<EventResponse>> searchEventsByTitle(@RequestParam String keyword) {
        return ResponseEntity.ok(eventService.searchEventsByTitle(keyword));
    }

    @GetMapping("/search/location")
    public ResponseEntity<List<EventResponse>> searchEventsByLocation(@RequestParam String location) {
        return ResponseEntity.ok(eventService.searchEventsByLocation(location));
    }

    // e.g. /api/events/date-range?startDate=2026-11-01T00:00:00&endDate=2026-11-30T23:59:59
    @GetMapping("/date-range")
    public ResponseEntity<List<EventResponse>> getEventsBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(eventService.getEventsBetweenDates(startDate, endDate));
    }

    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<List<EventResponse>> getEventsByOrganizer(@PathVariable Long organizerId) {
        return ResponseEntity.ok(eventService.getEventsByOrganizerId(organizerId));
    }

    // ===================== Organizer: own events =====================

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request,
                                                     Authentication authentication) {
        EventResponse created = eventService.createEvent(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id,
                                                     @Valid @RequestBody EventRequest request,
                                                     Authentication authentication) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id, Authentication authentication) {
        eventService.deleteEvent(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-events")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<EventResponse>> getMyEvents(Authentication authentication) {
        return ResponseEntity.ok(eventService.getMyEvents(authentication.getName()));
    }

    @GetMapping("/my-events/upcoming")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<EventResponse>> getMyUpcomingEvents(Authentication authentication) {
        return ResponseEntity.ok(eventService.getMyUpcomingEvents(authentication.getName()));
    }

    @GetMapping("/my-events/past")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<EventResponse>> getMyPastEvents(Authentication authentication) {
        return ResponseEntity.ok(eventService.getMyPastEvents(authentication.getName()));
    }

    // ===================== Capacity =====================

    @GetMapping("/{id}/capacity")
    public ResponseEntity<EventCapacityResponse> getEventCapacityInfo(@PathVariable Long id) {
        Event event = eventService.getEventById(id);
        EventCapacityResponse response = new EventCapacityResponse(
                event.getMaxCapacity(),
                eventService.getConfirmedAttendeeCount(id),
                eventService.getAvailableSpots(id),
                eventService.isEventAtCapacity(id)
        );
        return ResponseEntity.ok(response);
    }

    // ===================== Reminders =====================

    @PostMapping("/{eventId}/send-reminder")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<String> sendEventReminder(@PathVariable Long eventId,
                                                    @RequestBody Map<String, String> request,
                                                    Authentication authentication) {
        String result = eventService.sendEventReminder(eventId, request.get("message"), authentication.getName());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{eventId}/attendee-count")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Map<String, Object>> getAttendeeCount(@PathVariable Long eventId,
                                                                Authentication authentication) {
        eventService.validateReminderPermission(eventId, authentication.getName());
        int confirmedCount = eventService.getConfirmedAttendeeCount(eventId);

        Map<String, Object> response = new HashMap<>();
        response.put("confirmedAttendees", confirmedCount);
        response.put("canSendReminder", confirmedCount > 0);
        return ResponseEntity.ok(response);
    }

    // ===================== Registration open/close =====================

    @PutMapping("/{eventId}/close-registration")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Map<String, String>> closeEventRegistration(@PathVariable Long eventId,
                                                                      Authentication authentication) {
        Event event = eventService.closeEventRegistration(eventId, authentication.getName());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Registration closed successfully for: " + event.getTitle());
        response.put("status", event.getRegistrationStatus().toString());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{eventId}/open-registration")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Map<String, String>> openEventRegistration(@PathVariable Long eventId,
                                                                     Authentication authentication) {
        Event event = eventService.openEventRegistration(eventId, authentication.getName());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Registration opened successfully for: " + event.getTitle());
        response.put("status", event.getRegistrationStatus().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}/registration-status")
    public ResponseEntity<Map<String, Object>> getRegistrationStatus(@PathVariable Long eventId) {
        Event event = eventService.getEventById(eventId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", event.getRegistrationStatus().toString());
        response.put("isOpen", eventService.isRegistrationOpen(eventId));
        response.put("eventTitle", event.getTitle());
        return ResponseEntity.ok(response);
    }
}