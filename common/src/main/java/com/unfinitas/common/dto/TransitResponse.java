package com.unfinitas.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitResponse {
    private String routeName;
    private String stopName;
    private LocalDateTime departureTime;
    private String destination;
    private Integer delayMinutes;
}
