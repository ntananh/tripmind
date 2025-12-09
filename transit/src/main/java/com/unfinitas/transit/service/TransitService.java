package com.unfinitas.transit.service;

import com.unfinitas.transit.client.DigitransitClient;
import com.unfinitas.transit.client.LocationClient;
import com.unfinitas.transit.model.DepartureDto;
import com.unfinitas.transit.model.LocationSearchResult;
import com.unfinitas.transit.model.NextDeparturesResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransitService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(10);
    private final LocationClient locationClient;
    private final DigitransitClient digitransitClient;
    private final Map<String, CachedEntry> cache = new ConcurrentHashMap<>();

    public TransitService(final LocationClient locationClient, final DigitransitClient digitransitClient) {
        this.locationClient = locationClient;
        this.digitransitClient = digitransitClient;
    }

    public NextDeparturesResponse getNextDepartures(final String stopName, final int limit) {
        final String cacheKey = stopName.toLowerCase() + ":" + limit;
        final CachedEntry cached = cache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            return cached.response();
        }

        final LocationSearchResult stop = locationClient.resolveStop(stopName);
        if (stop == null) {
            final NextDeparturesResponse empty = new NextDeparturesResponse(stopName, null, List.of());
            cache.put(cacheKey, new CachedEntry(Instant.now(), empty));
            return empty;
        }

        final List<DepartureDto> departures = digitransitClient.fetchNextDepartures(stop.id(), limit);

        final NextDeparturesResponse response = new NextDeparturesResponse(
                stop.name(),
                stop.id(),
                departures
        );

        cache.put(cacheKey, new CachedEntry(Instant.now(), response));
        return response;
    }

    private record CachedEntry(
            Instant createdAt,
            NextDeparturesResponse response
    ) {
        boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(CACHE_TTL));
        }
    }
}
