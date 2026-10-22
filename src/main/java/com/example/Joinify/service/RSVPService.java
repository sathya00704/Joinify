package com.example.Joinify.service;

import com.example.Joinify.dto.AttendeeResponse;
import com.example.Joinify.dto.RsvpResponse;
import com.example.Joinify.entity.*;
import com.example.Joinify.exception.BadRequestException;
import com.example.Joinify.exception.DuplicateResourceException;
import com.example.Joinify.exception.EventCapacityExceededException;
import com.example.Joinify.exception.ResourceNotFoundException;
import com.example.Joinify.mapper.DtoMapper;
import com.example.Joinify.repository.EventRepository;
import com.example.Joinify.repository.RSVPRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RSVPService {

    @Autowired
    private RSVPRepository rsvpRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventService eventService;

    @Autowired
    private UserService userService;

    // ===================== Create / update / cancel =====================

    public RsvpResponse createRSVP(Long userId, Long eventId) {
        validateRSVPCreation(userId, eventId);

        Event event = eventService.getEventById(eventId);
        User user = userService.getUserById(userId);

        RSVP rsvp = new RSVP();
        rsvp.setUser(user);
        rsvp.setEvent(event);
        rsvp.setStatus(RSVPStatus.CONFIRMED);
        rsvp.setRsvpDate(LocalDateTime.now());

        return DtoMapper.toRsvpResponse(rsvpRepository.save(rsvp));
    }

    public RsvpResponse updateRSVPStatus(Long userId, Long eventId, RSVPStatus status) {
        RSVP rsvp = rsvpRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("RSVP not found for this event"));

        rsvp.setStatus(status);
        return DtoMapper.toRsvpResponse(rsvpRepository.save(rsvp));
    }

    public void cancelRSVP(Long userId, Long eventId) {
        RSVP rsvp = rsvpRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("RSVP not found for this event"));
        rsvpRepository.delete(rsvp);
    }

    // ===================== Current user's RSVPs =====================

    public Optional<RSVPStatus> getRSVPStatus(Long userId, Long eventId) {
        return rsvpRepository.findRSVPStatus(userId, eventId);
    }

    public boolean hasUserRSVPd(Long userId, Long eventId) {
        return rsvpRepository.existsByUserIdAndEventId(userId, eventId);
    }

    public List<RsvpResponse> getMyRSVPs(Long userId) {
        return DtoMapper.toRsvpResponses(rsvpRepository.findByUserIdWithEventDetails(userId));
    }

    public List<RsvpResponse> getUpcomingRSVPsForUser(Long userId) {
        return DtoMapper.toRsvpResponses(rsvpRepository.findUserUpcomingRSVPs(userId, LocalDateTime.now()));
    }

    public List<RsvpResponse> getPastRSVPsForUser(Long userId) {
        return DtoMapper.toRsvpResponses(rsvpRepository.findUserPastRSVPs(userId, LocalDateTime.now()));
    }

    // ===================== Organizer views of an event =====================

    public List<AttendeeResponse> getRSVPsForEvent(Long eventId) {
        return DtoMapper.toAttendeeResponses(rsvpRepository.findByEventIdWithUser(eventId));
    }

    public List<AttendeeResponse> getConfirmedAttendeesForEvent(Long eventId) {
        return DtoMapper.toAttendeeResponses(rsvpRepository.findConfirmedRSVPsByEventIdWithUser(eventId));
    }

    public List<AttendeeResponse> getPendingRSVPsForEvent(Long eventId) {
        return DtoMapper.toAttendeeResponses(rsvpRepository.findPendingRSVPsByEventId(eventId));
    }

    public List<AttendeeResponse> confirmPendingRSVPs(Long eventId) {
        List<RSVP> pendingRSVPs = rsvpRepository.findPendingRSVPsByEventId(eventId);

        for (RSVP rsvp : pendingRSVPs) {
            if (isEventAtCapacity(eventId)) {
                break; // Stop if capacity is reached
            }
            rsvp.setStatus(RSVPStatus.CONFIRMED);
            rsvpRepository.save(rsvp);
        }

        return DtoMapper.toAttendeeResponses(rsvpRepository.findConfirmedRSVPsByEventIdWithUser(eventId));
    }

    // ===================== Counts (unchanged) =====================

    public boolean isEventAtCapacity(Long eventId) {
        return rsvpRepository.isEventAtCapacity(eventId);
    }

    public long getConfirmedRSVPCount(Long eventId) {
        return rsvpRepository.countConfirmedRSVPsByEventId(eventId);
    }

    public long getTotalRSVPCount(Long eventId) {
        return rsvpRepository.countByEventId(eventId);
    }

    public int getAvailableSpots(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        long confirmedCount = rsvpRepository.countConfirmedRSVPsByEventId(eventId);
        return (int) Math.max(0, event.getMaxCapacity() - confirmedCount);
    }

    // ===================== Validation (unchanged) =====================

    private void validateRSVPCreation(Long userId, Long eventId) {
        if (rsvpRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw new DuplicateResourceException("You have already registered for this event");
        }

        Event event = eventService.getEventById(eventId);

        if (event.getRegistrationStatus() == RegistrationStatus.CLOSED) {
            throw new BadRequestException("Registration is closed for this event");
        }

        if (event.getRegistrationStatus() == RegistrationStatus.SUSPENDED) {
            throw new BadRequestException("Registration is temporarily suspended for this event");
        }

        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot register for past events");
        }

        if (isEventAtCapacity(eventId)) {
            throw new EventCapacityExceededException("Event is at full capacity");
        }
    }
}