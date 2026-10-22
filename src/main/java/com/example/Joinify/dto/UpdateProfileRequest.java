package com.example.Joinify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email")
        String email
) {
}