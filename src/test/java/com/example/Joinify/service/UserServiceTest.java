package com.example.Joinify.service;

import com.example.Joinify.dto.RegisterRequest;
import com.example.Joinify.entity.User;
import com.example.Joinify.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    public void testRegisterUser() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser123");
        request.setEmail("testuser123@example.com");
        request.setPassword("password123");
        request.setRole(UserRole.ORGANIZER);

        // When
        User savedUser = userService.registerUser(request);

        // Then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser123");
        assertThat(savedUser.getEmail()).isEqualTo("testuser123@example.com");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.ORGANIZER);
    }

    @Test
    public void testRegisterUserWithDuplicateUsername() {
        // Given
        RegisterRequest request1 = new RegisterRequest();
        request1.setUsername("duplicate123");
        request1.setEmail("user1@example.com");
        request1.setPassword("password123");
        request1.setRole(UserRole.ORGANIZER);

        RegisterRequest request2 = new RegisterRequest();
        request2.setUsername("duplicate123");
        request2.setEmail("user2@example.com");
        request2.setPassword("password123");
        request2.setRole(UserRole.ATTENDEE);

        // When
        userService.registerUser(request1);

        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(request2);
        });
    }
}
