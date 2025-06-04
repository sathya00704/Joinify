package com.example.Joinify.service;

import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.RSVP;
import com.example.Joinify.entity.RSVPStatus;
import com.example.Joinify.entity.User;
import com.example.Joinify.repository.EventRepository;
import com.example.Joinify.repository.RSVPRepository;
import com.example.Joinify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RSVPService {

    @Autowired
    private RSVPRepository rsvpRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    // Create RSVP
    public RSVP createRSVP(Long userId, Long eventId) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Event> eventOpt = eventRepository.findById(eventId);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        if (eventOpt.isEmpty()) {
            throw new IllegalArgumentException("Event not found");
        }

        Event event = eventOpt.get();
        User user = userOpt.get();

        // Check if event is in the past
        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot RSVP to past events");
        }

        // Check capacity
        long confirmedCount = rsvpRepository.countConfirmedRSVPsByEventId(eventId);
        if (confirmedCount >= event.getMaxCapacity()) {
            throw new IllegalStateException("Event is at full capacity");
        }

        // Check if RSVP already exists
        if (rsvpRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw new IllegalStateException("User has already RSVP'd to this event");
        }

        RSVP rsvp = new RSVP();
        rsvp.setUser(user);
        rsvp.setEvent(event);
        rsvp.setStatus(RSVPStatus.CONFIRMED);
        rsvp.setRsvpDate(LocalDateTime.now());

        return rsvpRepository.save(rsvp);
    }

    // Update RSVP status
    public RSVP updateRSVPStatus(Long userId, Long eventId, RSVPStatus status) {
        Optional<RSVP> rsvpOpt = rsvpRepository.findByUserIdAndEventId(userId, eventId);
        if (rsvpOpt.isEmpty()) {
            throw new IllegalArgumentException("RSVP not found for user and event");
        }

        RSVP rsvp = rsvpOpt.get();
        rsvp.setStatus(status);
        return rsvpRepository.save(rsvp);
    }

    // Cancel RSVP (delete it)
    public void cancelRSVP(Long userId, Long eventId) {
        Optional<RSVP> rsvpOpt = rsvpRepository.findByUserIdAndEventId(userId, eventId);
        if (rsvpOpt.isEmpty()) {
            throw new IllegalArgumentException("RSVP not found for user and event");
        }
        rsvpRepository.delete(rsvpOpt.get());
    }

    // Get RSVP by user and event
    public Optional<RSVP> getRSVP(Long userId, Long eventId) {
        return rsvpRepository.findByUserIdAndEventId(userId, eventId);
    }

    // Get RSVP status for user and event
    public Optional<RSVPStatus> getRSVPStatus(Long userId, Long eventId) {
        return rsvpRepository.findRSVPStatus(userId, eventId);
    }

    // Check if user has RSVP'd to event
    public boolean hasUserRSVPd(Long userId, Long eventId) {
        return rsvpRepository.existsByUserIdAndEventId(userId, eventId);
    }

    // Get all RSVPs for an event
    public List<RSVP> getRSVPsForEvent(Long eventId) {
        return rsvpRepository.findByEventId(eventId);
    }

    // Get confirmed attendees for an event
    public List<User> getConfirmedAttendeesForEvent(Long eventId) {
        return rsvpRepository.findConfirmedAttendeesByEventId(eventId);
    }

    // Get pending RSVPs for an event
    public List<RSVP> getPendingRSVPsForEvent(Long eventId) {
        return rsvpRepository.findPendingRSVPsByEventId(eventId);
    }

    // Check if event is at capacity
    public boolean isEventAtCapacity(Long eventId) {
        return rsvpRepository.isEventAtCapacity(eventId);
    }

    // Get confirmed RSVP count for an event
    public long getConfirmedRSVPCount(Long eventId) {
        return rsvpRepository.countConfirmedRSVPsByEventId(eventId);
    }

    // Get total RSVP count for an event
    public long getTotalRSVPCount(Long eventId) {
        return rsvpRepository.countByEventId(eventId);
    }

    // Get all RSVPs for a user
    public List<RSVP> getRSVPsForUser(Long userId) {
        return rsvpRepository.findByUserId(userId);
    }

    // Get upcoming RSVPs for a user
    public List<RSVP> getUpcomingRSVPsForUser(Long userId) {
        return rsvpRepository.findUserUpcomingRSVPs(userId, LocalDateTime.now());
    }

    // Get past RSVPs for a user
    public List<RSVP> getPastRSVPsForUser(Long userId) {
        return rsvpRepository.findUserPastRSVPs(userId, LocalDateTime.now());
    }

    // Get RSVPs by status
    public List<RSVP> getRSVPsByStatus(RSVPStatus status) {
        return rsvpRepository.findByStatus(status);
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

    // Count RSVPs by user
    public long countRSVPsByUser(Long userId) {
        return rsvpRepository.countByUserId(userId);
    }

    // Bulk confirm pending RSVPs for an event
    public List<RSVP> confirmPendingRSVPs(Long eventId) {
        List<RSVP> pendingRSVPs = rsvpRepository.findPendingRSVPsByEventId(eventId);

        for (RSVP rsvp : pendingRSVPs) {
            if (!isEventAtCapacity(eventId)) {
                rsvp.setStatus(RSVPStatus.CONFIRMED);
                rsvpRepository.save(rsvp);
            } else {
                break; // Stop if capacity is reached
            }
        }

        return rsvpRepository.findConfirmedRSVPsByEventId(eventId);
    }
}
