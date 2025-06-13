package com.example.Joinify.repository;

import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // Find upcoming events (events after current date/time)
    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE e.dateTime > :currentDateTime ORDER BY e.dateTime ASC")
    List<Event> findUpcomingEvents(@Param("currentDateTime") LocalDateTime currentDateTime);

    // Find past events (events before current date/time)
    @Query("SELECT e FROM Event e WHERE e.dateTime < :currentDateTime ORDER BY e.dateTime DESC")
    List<Event> findPastEvents(@Param("currentDateTime") LocalDateTime currentDateTime);

    // Find events by organizer
    List<Event> findByOrganizer(User organizer);

    // Find events by organizer ID
    List<Event> findByOrganizerId(Long organizerId);

    // Find upcoming events by organizer
    @Query("SELECT e FROM Event e WHERE e.organizer.id = :organizerId AND e.dateTime > :currentDateTime ORDER BY e.dateTime ASC")
    List<Event> findUpcomingEventsByOrganizer(@Param("organizerId") Long organizerId, @Param("currentDateTime") LocalDateTime currentDateTime);

    // Find past events by organizer
    @Query("SELECT e FROM Event e WHERE e.organizer.id = :organizerId AND e.dateTime < :currentDateTime ORDER BY e.dateTime DESC")
    List<Event> findPastEventsByOrganizer(@Param("organizerId") Long organizerId, @Param("currentDateTime") LocalDateTime currentDateTime);

    // Find events by title containing keyword (case-insensitive)
    @Query("SELECT e FROM Event e WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Event> findByTitleContainingIgnoreCase(@Param("keyword") String keyword);

    // Find events by location containing keyword (case-insensitive)
    @Query("SELECT e FROM Event e WHERE LOWER(e.location) LIKE LOWER(CONCAT('%', :location, '%'))")
    List<Event> findByLocationContainingIgnoreCase(@Param("location") String location);

    // Find events within date range
    @Query("SELECT e FROM Event e WHERE e.dateTime BETWEEN :startDate AND :endDate ORDER BY e.dateTime ASC")
    List<Event> findEventsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find events with available capacity
    @Query("SELECT e FROM Event e WHERE e.dateTime > :currentDateTime AND " +
            "(SELECT COUNT(r) FROM RSVP r WHERE r.event = e AND r.status = 'CONFIRMED') < e.maxCapacity")
    List<Event> findEventsWithAvailableCapacity(@Param("currentDateTime") LocalDateTime currentDateTime);

    // Count events by organizer
    long countByOrganizerId(Long organizerId);

    // Find top 5 recent events
    @Query("SELECT e FROM Event e ORDER BY e.dateTime DESC")
    List<Event> findTop5ByOrderByDateTimeDesc();

    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE e.organizer.id = :organizerId AND e.dateTime > :currentDateTime ORDER BY e.dateTime ASC")
    List<Event> findUpcomingEventsByOrganizerWithDetails(@Param("organizerId") Long organizerId, @Param("currentDateTime") LocalDateTime currentDateTime);

    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE e.organizer.id = :organizerId AND e.dateTime < :currentDateTime ORDER BY e.dateTime DESC")
    List<Event> findPastEventsByOrganizerWithDetails(@Param("organizerId") Long organizerId, @Param("currentDateTime") LocalDateTime currentDateTime);

    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE e.dateTime > :currentDateTime ORDER BY e.dateTime ASC")
    List<Event> findUpcomingEventsWithOrganizer(@Param("currentDateTime") LocalDateTime currentDateTime);

    // JOIN FETCH for past events
    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE e.dateTime < :currentDateTime ORDER BY e.dateTime DESC")
    List<Event> findPastEventsWithOrganizer(@Param("currentDateTime") LocalDateTime currentDateTime);

    // JOIN FETCH for all events (general discover)
    @Query("SELECT e FROM Event e JOIN FETCH e.organizer ORDER BY e.dateTime ASC")
    List<Event> findAllEventsWithOrganizer();

    // JOIN FETCH for events with available capacity
    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE e.dateTime > :currentDateTime AND " +
            "(SELECT COUNT(r) FROM RSVP r WHERE r.event = e AND r.status = 'CONFIRMED') < e.maxCapacity " +
            "ORDER BY e.dateTime ASC")
    List<Event> findEventsWithAvailableCapacityAndOrganizer(@Param("currentDateTime") LocalDateTime currentDateTime);

    // JOIN FETCH for search by title
    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY e.dateTime ASC")
    List<Event> findByTitleContainingIgnoreCaseWithOrganizer(@Param("keyword") String keyword);

    // JOIN FETCH for search by location
    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE LOWER(e.location) LIKE LOWER(CONCAT('%', :location, '%')) ORDER BY e.dateTime ASC")
    List<Event> findByLocationContainingIgnoreCaseWithOrganizer(@Param("location") String location);

    // JOIN FETCH for events within date range
    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE e.dateTime BETWEEN :startDate AND :endDate ORDER BY e.dateTime ASC")
    List<Event> findEventsBetweenDatesWithOrganizer(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // JOIN FETCH for organizer's events
    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE e.organizer.id = :organizerId ORDER BY e.dateTime DESC")
    List<Event> findByOrganizerIdWithOrganizer(@Param("organizerId") Long organizerId);

    // JOIN FETCH for organizer's upcoming events
    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE e.organizer.id = :organizerId AND e.dateTime > :currentDateTime ORDER BY e.dateTime ASC")
    List<Event> findUpcomingEventsByOrganizerWithOrganizer(@Param("organizerId") Long organizerId, @Param("currentDateTime") LocalDateTime currentDateTime);

    // JOIN FETCH for organizer's past events
    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE e.organizer.id = :organizerId AND e.dateTime < :currentDateTime ORDER BY e.dateTime DESC")
    List<Event> findPastEventsByOrganizerWithOrganizer(@Param("organizerId") Long organizerId, @Param("currentDateTime") LocalDateTime currentDateTime);
}
