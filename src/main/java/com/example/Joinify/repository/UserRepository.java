package com.example.Joinify.repository;

import com.example.Joinify.entity.User;
import com.example.Joinify.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by username
    Optional<User> findByUsername(String username);

    // Find user by email
    Optional<User> findByEmail(String email);

    // Check if username exists
    boolean existsByUsername(String username);

    // Check if email exists
    boolean existsByEmail(String email);

    // Find users by role
    List<User> findByRole(UserRole role);

    // Find all organizers
    @Query("SELECT u FROM User u WHERE u.role = 'ORGANIZER'")
    List<User> findAllOrganizers();

    // Find all attendees
    @Query("SELECT u FROM User u WHERE u.role = 'ATTENDEE'")
    List<User> findAllAttendees();

    // Find user by username or email (for login flexibility)
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    // Count users by role
    long countByRole(UserRole role);
}
