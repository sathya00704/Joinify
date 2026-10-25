package com.example.Joinify.notification;

import java.util.List;

public record EventReminderNotification(EventEmailDetails event, List<String> recipients, String customMessage) {
}