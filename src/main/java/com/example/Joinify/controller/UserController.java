package com.example.Joinify.controller;

import com.example.Joinify.entity.User;
import com.example.Joinify.entity.UserRole;
import com.example.Joinify.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // Response class for user statistics
    public static class UserStatisticsResponse {
        public final long total;
        public final long organizers;
        public final long attendees;

        public UserStatisticsResponse(long total, long organizers, long attendees) {
            this.total = total;
            this.organizers = organizers;
            this.attendees = attendees;
        }
    }

    // Get current user profile
    @GetMapping("/profile")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<User> getCurrentUserProfile(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> user = userService.getUserByUsername(username);

        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Update current user profile
    @PutMapping("/profile")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<User> updateCurrentUserProfile(@Valid @RequestBody User updatedUser,
                                                         Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> currentUserOpt = userService.getUserByUsername(username);

            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User currentUser = currentUserOpt.get();

            // Update only allowed fields - DON'T update password here
            currentUser.setEmail(updatedUser.getEmail());
            // Remove password update from here - it should be done via separate endpoint

            User savedUser = userService.updateUser(currentUser);

            // Remove password from response
            savedUser.setPassword(null);
            return ResponseEntity.ok(savedUser);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // Change password
    @PutMapping("/change-password")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<String> changePassword(@RequestParam String newPassword,
                                                 Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userService.getUserByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            userService.updateUserPassword(userOpt.get().getId(), newPassword);
            return ResponseEntity.ok("Password updated successfully");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update password");
        }
    }

    // Get user by ID (public access for basic info)
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            // Remove sensitive information
            User publicUser = user.get();
            publicUser.setPassword(null);
            return ResponseEntity.ok(publicUser);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Get all users (admin functionality - can be restricted)
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        // Remove passwords from response
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
    }

    // Get all organizers
    @GetMapping("/organizers")
    public ResponseEntity<List<User>> getAllOrganizers() {
        List<User> organizers = userService.getAllOrganizers();
        organizers.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(organizers);
    }

    // Get all attendees
    @GetMapping("/attendees")
    public ResponseEntity<List<User>> getAllAttendees() {
        List<User> attendees = userService.getAllAttendees();
        attendees.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(attendees);
    }

    // Get users by role
    @GetMapping("/role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable UserRole role) {
        List<User> users = userService.getUsersByRole(role);
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
    }

    // Get user statistics - FIXED
    @GetMapping("/stats")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics() {
        long totalUsers = userService.getTotalUserCount();
        long organizerCount = userService.countUsersByRole(UserRole.ORGANIZER);
        long attendeeCount = userService.countUsersByRole(UserRole.ATTENDEE);

        UserStatisticsResponse response = new UserStatisticsResponse(totalUsers, organizerCount, attendeeCount);
        return ResponseEntity.ok(response);
    }

    // Delete current user account
    @DeleteMapping("/profile")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<String> deleteCurrentUser(Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userService.getUserByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            userService.deleteUser(userOpt.get().getId());
            return ResponseEntity.ok("Account deleted successfully");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete account");
        }
    }

    // Admin: Delete user by ID (can be restricted to admin role)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')") // You might want to create an ADMIN role
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
