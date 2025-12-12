package com.unfinitas.assistant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReminderCreateRequest {
    private String userId;
    private String stopId;
    private String stopName;
    private String routeName;
    private int minutesBefore;
    private String departureTime;
    private String triggerTime;
}
