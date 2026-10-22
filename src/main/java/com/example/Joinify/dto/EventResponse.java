package com.example.Joinify.dto;

import com.example.Joinify.entity.RegistrationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        LocalDateTime dateTime,
        String location,
        int maxCapacity,
        String imageUrl,
        BigDecimal fee,
        RegistrationStatus registrationStatus,
        OrganizerSummary organizer
) {
}