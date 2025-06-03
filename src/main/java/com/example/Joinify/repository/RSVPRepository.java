package com.example.Joinify.repository;

import com.example.Joinify.entity.RSVP;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RSVPRepository extends JpaRepository<RSVP, Long> {
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    long countByEventId(Long eventId);
}