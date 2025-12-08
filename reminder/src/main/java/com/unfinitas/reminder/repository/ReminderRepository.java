package com.unfinitas.reminder.repository;

import com.unfinitas.reminder.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, String> {

    List<Reminder> findByUserId(String userId);

    List<Reminder> findByStatus(Reminder.ReminderStatus status);

    List<Reminder> findByStatusAndTriggerTimeBefore(
            Reminder.ReminderStatus status,
            LocalDateTime time
    );

    List<Reminder> findByUserIdAndStatus(String userId, Reminder.ReminderStatus status);
}
