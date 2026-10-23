package com.example.Joinify.controller;

import com.example.Joinify.dto.UpdateProfileRequest;
import com.example.Joinify.dto.UserResponse;
import com.example.Joinify.entity.User;
import com.example.Joinify.entity.UserRole;
import com.example.Joinify.exception.UnauthorizedException;
import com.example.Joinify.mapper.DtoMapper;
import com.example.Joinify.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    // ===================== Current user's profile =====================

    @GetMapping("/profile")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<UserResponse> getCurrentUserProfile(Authentication authentication) {
        return ResponseEntity.ok(DtoMapper.toUserResponse(currentUser(authentication)));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<UserResponse> updateCurrentUserProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                                 Authentication authentication) {
        User currentUser = currentUser(authentication);

        // Only the email can be changed here
        User changes = new User();
        changes.setEmail(request.email().trim());

        User savedUser = userService.updateUser(currentUser.getId(), changes, authentication.getName());
        return ResponseEntity.ok(DtoMapper.toUserResponse(savedUser));
    }

    // Rewritten in Phase 2 (current password check + strength rule)
    @PutMapping("/change-password")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<String> changePassword(@RequestParam String newPassword,
                                                 Authentication authentication) {
        userService.updateUserPassword(currentUser(authentication).getId(), newPassword);
        return ResponseEntity.ok("Password updated successfully");
    }

    @DeleteMapping("/profile")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ORGANIZER')")
    public ResponseEntity<String> deleteCurrentUser(Authentication authentication) {
        userService.deleteUser(currentUser(authentication).getId());
        return ResponseEntity.ok("Account deleted successfully");
    }

    // ===================== Public =====================

    // Used by the home page. Counts only, no personal data.
    @GetMapping("/stats")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics() {
        long totalUsers = userService.getTotalUserCount();
        long organizerCount = userService.countUsersByRole(UserRole.ORGANIZER);
        long attendeeCount = userService.countUsersByRole(UserRole.ATTENDEE);

        return ResponseEntity.ok(new UserStatisticsResponse(totalUsers, organizerCount, attendeeCount));
    }

    // ===================== Helper =====================

    private User currentUser(Authentication authentication) {
        return userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}