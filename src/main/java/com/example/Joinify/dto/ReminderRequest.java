package com.example.Joinify.dto;

import jakarta.validation.constraints.Size;

public class ReminderRequest {

    @Size(max = 500, message = "Custom message cannot exceed 500 characters")
    private String message;

    // Constructors
    public ReminderRequest() {}

    public ReminderRequest(String message) {
        this.message = message;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
