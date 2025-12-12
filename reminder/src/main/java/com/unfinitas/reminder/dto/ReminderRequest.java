package com.unfinitas.reminder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderRequest {

    private String userId;
    private String stopName;
    private String stopId;
    private String routeName;
    private Integer minutesBefore;

    private String departureTime;
    private String triggerTime;
}
