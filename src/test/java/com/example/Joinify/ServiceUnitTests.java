package com.example.Joinify;

import com.example.Joinify.dto.RegisterRequest;
import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.User;
import com.example.Joinify.entity.UserRole;
import com.example.Joinify.service.EventService;
import com.example.Joinify.service.RSVPService;
import com.example.Joinify.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class ServiceUnitTests {

    @Autowired
    private UserService userService;

    @Autowired
    private EventService eventService;

    @Autowired
    private RSVPService rsvpService;

    @Test
    public void testUserServiceOperations() {
        // Test user registration
        RegisterRequest request = new RegisterRequest("testUser", "test@example.com", "password123", UserRole.ORGANIZER);
        User savedUser = userService.registerUser(request);

        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testUser");

        // Test duplicate username
        RegisterRequest duplicateRequest = new RegisterRequest("testUser", "test2@example.com", "password123", UserRole.ATTENDEE);
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(duplicateRequest);
        });

        // Test user retrieval
        assertThat(userService.getUserByUsername("testUser")).isPresent();
        assertThat(userService.existsByUsername("testUser")).isTrue();
        assertThat(userService.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    public void testEventServiceOperations() {
        // Create organizer first
        RegisterRequest orgRequest = new RegisterRequest("eventOrg", "org@example.com", "password123", UserRole.ORGANIZER);
        User organizer = userService.registerUser(orgRequest);

        // Test event creation
        Event event = eventService.createEvent(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(7),
                "Test Location",
                100,
                organizer
        );

        assertThat(event).isNotNull();
        assertThat(event.getId()).isNotNull();
        assertThat(event.getTitle()).isEqualTo("Test Event");

        // Test event retrieval
        assertThat(eventService.getEventById(event.getId())).isPresent();

        List<Event> upcomingEvents = eventService.getUpcomingEvents();
        assertThat(upcomingEvents).contains(event);

        List<Event> organizerEvents = eventService.getEventsByOrganizerId(organizer.getId());
        assertThat(organizerEvents).contains(event);
    }

    @Test
    public void testRSVPServiceOperations() {
        // Create users and event
        RegisterRequest orgRequest = new RegisterRequest("rsvpOrg", "rsvporg@example.com", "password123", UserRole.ORGANIZER);
        RegisterRequest attRequest = new RegisterRequest("rsvpAtt", "rsvpatt@example.com", "password123", UserRole.ATTENDEE);

        User organizer = userService.registerUser(orgRequest);
        User attendee = userService.registerUser(attRequest);

        Event event = eventService.createEvent(
                "RSVP Test Event",
                "RSVP Test Description",
                LocalDateTime.now().plusDays(7),
                "RSVP Test Location",
                10,
                organizer
        );

        // Test RSVP creation
        assertThat(rsvpService.hasUserRSVPd(attendee.getId(), event.getId())).isFalse();

        rsvpService.createRSVP(attendee.getId(), event.getId());

        assertThat(rsvpService.hasUserRSVPd(attendee.getId(), event.getId())).isTrue();
        assertThat(rsvpService.getConfirmedRSVPCount(event.getId())).isEqualTo(1);
        assertThat(rsvpService.getAvailableSpots(event.getId())).isEqualTo(9);

        // Test RSVP cancellation
        rsvpService.cancelRSVP(attendee.getId(), event.getId());
        assertThat(rsvpService.hasUserRSVPd(attendee.getId(), event.getId())).isFalse();
        assertThat(rsvpService.getConfirmedRSVPCount(event.getId())).isEqualTo(0);
    }
}
