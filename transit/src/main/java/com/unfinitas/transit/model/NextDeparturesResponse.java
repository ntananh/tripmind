package com.unfinitas.transit.model;

import java.util.List;

public record NextDeparturesResponse(
        String stopName,
        String stopId,
        List<DepartureDto> departures
) { }
