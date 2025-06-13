package com.example.Joinify.repository;

import com.example.Joinify.entity.RSVP;
import com.example.Joinify.entity.RSVPStatus;
import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RSVPRepository extends JpaRepository<RSVP, Long> {

    // Check if user has RSVP'd to an event
    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    // Find RSVP by user and event
    Optional<RSVP> findByUserIdAndEventId(Long userId, Long eventId);

    // Find RSVP by user and event objects
    Optional<RSVP> findByUserAndEvent(User user, Event event);

    // Find all RSVPs for a specific event
    List<RSVP> findByEventId(Long eventId);

    // Find all RSVPs by a specific user
    List<RSVP> findByUserId(Long userId);

    // Count total RSVPs for an event
    long countByEventId(Long eventId);

    // Count confirmed RSVPs for an event
    @Query("SELECT COUNT(r) FROM RSVP r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    long countConfirmedRSVPsByEventId(@Param("eventId") Long eventId);

    // Find RSVPs by status
    List<RSVP> findByStatus(RSVPStatus status);

    // Find confirmed RSVPs for an event
    @Query("SELECT r FROM RSVP r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    List<RSVP> findConfirmedRSVPsByEventId(@Param("eventId") Long eventId);

    // Find pending RSVPs for an event
    @Query("SELECT r FROM RSVP r WHERE r.event.id = :eventId AND r.status = 'PENDING'")
    List<RSVP> findPendingRSVPsByEventId(@Param("eventId") Long eventId);

    // Find user's upcoming event RSVPs
    @Query("SELECT r FROM RSVP r WHERE r.user.id = :userId AND r.event.dateTime > :currentDateTime AND r.status = 'CONFIRMED' ORDER BY r.event.dateTime ASC")
    List<RSVP> findUserUpcomingRSVPs(@Param("userId") Long userId, @Param("currentDateTime") LocalDateTime currentDateTime);

    // Find user's past event RSVPs
    @Query("SELECT r FROM RSVP r WHERE r.user.id = :userId AND r.event.dateTime < :currentDateTime ORDER BY r.event.dateTime DESC")
    List<RSVP> findUserPastRSVPs(@Param("userId") Long userId, @Param("currentDateTime") LocalDateTime currentDateTime);

    // Check RSVP status for user and event
    @Query("SELECT r.status FROM RSVP r WHERE r.user.id = :userId AND r.event.id = :eventId")
    Optional<RSVPStatus> findRSVPStatus(@Param("userId") Long userId, @Param("eventId") Long eventId);

    // Find all attendees for an event (confirmed RSVPs)
    @Query("SELECT r.user FROM RSVP r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    List<User> findConfirmedAttendeesByEventId(@Param("eventId") Long eventId);

    // Find RSVPs created within date range
    @Query("SELECT r FROM RSVP r WHERE r.rsvpDate BETWEEN :startDate AND :endDate")
    List<RSVP> findRSVPsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Count RSVPs by user
    long countByUserId(Long userId);

    // Delete RSVP by user and event
    void deleteByUserIdAndEventId(Long userId, Long eventId);

    // Check if event is at capacity
    @Query("SELECT CASE WHEN COUNT(r) >= e.maxCapacity THEN true ELSE false END " +
            "FROM RSVP r JOIN r.event e WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    boolean isEventAtCapacity(@Param("eventId") Long eventId);

    @Query("SELECT r FROM RSVP r JOIN FETCH r.event JOIN FETCH r.user WHERE r.user.id = :userId ORDER BY r.event.dateTime DESC")
    List<RSVP> findByUserIdWithEventDetails(@Param("userId") Long userId);

    @Query("SELECT r FROM RSVP r JOIN FETCH r.event WHERE r.user.id = :userId AND r.event.dateTime > :currentDateTime ORDER BY r.event.dateTime ASC")
    List<RSVP> findUserUpcomingRSVPsWithEventDetails(@Param("userId") Long userId, @Param("currentDateTime") LocalDateTime currentDateTime);

    @Query("SELECT r FROM RSVP r JOIN FETCH r.event WHERE r.user.id = :userId AND r.event.dateTime < :currentDateTime ORDER BY r.event.dateTime DESC")
    List<RSVP> findUserPastRSVPsWithEventDetails(@Param("userId") Long userId, @Param("currentDateTime") LocalDateTime currentDateTime);

    @Query("SELECT r FROM RSVP r JOIN FETCH r.user WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    List<RSVP> findConfirmedRSVPsByEventIdWithUser(@Param("eventId") Long eventId);

    @Query("SELECT r FROM RSVP r JOIN FETCH r.user WHERE r.event.id = :eventId AND r.status = 'PENDING'")
    List<RSVP> findPendingRSVPsByEventIdWithUser(@Param("eventId") Long eventId);

    @Query("SELECT r FROM RSVP r JOIN FETCH r.user WHERE r.event.id = :eventId")
    List<RSVP> findByEventIdWithUser(@Param("eventId") Long eventId);
}
