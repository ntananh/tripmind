package com.unfinitas.transit.model;

import java.util.List;

public record DigitransitResponse(
        Data data
) {
    public record Data(
            Stop stop
    ) { }

    public record Stop(
            String name,
            List<Stoptime> stoptimesWithoutPatterns
    ) { }

    public record Stoptime(
            long serviceDay,
            int realtimeDeparture,
            int scheduledDeparture,
            Trip trip
    ) { }

    public record Trip(
            String routeShortName
    ) { }
}
