package com.unfinitas.assistant.model;

import lombok.Builder;

@Builder
public record ParsedIntent(
        Intent intent,
        String stopName,
        Integer minutesBefore
) {}
