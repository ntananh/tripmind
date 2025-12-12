package com.unfinitas.assistant.client;

import com.unfinitas.assistant.model.DepartureInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class TransitClient {

    private final WebClient webClient;

    public TransitClient(@Value("${services.transit.url}") final String transitUrl) {
        this.webClient = WebClient.builder().baseUrl(transitUrl).build();
    }

    public List<DepartureInfo> getNextDepartures(final String stopId, final int limit) {
        final String json = webClient.get()
                .uri("/api/transit/departures?stopId={stopId}&limit={limit}", stopId, limit)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (json == null || json.isBlank()) {
            return List.of();
        }

        final ObjectMapper mapper = new ObjectMapper();
        final List<Map<String, Object>> items = mapper.readValue(
                json, new TypeReference<>() {
                }
        );

        return items.stream()
                .map(m -> new DepartureInfo(
                        (String) m.get("route"),
                        Instant.parse((String) m.get("departureTime"))
                ))
                .toList();
    }
}
