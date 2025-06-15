package com.example.Joinify.job;

import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.User;
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
        System.out.println("EventReminderJob started at: " + LocalDateTime.now());

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime tomorrow = now.plusDays(1);

            // Check for events in the next 20-28 hours (wider range for testing)
            LocalDateTime startRange = tomorrow.minusHours(4);
            LocalDateTime endRange = tomorrow.plusHours(4);

            System.out.println("Checking for events between: " + startRange + " and " + endRange);

            // Get all events in the time range
            List<Event> upcomingEvents = eventRepository.findEventsBetweenDatesWithOrganizer(startRange, endRange);

            System.out.println("Found " + upcomingEvents.size() + " events for 24-hour reminders");

            if (upcomingEvents.isEmpty()) {
                System.out.println("No events found in the specified time range");
                return;
            }

            for (Event event : upcomingEvents) {
                try {
                    System.out.println("Processing event: " + event.getTitle() + " at " + event.getDateTime());

                    // Get confirmed attendees
                    List<User> attendees = rsvpRepository.findConfirmedAttendeesByEventId(event.getId());
                    System.out.println("👥 Found " + attendees.size() + " confirmed attendees");

                    if (!attendees.isEmpty()) {
                        List<String> attendeeEmails = attendees.stream()
                                .map(User::getEmail)
                                .collect(Collectors.toList());

                        System.out.println("Sending reminders to: " + attendeeEmails);
                        emailService.sendEventReminderNotification(event, attendeeEmails);
                        System.out.println("Sent reminder for event: " + event.getTitle() + " to " + attendeeEmails.size() + " attendees");
                    } else {
                        System.out.println("No confirmed attendees for event: " + event.getTitle());
                    }
                } catch (Exception e) {
                    System.err.println("Failed to send reminder for event " + event.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("EventReminderJob completed at: " + LocalDateTime.now());

        } catch (Exception e) {
            System.err.println("Critical error in EventReminderJob: " + e.getMessage());
            e.printStackTrace();
            throw new JobExecutionException(e);
        }
    }
}
