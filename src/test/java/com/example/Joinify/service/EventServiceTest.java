package com.example.Joinify.service;

import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.User;
import com.example.Joinify.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.example.Joinify.dto.RegisterRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class EventServiceTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserService userService;

    @Test
    public void testCreateEvent() {
        // Given
        User organizer = new User();
        organizer.setUsername("organizer123");
        organizer.setEmail("organizer123@example.com");
        organizer.setPassword("password");
        organizer.setRole(UserRole.ORGANIZER);

        // SAVE the user first to get an ID
        User savedOrganizer = userService.registerUser(new RegisterRequest(
                "organizer123",
                "organizer123@example.com",
                "password",
                UserRole.ORGANIZER
        ));

        // When
        Event event = eventService.createEvent(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(7),
                "Test Location",
                100,
                savedOrganizer
        );

        // Then
        assertThat(event).isNotNull();
        assertThat(event.getId()).isNotNull();
        assertThat(event.getTitle()).isEqualTo("Test Event");
        assertThat(event.getMaxCapacity()).isEqualTo(100);
    }

    @Test
    public void testGetUpcomingEvents() {
        // When
        List<Event> upcomingEvents = eventService.getUpcomingEvents();

        // Then
        assertThat(upcomingEvents).isNotNull();
        // All events should be in the future
        upcomingEvents.forEach(event ->
                assertThat(event.getDateTime()).isAfter(LocalDateTime.now())
        );
    }
}
