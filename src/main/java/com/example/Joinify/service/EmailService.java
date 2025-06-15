//package com.example.Joinify.service;
//
//import com.example.Joinify.entity.Event;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.stereotype.Service;
//import org.thymeleaf.TemplateEngine;
//import org.thymeleaf.context.Context;
//
//import jakarta.mail.MessagingException;
//import jakarta.mail.internet.MimeMessage;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.math.BigDecimal;
//
//@Service
//public class EmailService {
//
//    @Autowired
//    private JavaMailSender mailSender;
//
//    @Autowired
//    private TemplateEngine templateEngine;
//
//    @Value("${spring.mail.username}")
//    private String fromEmail;
//
//    public void sendEventUpdateNotification(Event event, List<String> attendeeEmails) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//            helper.setFrom(fromEmail);
//            helper.setTo(attendeeEmails.toArray(new String[0]));
//            helper.setSubject("Event Updated: " + event.getTitle());
//
//            // Create email content using Thymeleaf template
//            Context context = new Context();
//            context.setVariable("eventTitle", event.getTitle());
//            context.setVariable("eventDescription", event.getDescription());
//            context.setVariable("eventDateTime", formatDateTime(event.getDateTime()));
//            context.setVariable("eventLocation", event.getLocation());
//            context.setVariable("organizerName", event.getOrganizer().getUsername());
//            context.setVariable("eventFee", event.getFee() != null && event.getFee().compareTo(BigDecimal.ZERO) > 0
//                    ? "Rs. " + event.getFee().toString() : "Free");
//
//            String htmlContent = templateEngine.process("event-update-notification", context);
//            helper.setText(htmlContent, true);
//
//            mailSender.send(message);
//            System.out.println("Event update notification sent to " + attendeeEmails.size() + " attendees");
//
//        } catch (MessagingException e) {
//            System.err.println("Failed to send event update notification: " + e.getMessage());
//            throw new RuntimeException("Failed to send email notification", e);
//        }
//    }
//
//    public void sendEventReminderNotification(Event event, List<String> attendeeEmails) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//            helper.setFrom(fromEmail);
//            helper.setTo(attendeeEmails.toArray(new String[0]));
//            helper.setSubject("Reminder: " + event.getTitle() + " - Tomorrow!");
//
//            Context context = new Context();
//            context.setVariable("eventTitle", event.getTitle());
//            context.setVariable("eventDescription", event.getDescription());
//            context.setVariable("eventDateTime", formatDateTime(event.getDateTime()));
//            context.setVariable("eventLocation", event.getLocation());
//            context.setVariable("organizerName", event.getOrganizer().getUsername());
//            context.setVariable("eventFee", event.getFee() != null && event.getFee().compareTo(BigDecimal.ZERO) > 0
//                    ? "Rs. " + event.getFee().toString() : "Free");
//
//            String htmlContent = templateEngine.process("event-reminder-notification", context);
//            helper.setText(htmlContent, true);
//
//            mailSender.send(message);
//            System.out.println("Event reminder sent to " + attendeeEmails.size() + " attendees");
//
//        } catch (MessagingException e) {
//            System.err.println("Failed to send event reminder: " + e.getMessage());
//            throw new RuntimeException("Failed to send reminder email", e);
//        }
//    }
//
//    private String formatDateTime(LocalDateTime dateTime) {
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a");
//        return dateTime.format(formatter);
//    }
//}
package com.example.Joinify.service;

import com.example.Joinify.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.math.BigDecimal;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEventUpdateNotification(Event event, List<String> attendeeEmails) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(attendeeEmails.toArray(new String[0]));
            helper.setSubject("Event Updated: " + event.getTitle());

            Context context = new Context();
            context.setVariable("eventTitle", event.getTitle());
            context.setVariable("eventDescription", event.getDescription());
            context.setVariable("eventDateTime", formatDateTime(event.getDateTime()));
            context.setVariable("eventLocation", event.getLocation());
            context.setVariable("organizerName", event.getOrganizer().getUsername());
            context.setVariable("eventFee", event.getFee() != null && event.getFee().compareTo(BigDecimal.ZERO) > 0
                    ? "Rs. " + event.getFee().toString() : "Free");

            String htmlContent = templateEngine.process("event-update-notification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("Event update notification sent to " + attendeeEmails.size() + " attendees");

        } catch (MessagingException e) {
            System.err.println("Failed to send event update notification: " + e.getMessage());
            throw new RuntimeException("Failed to send email notification", e);
        }
    }

    // NEW: Manual reminder method for organizers
    public void sendManualEventReminder(Event event, List<String> attendeeEmails, String customMessage) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(attendeeEmails.toArray(new String[0]));
            helper.setSubject("Event Reminder: " + event.getTitle());

            Context context = new Context();
            context.setVariable("eventTitle", event.getTitle());
            context.setVariable("eventDescription", event.getDescription());
            context.setVariable("eventDateTime", formatDateTime(event.getDateTime()));
            context.setVariable("eventLocation", event.getLocation());
            context.setVariable("organizerName", event.getOrganizer().getUsername());
            context.setVariable("customMessage", customMessage != null && !customMessage.trim().isEmpty()
                    ? customMessage : "Don't forget about this upcoming event!");
            context.setVariable("eventFee", event.getFee() != null && event.getFee().compareTo(BigDecimal.ZERO) > 0
                    ? "Rs. " + event.getFee().toString() : "Free");

            String htmlContent = templateEngine.process("manual-event-reminder", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("Manual event reminder sent to " + attendeeEmails.size() + " attendees");

        } catch (MessagingException e) {
            System.err.println("Failed to send manual event reminder: " + e.getMessage());
            throw new RuntimeException("Failed to send reminder email", e);
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a");
        return dateTime.format(formatter);
    }
}
