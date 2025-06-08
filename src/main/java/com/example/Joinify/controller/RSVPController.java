package com.example.Joinify.controller;

import com.example.Joinify.entity.RSVP;
import com.example.Joinify.entity.RSVPStatus;
import com.example.Joinify.entity.User;
import com.example.Joinify.service.RSVPService;
import com.example.Joinify.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rsvp")
@CrossOrigin(origins = "*")
public class RSVPController {

    @Autowired
    private RSVPService rsvpService;

    @Autowired
    private UserService userService;

    // Create RSVP (Attendees only)
    @PostMapping("/event/{eventId}")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<RSVP> createRSVP(@PathVariable Long eventId, Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> user = userService.getUserByUsername(username);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            RSVP rsvp = rsvpService.createRSVP(user.get().getId(), eventId);
            return ResponseEntity.status(HttpStatus.CREATED).body(rsvp);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Cancel RSVP (Attendees only)
    @DeleteMapping("/event/{eventId}")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<Void> cancelRSVP(@PathVariable Long eventId, Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> user = userService.getUserByUsername(username);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            rsvpService.cancelRSVP(user.get().getId(), eventId);
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Update RSVP status
    @PutMapping("/event/{eventId}/status")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<RSVP> updateRSVPStatus(@PathVariable Long eventId,
                                                 @RequestParam RSVPStatus status,
                                                 Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> user = userService.getUserByUsername(username);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            RSVP updatedRSVP = rsvpService.updateRSVPStatus(user.get().getId(), eventId, status);
            return ResponseEntity.ok(updatedRSVP);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get RSVP status for current user and event
    // Get RSVP status for current user and event
    @GetMapping("/event/{eventId}/status")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<RSVPStatus> getRSVPStatus(@PathVariable Long eventId, Authentication authentication) {
        String username = authentication.getName();
        Optional<User> user = userService.getUserByUsername(username);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<RSVPStatus> status = rsvpService.getRSVPStatus(user.get().getId(), eventId);
        if (status.isPresent()) {
            return ResponseEntity.ok(status.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    // Check if user has RSVP'd to event
    // Check if user has RSVP'd to event
    @GetMapping("/event/{eventId}/check")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<Boolean> hasUserRSVPd(@PathVariable Long eventId, Authentication authentication) {
        String username = authentication.getName();
        Optional<User> user = userService.getUserByUsername(username);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean hasRSVPd = rsvpService.hasUserRSVPd(user.get().getId(), eventId);
        return ResponseEntity.ok(hasRSVPd);
    }


    // Get all RSVPs for an event (Organizers only)
    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<RSVP>> getRSVPsForEvent(@PathVariable Long eventId) {
        List<RSVP> rsvps = rsvpService.getRSVPsForEvent(eventId);
        return ResponseEntity.ok(rsvps);
    }

    // Get confirmed attendees for an event (Organizers only)
    @GetMapping("/event/{eventId}/attendees")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<User>> getConfirmedAttendeesForEvent(@PathVariable Long eventId) {
        List<User> attendees = rsvpService.getConfirmedAttendeesForEvent(eventId);
        return ResponseEntity.ok(attendees);
    }

    // Get pending RSVPs for an event (Organizers only)
    @GetMapping("/event/{eventId}/pending")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<RSVP>> getPendingRSVPsForEvent(@PathVariable Long eventId) {
        List<RSVP> pendingRSVPs = rsvpService.getPendingRSVPsForEvent(eventId);
        return ResponseEntity.ok(pendingRSVPs);
    }

    // Get my RSVPs (current user)
    @GetMapping("/my-rsvps")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<List<RSVP>> getMyRSVPs(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> user = userService.getUserByUsername(username);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<RSVP> rsvps = rsvpService.getRSVPsForUser(user.get().getId());
        return ResponseEntity.ok(rsvps);
    }

    // Get my upcoming RSVPs
    @GetMapping("/my-rsvps/upcoming")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<List<RSVP>> getMyUpcomingRSVPs(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> user = userService.getUserByUsername(username);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<RSVP> rsvps = rsvpService.getUpcomingRSVPsForUser(user.get().getId());
        return ResponseEntity.ok(rsvps);
    }

    // Get my past RSVPs
    @GetMapping("/my-rsvps/past")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<List<RSVP>> getPastRSVPs(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> user = userService.getUserByUsername(username);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<RSVP> rsvps = rsvpService.getPastRSVPsForUser(user.get().getId());
        return ResponseEntity.ok(rsvps);
    }

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
        long confirmedCount = rsvpService.getConfirmedRSVPCount(eventId);
        long totalCount = rsvpService.getTotalRSVPCount(eventId);
        int availableSpots = rsvpService.getAvailableSpots(eventId);
        boolean atCapacity = rsvpService.isEventAtCapacity(eventId);

        RSVPCountResponse response = new RSVPCountResponse(confirmedCount, totalCount, availableSpots, atCapacity);
        return ResponseEntity.ok(response);
    }


    // Bulk confirm pending RSVPs (Organizers only)
    @PostMapping("/event/{eventId}/confirm-pending")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<RSVP>> confirmPendingRSVPs(@PathVariable Long eventId) {
        try {
            List<RSVP> confirmedRSVPs = rsvpService.confirmPendingRSVPs(eventId);
            return ResponseEntity.ok(confirmedRSVPs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
