package com.example.Joinify.controller;

import com.example.Joinify.job.EventReminderJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private EventReminderJob eventReminderJob;

    @GetMapping("/reminder-job")
    public ResponseEntity<String> testReminderJob() {
        try {
            System.out.println("🧪 Manually triggering EventReminderJob...");
            eventReminderJob.execute(null);
            return ResponseEntity.ok("Reminder job executed successfully! Check console for details.");
        } catch (Exception e) {
            System.err.println("Error executing reminder job: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
