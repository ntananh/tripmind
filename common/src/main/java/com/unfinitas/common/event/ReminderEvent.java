package com.unfinitas.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderEvent implements Serializable {
    private String reminderId;
    private String userId;
    private String message;
    private String stopId;
    private String routeName;
    private LocalDateTime triggerTime;
    private LocalDateTime departureTime;
}
