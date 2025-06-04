package com.example.Joinify.repository;

import com.example.Joinify.entity.User;
import com.example.Joinify.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional  // This will rollback changes after each test
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testFindByUsername() {
        // Given
        User user = new User();
        user.setUsername("john123test");
        user.setEmail("john123test@gmail.com");
        user.setPassword("john123");
        user.setRole(UserRole.ORGANIZER);

        // Save user to database
        User savedUser = userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByUsername("john123test");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john123test@gmail.com");
        assertThat(found.get().getRole()).isEqualTo(UserRole.ORGANIZER);
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    public void testExistsByUsername() {
        // Given
        User user = new User();
        user.setUsername("jack123test");
        user.setEmail("jack123test@gmail.com");
        user.setPassword("jack123");
        user.setRole(UserRole.ATTENDEE);

        // Save user to database
        userRepository.save(user);

        // When & Then
        assertThat(userRepository.existsByUsername("jack123test")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    public void testFindByEmail() {
        // Given
        User user = new User();
        user.setUsername("emailtest");
        user.setEmail("emailtest@gmail.com");
        user.setPassword("password123");
        user.setRole(UserRole.ORGANIZER);

        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByEmail("emailtest@gmail.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("emailtest");
    }

    @Test
    public void testCountByRole() {
        // Given - Create test users
        User organizer1 = new User();
        organizer1.setUsername("org1test");
        organizer1.setEmail("org1test@gmail.com");
        organizer1.setPassword("password");
        organizer1.setRole(UserRole.ORGANIZER);

        User organizer2 = new User();
        organizer2.setUsername("org2test");
        organizer2.setEmail("org2test@gmail.com");
        organizer2.setPassword("password");
        organizer2.setRole(UserRole.ORGANIZER);

        User attendee = new User();
        attendee.setUsername("att1test");
        attendee.setEmail("att1test@gmail.com");
        attendee.setPassword("password");
        attendee.setRole(UserRole.ATTENDEE);

        userRepository.save(organizer1);
        userRepository.save(organizer2);
        userRepository.save(attendee);

        // When & Then
        long organizerCount = userRepository.countByRole(UserRole.ORGANIZER);
        long attendeeCount = userRepository.countByRole(UserRole.ATTENDEE);

        assertThat(organizerCount).isGreaterThanOrEqualTo(2);
        assertThat(attendeeCount).isGreaterThanOrEqualTo(1);
    }
}
