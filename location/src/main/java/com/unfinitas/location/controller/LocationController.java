package com.unfinitas.location.controller;

import com.unfinitas.common.dto.LocationDTO;
import com.unfinitas.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/search")
    public ResponseEntity<List<LocationDTO>> search(@RequestParam final String name) {
        return ResponseEntity.ok(locationService.searchByName(name));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationDTO> getById(@PathVariable final String id) {
        return locationService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stop/{stopId}")
    public ResponseEntity<LocationDTO> getByStopId(@PathVariable final String stopId) {
        return locationService.getByStopId(stopId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<LocationDTO>> getAllStops() {
        return ResponseEntity.ok(locationService.getAllStops());
    }

    @PostMapping
    public ResponseEntity<LocationDTO> create(@RequestBody final LocationDTO location) {
        return ResponseEntity.ok(locationService.save(location));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Location Service is running");
    }
}
