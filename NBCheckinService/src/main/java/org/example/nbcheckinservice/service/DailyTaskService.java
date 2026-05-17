package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.DailyTaskResponse;
import org.example.nbcheckinservice.entity.DailyTask;
import org.example.nbcheckinservice.repository.DailyTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing daily tasks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyTaskService {

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    private final DailyTaskRepository taskRepository;
    private final UserCharacterService characterService;

    @Transactional
    public List<DailyTaskResponse> getTodayTasks(Long userId) {
        LocalDate today = LocalDate.now(ALMATY_ZONE);

        List<DailyTask> existingTasks = taskRepository.findByUserIdAndTaskDate(userId, today);

        if (existingTasks.isEmpty()) {
            existingTasks = createDailyTasks(userId, today);
        }

        return existingTasks.stream()
                .map(this::buildTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DailyTaskResponse completeTask(Long userId, DailyTask.TaskType taskType) {
        return completeTask(userId, taskType, LocalDate.now(ALMATY_ZONE));
    }

    @Transactional
    public DailyTaskResponse completeTask(Long userId, DailyTask.TaskType taskType, LocalDate date) {
        DailyTask task = taskRepository
                .findByUserIdAndTaskDateAndTaskType(userId, date, taskType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Task " + taskType + " not found for date " + date));

        if (task.getIsCompleted()) {
            log.warn("Task {} already completed for user {} on {}", taskType, userId, date);
            return buildTaskResponse(task);
        }

        task.complete();
        DailyTask savedTask = taskRepository.save(task);
        characterService.addXp(userId, task.getXpReward());

        log.info("Task {} completed for user {} on {}, awarded {} XP",
                taskType, userId, date, task.getXpReward());

        return buildTaskResponse(savedTask);
    }

    @Transactional
    public void autoCompleteTask(Long userId, DailyTask.TaskType taskType) {
        autoCompleteTask(userId, taskType, LocalDate.now(ALMATY_ZONE));
    }

    @Transactional
    public void autoCompleteTask(Long userId, DailyTask.TaskType taskType, LocalDate date) {
        taskRepository.findByUserIdAndTaskDateAndTaskType(userId, date, taskType)
                .ifPresent(task -> {
                    if (!task.getIsCompleted()) {
                        completeTask(userId, taskType, date);
                    }
                });
    }

    /** Get (or create) tasks for any date, not just today. */
    @Transactional
    public List<DailyTaskResponse> getTasksForDate(Long userId, LocalDate date) {
        List<DailyTask> tasks = taskRepository.findByUserIdAndTaskDate(userId, date);
        if (tasks.isEmpty()) {
            tasks = createDailyTasks(userId, date);
        }
        return tasks.stream().map(this::buildTaskResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DailyTaskResponse> getTasksByDate(Long userId, LocalDate date) {
        return taskRepository.findByUserIdAndTaskDate(userId, date)
                .stream().map(this::buildTaskResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTaskHistory(Long userId, LocalDate startDate, LocalDate endDate) {
        return taskRepository.findByUserIdAndTaskDateBetween(userId, startDate, endDate)
                .stream()
                .collect(Collectors.groupingBy(t -> t.getTaskDate().toString()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, List<DailyTask>>comparingByKey().reversed())
                .map(e -> {
                    List<DailyTask> dayTasks = e.getValue();
                    long completed = dayTasks.stream().filter(DailyTask::getIsCompleted).count();
                    return Map.<String, Object>of(
                            "date", e.getKey(),
                            "tasks", dayTasks.stream().map(this::buildTaskResponse).collect(Collectors.toList()),
                            "completedCount", completed,
                            "totalCount", dayTasks.size(),
                            "completionPercentage", dayTasks.isEmpty() ? 0.0 : (completed * 100.0) / dayTasks.size()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskStatsResponse getTaskStats(Long userId) {
        LocalDate today = LocalDate.now(ALMATY_ZONE);
        List<DailyTask> todayTasks = taskRepository.findByUserIdAndTaskDate(userId, today);

        long completedCount = todayTasks.stream()
                .filter(DailyTask::getIsCompleted)
                .count();

        int totalXpEarned = todayTasks.stream()
                .filter(DailyTask::getIsCompleted)
                .mapToInt(DailyTask::getXpReward)
                .sum();

        return TaskStatsResponse.builder()
                .totalTasks(todayTasks.size())
                .completedTasks((int) completedCount)
                .xpEarned(totalXpEarned)
                .completionPercentage((completedCount * 100.0) / todayTasks.size())
                .build();
    }

    // ========== HELPER METHODS ==========

    private List<DailyTask> createDailyTasks(Long userId, LocalDate date) {
        log.info("Creating daily tasks for user {} on {}", userId, date);

        List<DailyTask> tasks = new ArrayList<>();

        for (DailyTask.TaskType taskType : DailyTask.TaskType.values()) {
            DailyTask task = DailyTask.builder()
                    .userId(userId)
                    .taskDate(date)
                    .taskType(taskType)
                    .xpReward(taskType.getBaseXp())
                    .isCompleted(false)
                    .build();

            tasks.add(task);
        }

        return taskRepository.saveAll(tasks);
    }

    private DailyTaskResponse buildTaskResponse(DailyTask task) {
        return DailyTaskResponse.builder()
                .id(task.getId())
                .taskType(task.getTaskType())
                .title(task.getTaskType().getDisplayName())
                .description(task.getTaskType().getDescription())
                .xpReward(task.getXpReward())
                .isCompleted(task.getIsCompleted())
                .completedAt(task.getCompletedAt())
                .taskDate(task.getTaskDate())
                .build();
    }

    // ========== DTO ==========

    @lombok.Data
    @lombok.Builder
    public static class TaskStatsResponse {
        private Integer totalTasks;
        private Integer completedTasks;
        private Integer xpEarned;
        private Double completionPercentage;
    }
}