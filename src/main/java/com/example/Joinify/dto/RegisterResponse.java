package com.example.Joinify.dto;

public class RegisterResponse {

    private String message;
    private String username;
    private String email;
    private boolean success;

    // Constructors
    public RegisterResponse() {}

    public RegisterResponse(String message, String username, String email, boolean success) {
        this.message = message;
        this.username = username;
        this.email = email;
        this.success = success;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
