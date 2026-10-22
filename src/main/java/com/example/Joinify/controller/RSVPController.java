package com.example.Joinify.controller;

import com.example.Joinify.dto.AttendeeResponse;
import com.example.Joinify.dto.RsvpResponse;
import com.example.Joinify.entity.RSVPStatus;
import com.example.Joinify.exception.UnauthorizedException;
import com.example.Joinify.service.RSVPService;
import com.example.Joinify.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rsvp")
@CrossOrigin(origins = "*")
public class RSVPController {

    @Autowired
    private RSVPService rsvpService;

    @Autowired
    private UserService userService;

    // ===================== Current user's RSVP for one event =====================

    @PostMapping("/event/{eventId}")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<RsvpResponse> createRSVP(@PathVariable Long eventId, Authentication authentication) {
        RsvpResponse rsvp = rsvpService.createRSVP(currentUserId(authentication), eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(rsvp);
    }

    @DeleteMapping("/event/{eventId}")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<Void> cancelRSVP(@PathVariable Long eventId, Authentication authentication) {
        rsvpService.cancelRSVP(currentUserId(authentication), eventId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/event/{eventId}/status")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<RsvpResponse> updateRSVPStatus(@PathVariable Long eventId,
                                                         @RequestParam RSVPStatus status,
                                                         Authentication authentication) {
        return ResponseEntity.ok(rsvpService.updateRSVPStatus(currentUserId(authentication), eventId, status));
    }

    @GetMapping("/event/{eventId}/status")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<RSVPStatus> getRSVPStatus(@PathVariable Long eventId, Authentication authentication) {
        return rsvpService.getRSVPStatus(currentUserId(authentication), eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/event/{eventId}/check")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<Boolean> hasUserRSVPd(@PathVariable Long eventId, Authentication authentication) {
        return ResponseEntity.ok(rsvpService.hasUserRSVPd(currentUserId(authentication), eventId));
    }

    // ===================== Current user's RSVP lists =====================

    @GetMapping("/my-rsvps")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<List<RsvpResponse>> getMyRSVPs(Authentication authentication) {
        return ResponseEntity.ok(rsvpService.getMyRSVPs(currentUserId(authentication)));
    }

    @GetMapping("/my-rsvps/upcoming")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<List<RsvpResponse>> getMyUpcomingRSVPs(Authentication authentication) {
        return ResponseEntity.ok(rsvpService.getUpcomingRSVPsForUser(currentUserId(authentication)));
    }

    @GetMapping("/my-rsvps/past")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<List<RsvpResponse>> getMyPastRSVPs(Authentication authentication) {
        return ResponseEntity.ok(rsvpService.getPastRSVPsForUser(currentUserId(authentication)));
    }

    // ===================== Organizer views of an event =====================

    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<AttendeeResponse>> getRSVPsForEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(rsvpService.getRSVPsForEvent(eventId));
    }

    @GetMapping("/event/{eventId}/attendees")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<AttendeeResponse>> getConfirmedAttendeesForEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(rsvpService.getConfirmedAttendeesForEvent(eventId));
    }

    @GetMapping("/event/{eventId}/pending")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<AttendeeResponse>> getPendingRSVPsForEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(rsvpService.getPendingRSVPsForEvent(eventId));
    }

    @PostMapping("/event/{eventId}/confirm-pending")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<AttendeeResponse>> confirmPendingRSVPs(@PathVariable Long eventId) {
        return ResponseEntity.ok(rsvpService.confirmPendingRSVPs(eventId));
    }

    // ===================== Public counts =====================

    public static class RSVPCountResponse {
        public final long confirmed;
        public final long total;
        public final int available;
        public final boolean atCapacity;

        public RSVPCountResponse(long confirmed, long total, int available, boolean atCapacity) {
            this.confirmed = confirmed;
            this.total = total;
            this.available = available;
            this.atCapacity = atCapacity;
        }
    }

    @GetMapping("/event/{eventId}/count")
    public ResponseEntity<RSVPCountResponse> getRSVPCounts(@PathVariable Long eventId) {
        RSVPCountResponse response = new RSVPCountResponse(
                rsvpService.getConfirmedRSVPCount(eventId),
                rsvpService.getTotalRSVPCount(eventId),
                rsvpService.getAvailableSpots(eventId),
                rsvpService.isEventAtCapacity(eventId)
        );
        return ResponseEntity.ok(response);
    }

    // ===================== Helper =====================

    private Long currentUserId(Authentication authentication) {
        return userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("User not found"))
                .getId();
    }
}