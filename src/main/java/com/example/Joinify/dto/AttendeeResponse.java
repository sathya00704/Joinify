package com.example.Joinify.dto;

import com.example.Joinify.entity.RSVPStatus;

import java.time.LocalDateTime;

// One person who RSVP'd to an event, as seen by that event's organizer
public record AttendeeResponse(
        Long id,
        String username,
        String email,
        RSVPStatus rsvpStatus,
        LocalDateTime registrationDate
) {
}