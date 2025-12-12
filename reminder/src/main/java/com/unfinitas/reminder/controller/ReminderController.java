package com.unfinitas.reminder.controller;

import com.unfinitas.common.event.ReminderEvent;
import com.unfinitas.reminder.dto.ReminderRequest;
import com.unfinitas.reminder.entity.Reminder;
import com.unfinitas.reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;
    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createReminder(@RequestBody final ReminderRequest request) {
        final Reminder reminder = reminderService.createReminder(request);
        return ResponseEntity.ok(Map.of(
                "id", reminder.getId(),
                "status", reminder.getStatus().name(),
                "triggerTime", reminder.getTriggerTime().toString(),
                "departureTime", reminder.getDepartureTime().toString(),
                "message", String.format(
                        "Reminder set for %d minutes before departure from %s",
                        reminder.getMinutesBefore(),
                        reminder.getStopName()
                )
        ));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Reminder>> getUserReminders(@PathVariable final String userId) {
        return ResponseEntity.ok(reminderService.getUserReminders(userId));
    }

    @GetMapping("/{userId}/pending")
    public ResponseEntity<List<Reminder>> getPendingReminders(@PathVariable final String userId) {
        return ResponseEntity.ok(reminderService.getPendingReminders(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> cancelReminder(@PathVariable final String id) {
        final boolean cancelled = reminderService.cancelReminder(id);
        return ResponseEntity.ok(Map.of(
                "id", id,
                "cancelled", cancelled
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Reminder Service is running");
    }

    @PostMapping("/test-send")
    public ResponseEntity<String> testSend() {
        try {
            final ReminderEvent event = ReminderEvent.builder()
                    .reminderId("test-123")
                    .userId("user1")
                    .message("Test notification")
                    .triggerTime(LocalDateTime.now())
                    .build();

            final String json = objectMapper.writeValueAsString(event);
            jmsTemplate.convertAndSend("queue.tony.reminder.notifications", json);
            return ResponseEntity.ok("Message sent!");
        } catch (final Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
