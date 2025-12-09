package com.unfinitas.transit.client;

import com.unfinitas.transit.model.LocationSearchResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class LocationClient {

    private final WebClient webClient;

    public LocationClient(@Qualifier("locationWebClient") final WebClient webClient) {
        this.webClient = webClient;
    }

    public LocationSearchResult resolveStop(final String stopName) {
        final List<LocationSearchResult> results = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/locations/search")
                        .queryParam("name", stopName)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<LocationSearchResult>>() {
                })
                .block();

        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.getFirst(); // POC: first match
    }
}
