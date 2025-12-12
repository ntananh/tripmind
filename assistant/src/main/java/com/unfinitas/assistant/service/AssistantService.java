package com.unfinitas.assistant.service;

import com.unfinitas.assistant.client.LocationClient;
import com.unfinitas.assistant.client.ReminderClient;
import com.unfinitas.assistant.client.TransitClient;
import com.unfinitas.assistant.model.ChatResponse;
import com.unfinitas.assistant.model.DepartureInfo;
import com.unfinitas.assistant.model.ParsedIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final IntentParserService intentParser;
    private final TransitClient transitClient;
    private final LocationClient locationClient;
    private final ReminderClient reminderClient;

    public ChatResponse process(final String message) {
        final ParsedIntent intent = intentParser.parse(message);
        log.info("Parsed intent: {}", intent);

        return switch (intent.intent()) {
            case NEXT_DEPARTURE -> handleNextDeparture(intent);
            case SET_REMINDER -> handleSetReminder(intent);
            case UNKNOWN -> ChatResponse.error(
                    "Sorry, I didn't understand. Try 'next bus from Keskustori'"
            );
        };
    }

    private ChatResponse handleNextDeparture(final ParsedIntent intent) {
        if (intent.stopName() == null) {
            return ChatResponse.error("Please specify a stop name");
        }

        try {
            final String stopId = locationClient.resolveStopId(intent.stopName());
            if (stopId == null) {
                return ChatResponse.error("Could not find stop: " + intent.stopName());
            }

            final List<DepartureInfo> departures = transitClient.getNextDepartures(stopId, 3);
            if (departures.isEmpty()) {
                return ChatResponse.ok("No upcoming departures from " + intent.stopName());
            }

            final StringBuilder reply = new StringBuilder(
                    "Next departures from " + intent.stopName() + ":\n"
            );

            for (final DepartureInfo dep : departures) {
                reply.append("• Line ")
                        .append(dep.route())
                        .append(" at ")
                        .append(TIME_FORMAT.format(dep.departureTime()))
                        .append("\n");
            }

            return ChatResponse.ok(reply.toString().trim());

        } catch (final Exception e) {
            log.error("Error getting departures", e);
            return ChatResponse.error("Failed to get departures: " + e.getMessage());
        }
    }

    private ChatResponse handleSetReminder(final ParsedIntent intent) {
        if (intent.stopName() == null) {
            return ChatResponse.error("Please specify a stop name");
        }
        if (intent.minutesBefore() == null) {
            return ChatResponse.error("Please specify how many minutes before departure");
        }

        try {
            final String stopId = locationClient.resolveStopId(intent.stopName());
            if (stopId == null) {
                return ChatResponse.error("Could not find stop: " + intent.stopName());
            }

            final List<DepartureInfo> departures = transitClient.getNextDepartures(stopId, 1);
            if (departures.isEmpty()) {
                return ChatResponse.error("No upcoming departures from " + intent.stopName());
            }

            final DepartureInfo nextDep = departures.getFirst();

            // Convert Instant → LocalDateTime (to match ReminderService expectations)
            LocalDateTime departureTime = LocalDateTime.ofInstant(
                    nextDep.departureTime(),
                    ZoneId.systemDefault()
            );

            LocalDateTime triggerTime = departureTime.minusMinutes(intent.minutesBefore());

            // Final string values for sending to Reminder service
            String departureTimeIso = departureTime.toString();
            String triggerTimeIso = triggerTime.toString();

            // Call reminder service
            reminderClient.createReminder(
                    "user123",
                    stopId,
                    intent.stopName(),
                    nextDep.route(),
                    intent.minutesBefore(),
                    departureTimeIso,
                    triggerTimeIso
            );

            return ChatResponse.ok(
                    "Reminder set for " + intent.minutesBefore()
                            + " minutes before line " + nextDep.route()
                            + " departs at " + TIME_FORMAT.format(nextDep.departureTime())
            );

        } catch (final Exception e) {
            log.error("Error setting reminder", e);
            return ChatResponse.error("Failed to set reminder: " + e.getMessage());
        }
    }
}
