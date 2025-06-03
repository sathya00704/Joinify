package com.example.Joinify.service;

import com.example.Joinify.dto.LoginRequest;
import com.example.Joinify.dto.LoginResponse;
import com.example.Joinify.dto.RegisterRequest;
import com.example.Joinify.dto.RegisterResponse;
import com.example.Joinify.entity.User;
import com.example.Joinify.repository.UserRepository;
import com.example.Joinify.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    // User Registration
    public RegisterResponse registerUser(RegisterRequest registerRequest) {
        try {
            // Check if username already exists
            if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
                return new RegisterResponse("Username already exists", null, null, false);
            }

            // Check if email already exists
            if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
                return new RegisterResponse("Email already exists", null, null, false);
            }

            // Create new user
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setRole(registerRequest.getRole());

            // Save user
            User savedUser = userRepository.save(user);

            return new RegisterResponse(
                    "User registered successfully",
                    savedUser.getUsername(),
                    savedUser.getEmail(),
                    true
            );

        } catch (Exception e) {
            return new RegisterResponse("Registration failed: " + e.getMessage(), null, null, false);
        }
    }

    // User Login
    public LoginResponse loginUser(LoginRequest loginRequest) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());

            // Get user from database
            Optional<User> userOptional = userRepository.findByUsername(loginRequest.getUsername());
            if (userOptional.isEmpty()) {
                return new LoginResponse(null, null, null, null, "User not found");
            }

            User user = userOptional.get();

            // Generate JWT token
            String token = jwtUtil.generateToken(userDetails);

            return new LoginResponse(
                    token,
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole(),
                    "Login successful"
            );

        } catch (BadCredentialsException e) {
            return new LoginResponse(null, null, null, null, "Invalid username or password");
        } catch (Exception e) {
            return new LoginResponse(null, null, null, null, "Login failed: " + e.getMessage());
        }
    }

    // Check if username exists
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    // Check if email exists
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
