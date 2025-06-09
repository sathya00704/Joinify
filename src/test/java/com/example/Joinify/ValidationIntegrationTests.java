package com.example.Joinify;

import com.example.Joinify.dto.RegisterRequest;
import com.example.Joinify.entity.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ValidationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testInvalidUserRegistration() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setUsername(""); // Invalid
        invalidRequest.setEmail("invalid-email"); // Invalid
        invalidRequest.setPassword("123"); // Too short
        invalidRequest.setRole(null); // Invalid

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    public void testResourceNotFound() throws Exception {
        mockMvc.perform(get("/api/events/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    public void testDuplicateUserRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("duplicatetest");
        request.setEmail("duplicate@test.com");
        request.setPassword("Password123!");
        request.setRole(UserRole.ORGANIZER);

        // First registration should succeed
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration should fail
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    public void testInvalidEventCreation() throws Exception {
        String invalidEventJson = "{" +
                "\"title\": \"\"," +
                "\"description\": \"Invalid event\"," +
                "\"dateTime\": \"2023-01-01T10:00:00\"," +
                "\"location\": \"\"," +
                "\"maxCapacity\": -5" +
                "}";

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEventJson))
                .andExpect(status().isBadRequest());
    }
}
