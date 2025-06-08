package com.example.Joinify;

import com.example.Joinify.dto.LoginRequest;
import com.example.Joinify.dto.RegisterRequest;
import com.example.Joinify.entity.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String organizerToken;
    private String attendeeToken;
    private Long testEventId;

    @BeforeEach
    public void setup() throws Exception {
        // Register organizer
        RegisterRequest organizerRegister = new RegisterRequest("organizerTest", "organizer@test.com", "password123", UserRole.ORGANIZER);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(organizerRegister)))
                .andExpect(status().isCreated());

        // Register attendee
        RegisterRequest attendeeRegister = new RegisterRequest("attendeeTest", "attendee@test.com", "password123", UserRole.ATTENDEE);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendeeRegister)))
                .andExpect(status().isCreated());

        // Login organizer and get token
        LoginRequest organizerLogin = new LoginRequest("organizerTest", "password123");
        MvcResult organizerLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(organizerLogin)))
                .andExpect(status().isOk())
                .andReturn();
        String organizerResponse = organizerLoginResult.getResponse().getContentAsString();
        organizerToken = objectMapper.readTree(organizerResponse).get("token").asText();

        // Login attendee and get token
        LoginRequest attendeeLogin = new LoginRequest("attendeeTest", "password123");
        MvcResult attendeeLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendeeLogin)))
                .andExpect(status().isOk())
                .andReturn();
        String attendeeResponse = attendeeLoginResult.getResponse().getContentAsString();
        attendeeToken = objectMapper.readTree(attendeeResponse).get("token").asText();
    }

    @Test
    public void testAuthenticationFlow() throws Exception {
        // Test username availability
        mockMvc.perform(get("/api/auth/check-username/newuser"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        // Test email availability
        mockMvc.perform(get("/api/auth/check-email/new@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        // Test duplicate username registration
        RegisterRequest duplicateUser = new RegisterRequest("organizerTest", "new@test.com", "password123", UserRole.ATTENDEE);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isBadRequest());

        // Test invalid login
        LoginRequest invalidLogin = new LoginRequest("organizerTest", "wrongpassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testEventManagement() throws Exception {
        // Create event as organizer
        String eventJson = "{" +
                "\"title\": \"Integration Test Event\"," +
                "\"description\": \"Event created during integration test\"," +
                "\"dateTime\": \"2025-12-31T10:00:00\"," +
                "\"location\": \"Test Location\"," +
                "\"maxCapacity\": 50" +
                "}";

        MvcResult createResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration Test Event"))
                .andExpect(jsonPath("$.maxCapacity").value(50))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        testEventId = objectMapper.readTree(createResponse).get("id").asLong();

        // Get event by ID
        mockMvc.perform(get("/api/events/" + testEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Integration Test Event"));

        // Get all events
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Get upcoming events
        mockMvc.perform(get("/api/events/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Get my events as organizer
        mockMvc.perform(get("/api/events/my-events")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Search events by title
        mockMvc.perform(get("/api/events/search/title?keyword=Integration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Get event capacity info
        mockMvc.perform(get("/api/events/" + testEventId + "/capacity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxCapacity").value(50))
                .andExpect(jsonPath("$.confirmedAttendees").value(0));

        // Update event as organizer
        String updatedEventJson = "{" +
                "\"title\": \"Updated Integration Test Event\"," +
                "\"description\": \"Updated description\"," +
                "\"dateTime\": \"2025-12-31T14:00:00\"," +
                "\"location\": \"Updated Location\"," +
                "\"maxCapacity\": 75" +
                "}";

        mockMvc.perform(put("/api/events/" + testEventId)
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedEventJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Integration Test Event"));
    }

    @Test
    public void testEventSecurityAndPermissions() throws Exception {
        // Try to create event without authentication
        String eventJson = "{" +
                "\"title\": \"Unauthorized Event\"," +
                "\"description\": \"This should fail\"," +
                "\"dateTime\": \"2025-12-31T10:00:00\"," +
                "\"location\": \"Test Location\"," +
                "\"maxCapacity\": 50" +
                "}";

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isUnauthorized());

        // Try to create event as attendee (should fail)
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testRSVPManagement() throws Exception {
        // First create an event
        String eventJson = "{" +
                "\"title\": \"RSVP Test Event\"," +
                "\"description\": \"Event for RSVP testing\"," +
                "\"dateTime\": \"2025-12-31T10:00:00\"," +
                "\"location\": \"RSVP Location\"," +
                "\"maxCapacity\": 10" +
                "}";

        MvcResult createResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isCreated())
                .andReturn();

        Long eventId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // RSVP to event as attendee
        mockMvc.perform(post("/api/rsvp/event/" + eventId)
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Check RSVP status - Fixed: Expect enum value with quotes
        mockMvc.perform(get("/api/rsvp/event/" + eventId + "/status")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(content().string("\"CONFIRMED\""));

        // Check if user has RSVP'd
        mockMvc.perform(get("/api/rsvp/event/" + eventId + "/check")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // Get my RSVPs
        mockMvc.perform(get("/api/rsvp/my-rsvps")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Get upcoming RSVPs
        mockMvc.perform(get("/api/rsvp/my-rsvps/upcoming")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Get RSVP counts for event
        mockMvc.perform(get("/api/rsvp/event/" + eventId + "/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(1))
                .andExpect(jsonPath("$.total").value(1));

        // Get attendees for event (as organizer)
        mockMvc.perform(get("/api/rsvp/event/" + eventId + "/attendees")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Cancel RSVP
        mockMvc.perform(delete("/api/rsvp/event/" + eventId)
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isNoContent());

        // Verify RSVP is cancelled
        mockMvc.perform(get("/api/rsvp/event/" + eventId + "/check")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    public void testRSVPCapacityLimits() throws Exception {
        // Create event with capacity of 1
        String eventJson = "{" +
                "\"title\": \"Capacity Test Event\"," +
                "\"description\": \"Event for capacity testing\"," +
                "\"dateTime\": \"2025-12-31T10:00:00\"," +
                "\"location\": \"Capacity Location\"," +
                "\"maxCapacity\": 1" +
                "}";

        MvcResult createResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isCreated())
                .andReturn();

        Long eventId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // First RSVP should succeed
        mockMvc.perform(post("/api/rsvp/event/" + eventId)
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isCreated());

        // Register another attendee
        RegisterRequest attendee2 = new RegisterRequest("attendee2Test", "attendee2@test.com", "password123", UserRole.ATTENDEE);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendee2)))
                .andExpect(status().isCreated());

        // Login second attendee
        LoginRequest attendee2Login = new LoginRequest("attendee2Test", "password123");
        MvcResult attendee2LoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendee2Login)))
                .andExpect(status().isOk())
                .andReturn();
        String attendee2Token = objectMapper.readTree(attendee2LoginResult.getResponse().getContentAsString()).get("token").asText();

        // Second RSVP should fail due to capacity
        mockMvc.perform(post("/api/rsvp/event/" + eventId)
                        .header("Authorization", "Bearer " + attendee2Token))
                .andExpect(status().isConflict());
    }

    @Test
    public void testUnauthorizedAccess() throws Exception {
        // Try to access protected endpoints without authentication
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/events/my-events"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/rsvp/event/1"))
                .andExpect(status().isUnauthorized());

        // Try to access organizer-only endpoints as attendee - expect 400 due to validation
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDataValidation() throws Exception {
        // Try to create event with invalid data
        String invalidEventJson = "{" +
                "\"title\": \"\"," +
                "\"description\": \"Invalid event\"," +
                "\"dateTime\": \"invalid-date\"," +
                "\"location\": \"Test Location\"," +
                "\"maxCapacity\": -1" +
                "}";

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEventJson))
                .andExpect(status().isBadRequest());

        // Try to register with invalid data
        RegisterRequest invalidRegister = new RegisterRequest("", "invalid-email", "123", null);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRegister)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCompleteWorkflow() throws Exception {
        // Complete workflow: Register -> Login -> Create Event -> RSVP -> Cancel

        // 1. Register new users
        RegisterRequest organizer = new RegisterRequest("workflowOrg", "workflow.org@test.com", "password123", UserRole.ORGANIZER);
        RegisterRequest attendee = new RegisterRequest("workflowAtt", "workflow.att@test.com", "password123", UserRole.ATTENDEE);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(organizer)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendee)))
                .andExpect(status().isCreated());

        // 2. Login both users
        LoginRequest orgLogin = new LoginRequest("workflowOrg", "password123");
        LoginRequest attLogin = new LoginRequest("workflowAtt", "password123");

        MvcResult orgResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orgLogin)))
                .andExpect(status().isOk())
                .andReturn();
        String orgToken = objectMapper.readTree(orgResult.getResponse().getContentAsString()).get("token").asText();

        MvcResult attResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attLogin)))
                .andExpect(status().isOk())
                .andReturn();
        String attToken = objectMapper.readTree(attResult.getResponse().getContentAsString()).get("token").asText();

        // 3. Create event
        String eventJson = "{" +
                "\"title\": \"Workflow Test Event\"," +
                "\"description\": \"Complete workflow test\"," +
                "\"dateTime\": \"2025-12-31T10:00:00\"," +
                "\"location\": \"Workflow Location\"," +
                "\"maxCapacity\": 100" +
                "}";

        MvcResult eventResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isCreated())
                .andReturn();
        Long eventId = objectMapper.readTree(eventResult.getResponse().getContentAsString()).get("id").asLong();

        // 4. RSVP to event
        mockMvc.perform(post("/api/rsvp/event/" + eventId)
                        .header("Authorization", "Bearer " + attToken))
                .andExpect(status().isCreated());

        // 5. Verify RSVP - Fixed: Expect enum value with quotes
        mockMvc.perform(get("/api/rsvp/event/" + eventId + "/status")
                        .header("Authorization", "Bearer " + attToken))
                .andExpect(status().isOk())
                .andExpect(content().string("\"CONFIRMED\""));

        // 6. Check event capacity
        mockMvc.perform(get("/api/events/" + eventId + "/capacity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedAttendees").value(1));

        // 7. Cancel RSVP
        mockMvc.perform(delete("/api/rsvp/event/" + eventId)
                        .header("Authorization", "Bearer " + attToken))
                .andExpect(status().isNoContent());

        // 8. Verify cancellation
        mockMvc.perform(get("/api/events/" + eventId + "/capacity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedAttendees").value(0));
    }
}
