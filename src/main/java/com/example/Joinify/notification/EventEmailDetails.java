package com.example.Joinify.notification;

import com.example.Joinify.entity.Event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// A plain snapshot of the event for emails. Built inside the transaction,
// so the background email thread never touches the database or lazy relations.
public record EventEmailDetails(
        String title,
        String description,
        LocalDateTime dateTime,
        String location,
        String organizerName,
        String organizerEmail,
        BigDecimal fee
) {
    public static EventEmailDetails from(Event event) {
        return new EventEmailDetails(
                event.getTitle(),
                event.getDescription(),
                event.getDateTime(),
                event.getLocation(),
                event.getOrganizer().getUsername(),
                event.getOrganizer().getEmail(),
                event.getFee()
        );
    }
}