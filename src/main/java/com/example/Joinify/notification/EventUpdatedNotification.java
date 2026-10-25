package com.example.Joinify.notification;

import java.util.List;

public record EventUpdatedNotification(EventEmailDetails event, List<String> recipients) {
}