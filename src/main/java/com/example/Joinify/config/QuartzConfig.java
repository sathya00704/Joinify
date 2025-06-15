package com.example.Joinify.config;

import com.example.Joinify.job.EventReminderJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail eventReminderJobDetail() {
        System.out.println("Creating EventReminderJob JobDetail bean");
        return JobBuilder.newJob(EventReminderJob.class)
                .withIdentity("eventReminderJob")
                .withDescription("Send 24-hour event reminders")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger eventReminderTrigger() {
        System.out.println("Creating EventReminderJob Trigger bean");
        // Run every 30 minutes for testing (change back to 2 hours later)
        return TriggerBuilder.newTrigger()
                .forJob(eventReminderJobDetail())
                .withIdentity("eventReminderTrigger")
                .withDescription("Trigger for 24-hour event reminders")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 */30 * * * ?")) // Every 30 minutes for testing
                .build();
    }
}
