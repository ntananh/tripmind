package com.unfinitas.assistant.service;

import com.unfinitas.assistant.model.Intent;
import com.unfinitas.assistant.model.ParsedIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IntentParserService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You are a transit assistant intent parser. Parse the user message and extract:
            1. intent: NEXT_DEPARTURE, SET_REMINDER, or UNKNOWN
            2. stopName: the transit stop name mentioned (in Finnish)
            3. minutesBefore: for reminders, how many minutes before departure
            
            Respond ONLY in this exact JSON format, nothing else:
            {"intent":"NEXT_DEPARTURE","stopName":"Keskustori","minutesBefore":null}
            
            Examples:
            - "next bus from Keskustori" -> {"intent":"NEXT_DEPARTURE","stopName":"Keskustori","minutesBefore":null}
            - "when is the next tram from Koskipuisto" -> {"intent":"NEXT_DEPARTURE","stopName":"Koskipuisto","minutesBefore":null}
            - "remind me 5 minutes before bus from Hervanta" -> {"intent":"SET_REMINDER","stopName":"Hervanta","minutesBefore":5}
            - "hello" -> {"intent":"UNKNOWN","stopName":null,"minutesBefore":null}
            """;

    public IntentParserService(final ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public ParsedIntent parse(final String message) {
        log.info("Parsing message with Gemini: {}", message);

        try {
            final String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(message)
                    .call()
                    .content();

            log.info("Gemini response: {}", response);
            return parseJsonResponse(response);
        } catch (final Exception e) {
            log.error("Error calling Gemini", e);
            return ParsedIntent.builder().intent(Intent.UNKNOWN).build();
        }
    }

    private ParsedIntent parseJsonResponse(String json) {
        try {
            json = json.replace("```json", "").replace("```", "").trim();

            final String intent = extractJsonField(json, "intent");
            final String stopName = extractJsonField(json, "stopName");
            final String minutesBefore = extractJsonField(json, "minutesBefore");

            return ParsedIntent.builder()
                    .intent(Intent.valueOf(intent))
                    .stopName("null".equals(stopName) ? null : stopName)
                    .minutesBefore("null".equals(minutesBefore) ? null : Integer.parseInt(minutesBefore))
                    .build();
        } catch (final Exception e) {
            log.error("Failed to parse JSON: {}", json, e);
            return ParsedIntent.builder().intent(Intent.UNKNOWN).build();
        }
    }

    private String extractJsonField(final String json, final String field) {
        final String pattern = "\"" + field + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return "null";

        start += pattern.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;

        if (json.charAt(start) == '"') {
            final int end = json.indexOf('"', start + 1);
            return json.substring(start + 1, end);
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }
}
