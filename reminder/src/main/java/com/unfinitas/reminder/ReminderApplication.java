package com.unfinitas.reminder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJms
public class ReminderApplication {
    public static void main(final String[] args) {
        SpringApplication.run(ReminderApplication.class, args);
    }
}
