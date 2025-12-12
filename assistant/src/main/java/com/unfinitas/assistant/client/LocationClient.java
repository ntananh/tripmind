package com.unfinitas.assistant.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;
import java.util.List;
import java.util.Map;

@Component
public class LocationClient {
    private final WebClient webClient;

    public LocationClient(@Value("${services.location.url}") final String locationUrl) {
        this.webClient = WebClient.builder().baseUrl(locationUrl).build();
    }

    public String resolveStopId(final String stopName) {
        try {
            final List<Map<String, Object>> response = webClient.get()
                    .uri("/api/locations/search?name={name}", stopName)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();

            if (response != null && !response.isEmpty()) {
                return (String) response.getFirst().get("stopId");
            }
            return null;
        } catch (final Exception e) {
            return null;
        }
    }
}
