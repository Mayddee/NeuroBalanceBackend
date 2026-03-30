package org.example.nbcheckinservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nbcheckinservice.entity.DailyTask;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTaskResponse {

    private Long id;
    private DailyTask.TaskType taskType;
    private String title;
    private String description;

    private Integer xpReward;
    private Boolean isCompleted;

    private LocalDate taskDate;
    private LocalDateTime completedAt;
}