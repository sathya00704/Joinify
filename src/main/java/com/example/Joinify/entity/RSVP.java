package com.example.Joinify.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class RSVP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    private RSVPStatus status;

    private LocalDateTime rsvpDate;

    // Constructors, Getters, Setters
    public RSVP() {
        this.rsvpDate = LocalDateTime.now();
        this.status = RSVPStatus.PENDING;
    }

    public RSVP(Long id, User user, Event event, RSVPStatus status, LocalDateTime rsvpDate) {
        this.id = id;
        this.user = user;
        this.event = event;
        this.status = status;
        this.rsvpDate = rsvpDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public RSVPStatus getStatus() {
        return status;
    }

    public void setStatus(RSVPStatus status) {
        this.status = status;
    }

    public LocalDateTime getRsvpDate() {
        return rsvpDate;
    }

    public void setRsvpDate(LocalDateTime rsvpDate) {
        this.rsvpDate = rsvpDate;
    }
}


