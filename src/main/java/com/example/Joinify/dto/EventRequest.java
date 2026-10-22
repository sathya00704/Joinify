package com.example.Joinify.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Only the fields an organizer is allowed to send. No id, organizer or registrationStatus.
public record EventRequest(

        @NotBlank(message = "Event title is required")
        @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
        String title,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @NotNull(message = "Event date and time is required")
        @Future(message = "Event date must be in the future")
        LocalDateTime dateTime,

        @NotBlank(message = "Location is required")
        @Size(min = 3, max = 100, message = "Location must be between 3 and 100 characters")
        String location,

        @NotNull(message = "Maximum capacity is required")
        @Min(value = 1, message = "Maximum capacity must be at least 1")
        @Max(value = 10000, message = "Maximum capacity cannot exceed 10000")
        Integer maxCapacity,

        @NotBlank(message = "Image URL is required")
        @Size(max = 255, message = "Image URL cannot exceed 255 characters")
        String imageUrl,

        @DecimalMin(value = "0.00", message = "Event fee cannot be negative")
        @DecimalMax(value = "100000.00", message = "Event fee cannot exceed Rs. 100000")
        @Digits(integer = 6, fraction = 2, message = "Event fee can have at most 2 decimal places")
        BigDecimal fee
) {
}