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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    public Reminder createReminder(final ReminderRequest request) {
        log.info("Creating reminder for user {} at stop {}", request.getUserId(), request.getStopName());

        final LocalDateTime departureTime = LocalDateTime.now().plusMinutes(10);
        final LocalDateTime triggerTime = departureTime.minusMinutes(
                request.getMinutesBefore() != null ? request.getMinutesBefore() : 5
        );

        Reminder reminder = Reminder.builder()
                .userId(request.getUserId())
                .stopName(request.getStopName())
                .stopId(request.getStopId())
                .routeName(request.getRouteName())
                .minutesBefore(request.getMinutesBefore() != null ? request.getMinutesBefore() : 5)
                .departureTime(departureTime)
                .triggerTime(triggerTime)
                .status(Reminder.ReminderStatus.PENDING)
                .build();

        reminder = reminderRepository.save(reminder);
        log.info("Created reminder {} with trigger time {}", reminder.getId(), triggerTime);

        return reminder;
    }

    public List<Reminder> getUserReminders(final String userId) {
        return reminderRepository.findByUserId(userId);
    }

    public List<Reminder> getPendingReminders(final String userId) {
        return reminderRepository.findByUserIdAndStatus(userId, Reminder.ReminderStatus.PENDING);
    }

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

    @Scheduled(fixedRate = 30000)
    public void checkAndTriggerReminders() {
        final LocalDateTime now = LocalDateTime.now();
        log.debug("Checking for reminders to trigger at {}", now);

        final List<Reminder> dueReminders = reminderRepository.findByStatusAndTriggerTimeBefore(
                Reminder.ReminderStatus.PENDING,
                now
        );

        for (final Reminder reminder : dueReminders) {
            triggerReminder(reminder);
        }

        if (!dueReminders.isEmpty()) {
            log.info("Triggered {} reminders", dueReminders.size());
        }
    }

    private void triggerReminder(final Reminder reminder) {
        log.info("Triggering reminder {} for user {}", reminder.getId(), reminder.getUserId());

        final ReminderEvent event = ReminderEvent.builder()
                .reminderId(reminder.getId())
                .userId(reminder.getUserId())
                .stopId(reminder.getStopId())
                .routeName(reminder.getRouteName())
                .message(String.format(
                        "Reminder: Bus %s departs from %s in %d minutes!",
                        reminder.getRouteName() != null ? reminder.getRouteName() : "N/A",
                        reminder.getStopName(),
                        reminder.getMinutesBefore()
                ))
                .triggerTime(LocalDateTime.now())
                .departureTime(reminder.getDepartureTime())
                .build();

        try {
            final String json = objectMapper.writeValueAsString(event);
            jmsTemplate.convertAndSend(notificationQueue, json);
            log.info("Published reminder event to Artemis queue: {}", notificationQueue);
        } catch (final Exception e) {
            log.error("Failed to publish reminder event", e);
        }

        reminder.setStatus(Reminder.ReminderStatus.TRIGGERED);
        reminder.setTriggeredAt(LocalDateTime.now());
        reminderRepository.save(reminder);
    }
}
