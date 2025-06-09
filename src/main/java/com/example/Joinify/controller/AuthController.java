package com.example.Joinify.controller;

import com.example.Joinify.dto.LoginRequest;
import com.example.Joinify.dto.LoginResponse;
import com.example.Joinify.dto.RegisterRequest;
import com.example.Joinify.dto.RegisterResponse;
import com.example.Joinify.exception.BadRequestException;
import com.example.Joinify.exception.DuplicateResourceException;
import com.example.Joinify.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    // User Registration Endpoint
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse response = authService.registerUser(registerRequest);

        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            // Convert failure response to appropriate exception
            if (response.getMessage().contains("already exists")) {
                throw new DuplicateResourceException(response.getMessage());
            } else {
                throw new BadRequestException(response.getMessage());
            }
        }
    }



    // User Login Endpoint
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.loginUser(loginRequest);

            if (response.getToken() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            LoginResponse errorResponse = new LoginResponse(
                    null,
                    null,
                    null,
                    null,
                    "Login failed: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Check Username Availability
    @GetMapping("/check-username/{username}")
    public ResponseEntity<Boolean> checkUsername(@PathVariable String username) {
        boolean exists = authService.existsByUsername(username);
        return ResponseEntity.ok(exists);
    }

    // Check Email Availability
    @GetMapping("/check-email/{email}")
    public ResponseEntity<Boolean> checkEmail(@PathVariable String email) {
        boolean exists = authService.existsByEmail(email);
        return ResponseEntity.ok(exists);
    }

}
