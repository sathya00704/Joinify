package com.example.Joinify.dto;

// Only the organizer details that are safe to show publicly
public record OrganizerSummary(Long id, String username) {
}