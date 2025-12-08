package com.unfinitas.reminder.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unfinitas.common.event.ReminderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ObjectMapper objectMapper;

    @JmsListener(destination = "queue.tony.reminder.notifications")
    public void handleReminderEvent(final String json) {
        try {
            final ReminderEvent event = objectMapper.readValue(json, ReminderEvent.class);

            log.info("========================================");
            log.info("NOTIFICATION RECEIVED!");
            log.info("Reminder ID: {}", event.getReminderId());
            log.info("User: {}", event.getUserId());
            log.info("Message: {}", event.getMessage());
            log.info("Departure Time: {}", event.getDepartureTime());
            log.info("========================================");

            simulateNotificationSend(event);
        } catch (final Exception e) {
            log.error("Failed to parse reminder event", e);
        }
    }

    private void simulateNotificationSend(final ReminderEvent event) {
        log.info("[SIMULATED] Sending push notification to user {}: {}",
                event.getUserId(),
                event.getMessage());
    }
}
