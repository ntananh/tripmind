package com.unfinitas.transit.client;

import com.unfinitas.transit.model.DepartureDto;
import com.unfinitas.transit.model.DigitransitResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class DigitransitClient {

    private final WebClient webClient;

    public DigitransitClient(@Qualifier("digitransitWebClient") final WebClient webClient) {
        this.webClient = webClient;
    }

    public List<DepartureDto> fetchNextDepartures(final String stopId, final int limit) {
        try {
            return fetchFromApi(stopId, limit);
        } catch (final Exception e) {
            // Fallback to mock data when API is unavailable or unauthorized
            return getMockDepartures(limit);
        }
    }

    private List<DepartureDto> fetchFromApi(final String stopId, final int limit) {
        final String graphQlQuery = String.format("""
                {
                  stop(id: "%s") {
                    name
                    stoptimesWithoutPatterns(numberOfDepartures: %d) {
                      serviceDay
                      realtimeDeparture
                      scheduledDeparture
                      trip {
                        routeShortName
                      }
                    }
                  }
                }
                """, stopId, limit);

        final DigitransitResponse response = webClient
                .post()
                .uri("/routing/v2/waltti/gtfs/v1")
                .bodyValue(Map.of("query", graphQlQuery))
                .retrieve()
                .bodyToMono(DigitransitResponse.class)
                .block();

        if (response == null || response.data() == null || response.data().stop() == null) {
            return List.of();
        }

        final var stop = response.data().stop();
        if (stop.stoptimesWithoutPatterns() == null) {
            return List.of();
        }

        return stop.stoptimesWithoutPatterns().stream()
                .map(st -> {
                    final long epochSeconds = st.serviceDay() + st.realtimeDeparture();
                    final Instant time = Instant.ofEpochSecond(epochSeconds);
                    final String route = st.trip() != null ? st.trip().routeShortName() : "?";
                    return new DepartureDto(route, time);
                })
                .collect(Collectors.toList());
    }

    private List<DepartureDto> getMockDepartures(final int limit) {
        return Stream.of(
                new DepartureDto("3", Instant.now().plusSeconds(180)),
                new DepartureDto("1", Instant.now().plusSeconds(420)),
                new DepartureDto("25", Instant.now().plusSeconds(660)),
                new DepartureDto("8", Instant.now().plusSeconds(900)),
                new DepartureDto("3", Instant.now().plusSeconds(1200))
        ).limit(limit).toList();
    }
}
