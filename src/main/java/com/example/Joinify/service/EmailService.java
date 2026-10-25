package com.example.Joinify.service;

import com.example.Joinify.notification.EventEmailDetails;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a");

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private Context baseContext(EventEmailDetails event) {
        Context context = new Context();
        context.setVariable("eventTitle", event.title());
        context.setVariable("eventDescription", event.description());
        context.setVariable("eventDateTime", event.dateTime().format(DATE_FORMAT));
        context.setVariable("eventLocation", event.location());
        context.setVariable("organizerName", event.organizerName());
        context.setVariable("eventFee", event.fee() != null && event.fee().compareTo(BigDecimal.ZERO) > 0
                ? "Rs. " + event.fee() : "Free");
        return context;
    }

    public void sendEventUpdateNotification(EventEmailDetails event, List<String> recipients) {
        String html = templateEngine.process("event-update-notification", baseContext(event));
        List<String> failed = sendToEach(recipients, "Event Updated: " + event.title(), html);
        notifyOrganizerOfFailures(event, "event update", failed, recipients.size());
    }

    public void sendEventReminder(EventEmailDetails event, List<String> recipients, String customMessage) {
        Context context = baseContext(event);
        context.setVariable("customMessage", customMessage != null && !customMessage.isBlank()
                ? customMessage : "Don't forget about this upcoming event!");

        String html = templateEngine.process("manual-event-reminder", context);
        List<String> failed = sendToEach(recipients, "Event Reminder: " + event.title(), html);
        notifyOrganizerOfFailures(event, "event reminder", failed, recipients.size());
    }

    // One email per recipient: nobody sees anyone else's address,
    // and one bad address doesn't stop the rest from being sent.
    // Returns the addresses that failed.
    private List<String> sendToEach(List<String> recipients, String subject, String html) {
        List<String> failed = new ArrayList<>();
        for (String to : recipients) {
            if (!sendOne(to, subject, html)) {
                failed.add(to);
            }
        }
        log.info("Sent '{}' to {}/{} recipients", subject, recipients.size() - failed.size(), recipients.size());
        return failed;
    }

    // Tells the organizer which attendees didn't get the email.
    // Sent once; if this also fails it is only logged (no retry loop).
    private void notifyOrganizerOfFailures(EventEmailDetails event, String emailType,
                                           List<String> failed, int totalCount) {
        if (failed.isEmpty()) {
            return;
        }

        Context context = new Context();
        context.setVariable("organizerName", event.organizerName());
        context.setVariable("eventTitle", event.title());
        context.setVariable("emailType", emailType);
        context.setVariable("failedCount", failed.size());
        context.setVariable("totalCount", totalCount);
        context.setVariable("failedRecipients", failed);

        String html = templateEngine.process("email-failure-notice", context);
        boolean sent = sendOne(event.organizerEmail(), "Email delivery issue: " + event.title(), html);

        if (!sent) {
            log.error("Could not notify organizer {} about {} failed '{}' emails for event '{}'",
                    event.organizerEmail(), failed.size(), emailType, event.title());
        }
    }

    private boolean sendOne(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            return true;
        } catch (MessagingException | MailException e) {
            log.warn("Failed to send '{}' to {}: {}", subject, to, e.getMessage());
            return false;
        }
    }
}