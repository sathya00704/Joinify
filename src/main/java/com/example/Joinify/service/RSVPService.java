package com.example.Joinify.service;

import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.RSVP;
import com.example.Joinify.entity.RSVPStatus;
import com.example.Joinify.entity.User;
import com.example.Joinify.exception.BadRequestException;
import com.example.Joinify.exception.DuplicateResourceException;
import com.example.Joinify.exception.EventCapacityExceededException;
import com.example.Joinify.exception.ResourceNotFoundException;
import com.example.Joinify.repository.EventRepository;
import com.example.Joinify.repository.RSVPRepository;
import com.example.Joinify.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RSVPService {

    @Autowired
    private RSVPRepository rsvpRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    // Create RSVP
    public RSVP createRSVP(Long userId, Long eventId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        // Check if event is in the past
        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot RSVP to past events");
        }

        // Check capacity
        long confirmedCount = rsvpRepository.countConfirmedRSVPsByEventId(eventId);
        if (confirmedCount >= event.getMaxCapacity()) {
            throw new EventCapacityExceededException("Event '" + event.getTitle() + "' is at full capacity");
        }

        // Check if RSVP already exists
        if (rsvpRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw new DuplicateResourceException("User has already RSVP'd to this event");
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
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }
        if (status == null) {
            throw new BadRequestException("RSVP status cannot be null");
        }

        RSVP rsvp = rsvpRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("RSVP not found for user and event"));

        rsvp.setStatus(status);
        return rsvpRepository.save(rsvp);
    }

    // Cancel RSVP (delete it)
    public void cancelRSVP(Long userId, Long eventId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }

        RSVP rsvp = rsvpRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("RSVP not found for user and event"));

        rsvpRepository.delete(rsvp);
    }

    // Get RSVP by user and event
    public Optional<RSVP> getRSVP(Long userId, Long eventId) {
        if (userId == null || eventId == null) {
            throw new BadRequestException("User ID and Event ID cannot be null");
        }
        return rsvpRepository.findByUserIdAndEventId(userId, eventId);
    }

    // Get RSVP status for user and event
    public Optional<RSVPStatus> getRSVPStatus(Long userId, Long eventId) {
        if (userId == null || eventId == null) {
            throw new BadRequestException("User ID and Event ID cannot be null");
        }
        return rsvpRepository.findRSVPStatus(userId, eventId);
    }

    // Check if user has RSVP'd to event
    public boolean hasUserRSVPd(Long userId, Long eventId) {
        if (userId == null || eventId == null) {
            throw new BadRequestException("User ID and Event ID cannot be null");
        }
        return rsvpRepository.existsByUserIdAndEventId(userId, eventId);
    }

    // Get all RSVPs for an event
    public List<RSVP> getRSVPsForEvent(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }
        return rsvpRepository.findByEventId(eventId);
    }

    // Get confirmed attendees for an event
    public List<User> getConfirmedAttendeesForEvent(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }
        return rsvpRepository.findConfirmedAttendeesByEventId(eventId);
    }

    // Get pending RSVPs for an event
    public List<RSVP> getPendingRSVPsForEvent(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }
        return rsvpRepository.findPendingRSVPsByEventId(eventId);
    }

    // Check if event is at capacity
    public boolean isEventAtCapacity(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }
        return rsvpRepository.isEventAtCapacity(eventId);
    }

    // Get confirmed RSVP count for an event
    public long getConfirmedRSVPCount(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }
        return rsvpRepository.countConfirmedRSVPsByEventId(eventId);
    }

    // Get total RSVP count for an event
    public long getTotalRSVPCount(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }
        return rsvpRepository.countByEventId(eventId);
    }

    // Get all RSVPs for a user
    public List<RSVP> getRSVPsForUser(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        return rsvpRepository.findByUserId(userId);
    }

    // Get upcoming RSVPs for a user
    public List<RSVP> getUpcomingRSVPsForUser(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        return rsvpRepository.findUserUpcomingRSVPs(userId, LocalDateTime.now());
    }

    // Get past RSVPs for a user
    public List<RSVP> getPastRSVPsForUser(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        return rsvpRepository.findUserPastRSVPs(userId, LocalDateTime.now());
    }

    // Get RSVPs by status
    public List<RSVP> getRSVPsByStatus(RSVPStatus status) {
        if (status == null) {
            throw new BadRequestException("RSVP status cannot be null");
        }
        return rsvpRepository.findByStatus(status);
    }

    // Get available spots for an event
    public int getAvailableSpots(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        long confirmedCount = rsvpRepository.countConfirmedRSVPsByEventId(eventId);
        return (int) Math.max(0, event.getMaxCapacity() - confirmedCount);
    }

    // Count RSVPs by user
    public long countRSVPsByUser(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        return rsvpRepository.countByUserId(userId);
    }

    // Bulk confirm pending RSVPs for an event
    public List<RSVP> confirmPendingRSVPs(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("Event ID cannot be null");
        }

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

//    public List<RSVP> getUserRSVPs(Long userId) {
//        return rsvpRepository.findByUserIdWithDetails(userId);
//    }
//
//    public List<RSVP> getUserUpcomingRSVPs(Long userId) {
//        return rsvpRepository.findUserUpcomingRSVPsWithDetails(userId, LocalDateTime.now());
//    }
//
//    public List<RSVP> getUserPastRSVPs(Long userId) {
//        return rsvpRepository.findUserPastRSVPsWithDetails(userId, LocalDateTime.now());
//    }

    public List<RSVP> getEventRSVPs(Long eventId) {
        return rsvpRepository.findByEventIdWithUser(eventId);
    }

    public List<User> getConfirmedAttendees(Long eventId) {
        List<RSVP> confirmedRSVPs = rsvpRepository.findConfirmedRSVPsByEventIdWithUser(eventId);
        return confirmedRSVPs.stream()
                .map(RSVP::getUser)
                .collect(Collectors.toList());
    }
}
