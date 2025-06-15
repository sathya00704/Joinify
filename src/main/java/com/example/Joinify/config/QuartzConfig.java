package com.example.Joinify.config;

import com.example.Joinify.job.EventReminderJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail eventReminderJobDetail() {
        return JobBuilder.newJob(EventReminderJob.class)
                .withIdentity("eventReminderJob")
                .withDescription("Send 24-hour event reminders")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger eventReminderTrigger() {
        // Run every 4 hours to check for events in the next 24 hours
        return TriggerBuilder.newTrigger()
                .forJob(eventReminderJobDetail())
                .withIdentity("eventReminderTrigger")
                .withDescription("Trigger for 24-hour event reminders")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 */4 * * ?")) // Every 4 hours
                .build();
    }
}
