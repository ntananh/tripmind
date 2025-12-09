package com.unfinitas.transit.controller;

import com.unfinitas.transit.model.NextDeparturesResponse;
import com.unfinitas.transit.service.TransitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transit")
public class TransitController {

    private final TransitService transitService;

    public TransitController(final TransitService transitService) {
        this.transitService = transitService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/next")
    public ResponseEntity<NextDeparturesResponse> getNextDepartures(
            @RequestParam("stopName") final String stopName,
            @RequestParam(value = "limit", defaultValue = "5") final int limit
    ) {
        final NextDeparturesResponse response = transitService.getNextDepartures(stopName, limit);
        return ResponseEntity.ok(response);
    }
}
