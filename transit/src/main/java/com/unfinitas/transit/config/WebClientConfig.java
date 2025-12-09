package com.unfinitas.transit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean(name = "locationWebClient")
    public WebClient locationWebClient(
            @Value("${tripmind.location-service.base-url}") final String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean(name = "digitransitWebClient")
    public WebClient digitransitWebClient(
            @Value("${tripmind.digitransit.base-url}") final String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
