package com.unfinitas.transit.client;

import com.unfinitas.transit.model.LocationSearchResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Component
public class LocationClient {
    private final WebClient webClient;

    public LocationClient(@Qualifier("locationWebClient") final WebClient webClient) {
        this.webClient = webClient;
    }

    public LocationSearchResult resolveStop(final String stopId) {
        return webClient
                .get()
                .uri("/api/locations/stop/{id}",stopId)
                .retrieve()
                .bodyToMono(LocationSearchResult.class)
                .block();
    }

    public LocationSearchResult resolveStopByName(final String stopName) {
        final var results = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/locations/search")
                        .queryParam("name", stopName)
                        .build())
                .retrieve()
                .bodyToFlux(LocationSearchResult.class)
                .collectList()
                .block();

        if (results == null || results.isEmpty()) {
            throw new RuntimeException("Stop not found: " + stopName);
        }

        return results.getFirst();
    }
}
