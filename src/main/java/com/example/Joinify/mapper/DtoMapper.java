package com.example.Joinify.mapper;

import com.example.Joinify.dto.*;
import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.RSVP;
import com.example.Joinify.entity.User;

import java.math.BigDecimal;
import java.util.List;

// Converts entities to DTOs (and request DTOs into entities).
// Call these inside a @Transactional service method, because they read lazy relations.
public final class DtoMapper {

    private DtoMapper() {
    }

    public static EventResponse toEventResponse(Event event) {
        User organizer = event.getOrganizer();
        OrganizerSummary organizerSummary = organizer == null
                ? null
                : new OrganizerSummary(organizer.getId(), organizer.getUsername());

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getDateTime(),
                event.getLocation(),
                event.getMaxCapacity(),
                event.getImageUrl(),
                event.getFee(),
                event.getRegistrationStatus(),
                organizerSummary
        );
    }

    public static List<EventResponse> toEventResponses(List<Event> events) {
        return events.stream().map(DtoMapper::toEventResponse).toList();
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public static AttendeeResponse toAttendeeResponse(RSVP rsvp) {
        User user = rsvp.getUser();
        return new AttendeeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                rsvp.getStatus(),
                rsvp.getRsvpDate()
        );
    }

    public static List<AttendeeResponse> toAttendeeResponses(List<RSVP> rsvps) {
        return rsvps.stream().map(DtoMapper::toAttendeeResponse).toList();
    }

    public static RsvpResponse toRsvpResponse(RSVP rsvp) {
        return new RsvpResponse(
                rsvp.getId(),
                rsvp.getStatus(),
                rsvp.getRsvpDate(),
                toEventResponse(rsvp.getEvent())
        );
    }

    public static List<RsvpResponse> toRsvpResponses(List<RSVP> rsvps) {
        return rsvps.stream().map(DtoMapper::toRsvpResponse).toList();
    }

    // Copies the editable fields from the request onto a new or existing event.
    // id, organizer and registrationStatus are never taken from the client.
    public static void copyToEntity(EventRequest request, Event event) {
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setDateTime(request.dateTime());
        event.setLocation(request.location().trim());
        event.setMaxCapacity(request.maxCapacity());
        event.setImageUrl(request.imageUrl().trim());
        event.setFee(request.fee() != null ? request.fee() : BigDecimal.ZERO);
    }
}