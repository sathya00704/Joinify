package com.example.Joinify.notification;

import java.util.List;

// automatic = true  -> sent by the 24-hour scheduler
// automatic = false -> sent manually by the organizer
public record EventReminderNotification(EventEmailDetails event,
                                        List<String> recipients,
                                        String customMessage,
                                        boolean automatic) {
}