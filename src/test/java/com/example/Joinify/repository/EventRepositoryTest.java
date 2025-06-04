package com.example.Joinify.repository;

import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.User;
import com.example.Joinify.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional  // This will rollback changes after each test
public class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testFindUpcomingEvents() {
        // Given
        User organizer = new User();
        organizer.setUsername("organizer1test");
        organizer.setEmail("org1test@example.com");
        organizer.setPassword("password");
        organizer.setRole(UserRole.ORGANIZER);
        User savedOrganizer = userRepository.save(organizer);

        Event futureEvent = new Event();
        futureEvent.setTitle("Future Event Test");
        futureEvent.setDescription("This is a future event");
        futureEvent.setDateTime(LocalDateTime.now().plusDays(5));
        futureEvent.setLocation("Future Location");
        futureEvent.setMaxCapacity(100);
        futureEvent.setOrganizer(savedOrganizer);

        Event pastEvent = new Event();
        pastEvent.setTitle("Past Event Test");
        pastEvent.setDescription("This is a past event");
        pastEvent.setDateTime(LocalDateTime.now().minusDays(5));
        pastEvent.setLocation("Past Location");
        pastEvent.setMaxCapacity(50);
        pastEvent.setOrganizer(savedOrganizer);

        eventRepository.save(futureEvent);
        eventRepository.save(pastEvent);

        // When
        List<Event> upcomingEvents = eventRepository.findUpcomingEvents(LocalDateTime.now());

        // Then
        assertThat(upcomingEvents).hasSizeGreaterThanOrEqualTo(1);
        assertThat(upcomingEvents).anyMatch(event -> event.getTitle().equals("Future Event Test"));
    }

    @Test
    public void testFindPastEvents() {
        // Given
        User organizer = new User();
        organizer.setUsername("organizer2test");
        organizer.setEmail("org2test@example.com");
        organizer.setPassword("password");
        organizer.setRole(UserRole.ORGANIZER);
        User savedOrganizer = userRepository.save(organizer);

        Event pastEvent1 = new Event();
        pastEvent1.setTitle("Past Event 1");
        pastEvent1.setDescription("This is past event 1");
        pastEvent1.setDateTime(LocalDateTime.now().minusDays(10));
        pastEvent1.setLocation("Past Location 1");
        pastEvent1.setMaxCapacity(50);
        pastEvent1.setOrganizer(savedOrganizer);

        Event pastEvent2 = new Event();
        pastEvent2.setTitle("Past Event 2");
        pastEvent2.setDescription("This is past event 2");
        pastEvent2.setDateTime(LocalDateTime.now().minusDays(3));
        pastEvent2.setLocation("Past Location 2");
        pastEvent2.setMaxCapacity(75);
        pastEvent2.setOrganizer(savedOrganizer);

        Event futureEvent = new Event();
        futureEvent.setTitle("Future Event");
        futureEvent.setDescription("This is a future event");
        futureEvent.setDateTime(LocalDateTime.now().plusDays(2));
        futureEvent.setLocation("Future Location");
        futureEvent.setMaxCapacity(100);
        futureEvent.setOrganizer(savedOrganizer);

        eventRepository.save(pastEvent1);
        eventRepository.save(pastEvent2);
        eventRepository.save(futureEvent);

        // When
        List<Event> pastEvents = eventRepository.findPastEvents(LocalDateTime.now());

        // Then
        assertThat(pastEvents).hasSizeGreaterThanOrEqualTo(2);
        assertThat(pastEvents).anyMatch(event -> event.getTitle().equals("Past Event 1"));
        assertThat(pastEvents).anyMatch(event -> event.getTitle().equals("Past Event 2"));
        assertThat(pastEvents).noneMatch(event -> event.getTitle().equals("Future Event"));
    }

    @Test
    public void testFindEventsByOrganizer() {
        // Given
        User organizer1 = new User();
        organizer1.setUsername("organizer3test");
        organizer1.setEmail("org3test@example.com");
        organizer1.setPassword("password");
        organizer1.setRole(UserRole.ORGANIZER);
        User savedOrganizer1 = userRepository.save(organizer1);

        User organizer2 = new User();
        organizer2.setUsername("organizer4test");
        organizer2.setEmail("org4test@example.com");
        organizer2.setPassword("password");
        organizer2.setRole(UserRole.ORGANIZER);
        User savedOrganizer2 = userRepository.save(organizer2);

        Event event1 = new Event();
        event1.setTitle("Event by Organizer 1");
        event1.setDescription("Event organized by organizer 1");
        event1.setDateTime(LocalDateTime.now().plusDays(1));
        event1.setLocation("Location 1");
        event1.setMaxCapacity(50);
        event1.setOrganizer(savedOrganizer1);

        Event event2 = new Event();
        event2.setTitle("Another Event by Organizer 1");
        event2.setDescription("Another event by organizer 1");
        event2.setDateTime(LocalDateTime.now().plusDays(3));
        event2.setLocation("Location 2");
        event2.setMaxCapacity(75);
        event2.setOrganizer(savedOrganizer1);

        Event event3 = new Event();
        event3.setTitle("Event by Organizer 2");
        event3.setDescription("Event by organizer 2");
        event3.setDateTime(LocalDateTime.now().plusDays(2));
        event3.setLocation("Location 3");
        event3.setMaxCapacity(100);
        event3.setOrganizer(savedOrganizer2);

        eventRepository.save(event1);
        eventRepository.save(event2);
        eventRepository.save(event3);

        // When
        List<Event> organizer1Events = eventRepository.findByOrganizerId(savedOrganizer1.getId());

        // Then
        assertThat(organizer1Events).hasSize(2);
        assertThat(organizer1Events).allMatch(event -> event.getOrganizer().getId().equals(savedOrganizer1.getId()));
        assertThat(organizer1Events).anyMatch(event -> event.getTitle().equals("Event by Organizer 1"));
        assertThat(organizer1Events).anyMatch(event -> event.getTitle().equals("Another Event by Organizer 1"));
    }

    @Test
    public void testFindEventsByTitleContaining() {
        // Given
        User organizer = new User();
        organizer.setUsername("organizer5test");
        organizer.setEmail("org5test@example.com");
        organizer.setPassword("password");
        organizer.setRole(UserRole.ORGANIZER);
        User savedOrganizer = userRepository.save(organizer);

        Event techEvent = new Event();
        techEvent.setTitle("Tech Conference 2024");
        techEvent.setDescription("Annual tech conference");
        techEvent.setDateTime(LocalDateTime.now().plusDays(7));
        techEvent.setLocation("Tech Center");
        techEvent.setMaxCapacity(200);
        techEvent.setOrganizer(savedOrganizer);

        Event musicEvent = new Event();
        musicEvent.setTitle("Music Festival");
        musicEvent.setDescription("Summer music festival");
        musicEvent.setDateTime(LocalDateTime.now().plusDays(14));
        musicEvent.setLocation("Music Park");
        musicEvent.setMaxCapacity(500);
        musicEvent.setOrganizer(savedOrganizer);

        eventRepository.save(techEvent);
        eventRepository.save(musicEvent);

        // When
        List<Event> techEvents = eventRepository.findByTitleContainingIgnoreCase("tech");

        // Then
        assertThat(techEvents).hasSizeGreaterThanOrEqualTo(1);
        assertThat(techEvents).anyMatch(event -> event.getTitle().contains("Tech"));
    }
}
