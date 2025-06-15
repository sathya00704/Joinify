package com.example.Joinify.service;

import com.example.Joinify.dto.RegisterRequest;
import com.example.Joinify.entity.User;
import com.example.Joinify.entity.UserRole;
import com.example.Joinify.exception.BadRequestException;
import com.example.Joinify.exception.DuplicateResourceException;
import com.example.Joinify.exception.ResourceNotFoundException;
import com.example.Joinify.exception.UnauthorizedException;
import com.example.Joinify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Register a new user
    public User registerUser(User user) {
        validateUserRegistration(user);

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // Get user by ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    // Get user by username
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Get user by email
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get users by role
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    // Get all organizers
    public List<User> getAllOrganizers() {
        return userRepository.findAllOrganizers();
    }

    // Get all attendees
    public List<User> getAllAttendees() {
        return userRepository.findAllAttendees();
    }

    // Update user details
    public User updateUser(Long userId, User updatedUser, String currentUsername) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Authorization check
        if (!existingUser.getUsername().equals(currentUsername)) {
            throw new UnauthorizedException("You can only update your own profile");
        }

        validateUserUpdate(updatedUser, existingUser);

        // Update allowed fields
        existingUser.setEmail(updatedUser.getEmail());
        // Don't update username or password through this method

        return userRepository.save(existingUser);
    }

    // Update user password
    public User updateUserPassword(Long userId, String newPassword) {
        User user = getUserById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    // Delete user by ID
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        userRepository.deleteById(id);
    }

    // Check if username exists
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // Check if email exists
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Count users by role
    public long countUsersByRole(UserRole role) {
        return userRepository.countByRole(role);
    }

    // Get total user count
    public long getTotalUserCount() {
        return userRepository.count();
    }

    private void validateUserRegistration(User user) {
        validateUsername(user.getUsername());
        validateEmail(user.getEmail());
        validatePassword(user.getPassword());

        // Check for duplicates
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }
    }

    private void validateUserUpdate(User updatedUser, User existingUser) {
        if (updatedUser.getEmail() != null) {
            validateEmail(updatedUser.getEmail());

            // Check if email is being changed and if new email already exists
            if (!existingUser.getEmail().equals(updatedUser.getEmail()) &&
                    userRepository.existsByEmail(updatedUser.getEmail())) {
                throw new DuplicateResourceException("Email already registered");
            }
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BadRequestException("Username is required");
        }
        if (username.length() < 3) {
            throw new BadRequestException("Username must be at least 3 characters long");
        }
        if (username.length() > 50) {
            throw new BadRequestException("Username cannot exceed 50 characters");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new BadRequestException("Username can only contain letters, numbers, and underscores");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BadRequestException("Please enter a valid email address");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new BadRequestException("Password is required");
        }
        if (password.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters long");
        }
        if (password.length() > 100) {
            throw new BadRequestException("Password cannot exceed 100 characters");
        }
    }
}
