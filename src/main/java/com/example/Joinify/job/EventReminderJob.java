package com.example.Joinify.job;

import com.example.Joinify.entity.Event;
import com.example.Joinify.repository.EventRepository;
import com.example.Joinify.repository.RSVPRepository;
import com.example.Joinify.service.EmailService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventReminderJob implements Job {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RSVPRepository rsvpRepository;

    @Autowired
    private EmailService emailService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime tomorrow = now.plusDays(1);

            // Find events happening in the next 24 hours (with 1-hour buffer)
            LocalDateTime startRange = tomorrow.minusHours(1);
            LocalDateTime endRange = tomorrow.plusHours(1);

            List<Event> upcomingEvents = eventRepository.findEventsBetweenDatesWithOrganizer(startRange, endRange);

            System.out.println("Found " + upcomingEvents.size() + " events for 24-hour reminders");

            for (Event event : upcomingEvents) {
                try {
                    // Get confirmed attendees' emails
                    List<String> attendeeEmails = rsvpRepository.findConfirmedAttendeesByEventId(event.getId())
                            .stream()
                            .map(user -> user.getEmail())
                            .collect(Collectors.toList());

                    if (!attendeeEmails.isEmpty()) {
                        emailService.sendEventReminderNotification(event, attendeeEmails);
                        System.out.println("Sent reminder for event: " + event.getTitle() + " to " + attendeeEmails.size() + " attendees");
                    }
                } catch (Exception e) {
                    System.err.println("Failed to send reminder for event " + event.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in EventReminderJob: " + e.getMessage());
            throw new JobExecutionException(e);
        }
    }
}
