package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Entity representing daily tasks for users
 */
@Entity
@Table(name = "daily_tasks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "task_date", "task_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;

    @Column(name = "task_date", nullable = false)
    @NotNull(message = "Task date is required")
    @Builder.Default
    private LocalDate taskDate = LocalDate.now(ZoneId.of("Asia/Almaty"));

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    @NotNull(message = "Task type is required")
    private TaskType taskType;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "xp_reward", nullable = false)
    @Builder.Default
    private Integer xpReward = 50;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneId.of("Asia/Almaty"));
        if (taskDate == null) {
            taskDate = LocalDate.now(ZoneId.of("Asia/Almaty"));
        }
    }

    /**
     * Mark task as completed
     */
    public void complete() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now(ZoneId.of("Asia/Almaty"));
    }

    // ========== TASK TYPES ==========

    public enum TaskType {
        COMPLETE_CHECKIN(
                "Отметить настроение",
                "Complete your daily mood check-in",
                50
        ),
        SLEEP_7_HOURS(
                "7+ часов сна",
                "Get at least 7 hours of sleep",
                50
        ),
        PLAY_GAME(
                "Сыграть в игру",
                "Play a fun game",
                50
        ),
        LOG_MOOD(
                "Отметить состояние",
                "Log your current mood",
                50
        ),
        WRITE_NOTE(
                "Написать заметку",
                "Write a reflection note",
                50
        );

        private final String displayName;
        private final String description;
        private final int baseXp;

        TaskType(String displayName, String description, int baseXp) {
            this.displayName = displayName;
            this.description = description;
            this.baseXp = baseXp;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public int getBaseXp() {
            return baseXp;
        }
    }
}