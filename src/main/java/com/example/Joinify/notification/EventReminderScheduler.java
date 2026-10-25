package com.example.Joinify.notification;

import com.example.Joinify.entity.Event;
import com.example.Joinify.entity.User;
import com.example.Joinify.repository.EventRepository;
import com.example.Joinify.repository.RSVPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EventReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventReminderScheduler.class);
    private static final String AUTO_MESSAGE = "Your event starts within the next 24 hours. See you there!";

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RSVPRepository rsvpRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // Runs every 15 minutes by default (see app.reminders.cron)
    @Scheduled(cron = "${app.reminders.cron}")
    @Transactional
    public void sendUpcomingEventReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> events = eventRepository.findEventsNeedingReminder(now, now.plusHours(24));

        for (Event event : events) {
            List<String> recipients = rsvpRepository.findConfirmedAttendeesByEventId(event.getId())
                    .stream()
                    .map(User::getEmail)
                    .toList();

            // Mark first: even with no attendees, don't check this event again every 15 minutes.
            // Saved automatically when the transaction commits (dirty checking).
            event.setReminderSent(true);

            if (!recipients.isEmpty()) {
                eventPublisher.publishEvent(new EventReminderNotification(
                        EventEmailDetails.from(event), recipients, AUTO_MESSAGE, true));
            }
        }

        if (!events.isEmpty()) {
            log.info("Automatic reminders queued for {} event(s)", events.size());
        }
    }
}