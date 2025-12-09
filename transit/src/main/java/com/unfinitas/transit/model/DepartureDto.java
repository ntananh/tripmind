package com.unfinitas.transit.model;

import java.time.Instant;

public record DepartureDto(
        String route,
        Instant departureTime
) { }
