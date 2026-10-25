package com.example.Joinify.notification;

import com.example.Joinify.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmailNotificationListener {

    @Autowired
    private EmailService emailService;

    // Runs only if the publishing transaction COMMITS, and on a background thread
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventUpdated(EventUpdatedNotification notification) {
        emailService.sendEventUpdateNotification(notification.event(), notification.recipients());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventReminder(EventReminderNotification notification) {
        emailService.sendEventReminder(notification.event(), notification.recipients(),
                notification.customMessage(), notification.automatic());
    }
}