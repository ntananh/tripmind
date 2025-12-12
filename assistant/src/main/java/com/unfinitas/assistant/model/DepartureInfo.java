package com.unfinitas.assistant.model;

import java.time.Instant;

public record DepartureInfo(String route, Instant departureTime) {
}
