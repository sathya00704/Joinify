package com.example.Joinify.dto;

import com.example.Joinify.entity.UserRole;

public class LoginResponse {

    private String token;
    private String username;
    private String email;
    private UserRole role;
    private String message;

    // Constructors
    public LoginResponse() {}

    public LoginResponse(String token, String username, String email, UserRole role, String message) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.role = role;
        this.message = message;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
