package com.unfinitas.assistant.client;

import com.unfinitas.assistant.dto.ReminderCreateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ReminderClient {

    private final WebClient webClient;

    public ReminderClient(@Value("${services.reminder.url}") final String reminderUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(reminderUrl)
                .build();
    }

    public void createReminder(
            final String userId,
            final String stopId,
            final String stopName,
            final String routeName,
            final int minutesBefore,
            final String departureTimeIso,
            final String triggerTimeIso
    ) {
        final ReminderCreateRequest body = ReminderCreateRequest.builder()
                .userId(userId)
                .stopId(stopId)
                .stopName(stopName)
                .routeName(routeName)
                .minutesBefore(minutesBefore)
                .departureTime(departureTimeIso)
                .triggerTime(triggerTimeIso)
                .build();

        webClient.post()
                .uri("/api/reminders")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block(); // synchronous for simplicity
    }
}
