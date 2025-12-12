package com.unfinitas.reminder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unfinitas.common.event.ReminderEvent;
import com.unfinitas.reminder.dto.ReminderRequest;
import com.unfinitas.reminder.entity.Reminder;
import com.unfinitas.reminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${artemis.queue.notifications}")
    private String notificationQueue;

    /**
     * Create and schedule a reminder using JMS delayed delivery.
     */
    public Reminder createReminder(final ReminderRequest request) {
        log.info("Creating reminder for user {} at stop {}", request.getUserId(), request.getStopName());

        // Parse ISO-8601 timestamps
        final LocalDateTime departureTime = LocalDateTime.parse(request.getDepartureTime());
        final LocalDateTime triggerTime = LocalDateTime.parse(request.getTriggerTime());

        // Save reminder
        Reminder reminder = Reminder.builder()
                .userId(request.getUserId())
                .stopName(request.getStopName())
                .stopId(request.getStopId())
                .routeName(request.getRouteName())
                .minutesBefore(request.getMinutesBefore())
                .departureTime(departureTime)
                .triggerTime(triggerTime)
                .status(Reminder.ReminderStatus.PENDING)
                .build();

        reminder = reminderRepository.save(reminder);

        // Compute JMS delay
        long delayMs = Duration.between(LocalDateTime.now(), triggerTime).toMillis();
        delayMs = Math.max(delayMs, 0);

        // Build event payload
        final ReminderEvent event = ReminderEvent.builder()
                .reminderId(reminder.getId())
                .userId(reminder.getUserId())
                .stopId(reminder.getStopId())
                .routeName(reminder.getRouteName())
                .message(String.format(
                        "Reminder: Bus %s departs from %s in %d minutes!",
                        reminder.getRouteName(),
                        reminder.getStopName(),
                        reminder.getMinutesBefore()
                ))
                .triggerTime(triggerTime)
                .departureTime(departureTime)
                .build();

        try {
            final String json = objectMapper.writeValueAsString(event);

            final long finalDelayMs = delayMs;
            jmsTemplate.convertAndSend(notificationQueue, json, msg -> {
                msg.setLongProperty("_AMQ_SCHED_DELIVERY_DELAY", finalDelayMs);
                return msg;
            });

            log.info("Scheduled reminder {} to fire after {} ms", reminder.getId(), delayMs);

        } catch (final Exception e) {
            log.error("Failed to schedule reminder event", e);
        }

        return reminder;
    }

    /**
     * List ALL reminders for a user.
     */
    public List<Reminder> getUserReminders(final String userId) {
        return reminderRepository.findByUserId(userId);
    }

    /**
     * List only pending reminders (not triggered or cancelled).
     */
    public List<Reminder> getPendingReminders(final String userId) {
        return reminderRepository.findByUserIdAndStatus(userId, Reminder.ReminderStatus.PENDING);
    }

    /**
     * Cancel a scheduled reminder.
     */
    public boolean cancelReminder(final String reminderId) {
        return reminderRepository.findById(reminderId)
                .map(reminder -> {
                    reminder.setStatus(Reminder.ReminderStatus.CANCELLED);
                    reminderRepository.save(reminder);
                    log.info("Cancelled reminder {}", reminderId);
                    return true;
                })
                .orElse(false);
    }
}
