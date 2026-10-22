package com.example.Joinify.dto;

import com.example.Joinify.entity.RSVPStatus;

import java.time.LocalDateTime;

// One of the logged-in user's own RSVPs, with the event it belongs to
public record RsvpResponse(
        Long id,
        RSVPStatus status,
        LocalDateTime rsvpDate,
        EventResponse event
) {
}