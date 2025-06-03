package com.example.Joinify.repository;

import com.example.Joinify.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByDateTimeAfter(LocalDateTime date);
    List<Event> findByDateTimeBefore(LocalDateTime date);
    List<Event> findByOrganizerId(Long organizerId);
}