package com.unfinitas.location.service;

import com.unfinitas.common.dto.LocationDTO;
import com.unfinitas.location.entity.Location;
import com.unfinitas.location.repository.LocationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    @PostConstruct
    public void seedData() {
        if (locationRepository.count() == 0) {
            log.info("Seeding location data...");

            locationRepository.saveAll(List.of(
                    Location.builder()
                            .name("Keskustori")
                            .stopId("tampere:0001")
                            .latitude(61.4978)
                            .longitude(23.7610)
                            .type(Location.LocationType.STOP)
                            .build(),
                    Location.builder()
                            .name("Rautatieasema")
                            .stopId("tampere:0002")
                            .latitude(61.4985)
                            .longitude(23.7730)
                            .type(Location.LocationType.STOP)
                            .build(),
                    Location.builder()
                            .name("Hervanta")
                            .stopId("tampere:0003")
                            .latitude(61.4505)
                            .longitude(23.8510)
                            .type(Location.LocationType.STOP)
                            .build(),
                    Location.builder()
                            .name("Lielahti")
                            .stopId("tampere:0004")
                            .latitude(61.5180)
                            .longitude(23.6990)
                            .type(Location.LocationType.STOP)
                            .build(),
                    Location.builder()
                            .name("Tays")
                            .stopId("tampere:0005")
                            .latitude(61.5050)
                            .longitude(23.8160)
                            .type(Location.LocationType.STOP)
                            .build()
            ));

            log.info("Seeded {} locations", locationRepository.count());
        }
    }

    public List<LocationDTO> searchByName(final String name) {
        log.info("Searching locations by name: {}", name);
        return locationRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public Optional<LocationDTO> getById(final String id) {
        return locationRepository.findById(id).map(this::toDTO);
    }

    public Optional<LocationDTO> getByStopId(final String stopId) {
        return locationRepository.findByStopId(stopId).map(this::toDTO);
    }

    public LocationDTO save(final LocationDTO dto) {
        final Location location = Location.builder()
                .name(dto.getName())
                .stopId(dto.getStopId())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .type(Location.LocationType.valueOf(dto.getType()))
                .build();
        return toDTO(locationRepository.save(location));
    }

    public List<LocationDTO> getAllStops() {
        return locationRepository.findByType(Location.LocationType.STOP)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private LocationDTO toDTO(final Location location) {
        return LocationDTO.builder()
                .id(location.getId())
                .name(location.getName())
                .stopId(location.getStopId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .type(location.getType().name())
                .build();
    }
}
