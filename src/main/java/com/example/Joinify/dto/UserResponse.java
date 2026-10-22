package com.example.Joinify.dto;

import com.example.Joinify.entity.UserRole;

public record UserResponse(Long id, String username, String email, UserRole role) {
}