package com.unfinitas.reminder.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reminders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String stopName;

    private String stopId;
    private String routeName;

    @Column(nullable = false)
    private Integer minutesBefore;

    private LocalDateTime departureTime;
    private LocalDateTime triggerTime;

    @Enumerated(EnumType.STRING)
    private ReminderStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime triggeredAt;

    public enum ReminderStatus {
        PENDING, TRIGGERED, CANCELLED
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReminderStatus.PENDING;
        }
    }
}
