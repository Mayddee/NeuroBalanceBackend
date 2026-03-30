package org.example.nbcheckinservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.DailyTaskResponse;
import org.example.nbcheckinservice.entity.DailyTask;
import org.example.nbcheckinservice.service.DailyTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Daily Tasks
 * FIXED: Extracts userId from request attribute (set by AuthFilter)
 */
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Daily Tasks", description = "Daily task management")
public class DailyTaskController {

    private final DailyTaskService taskService;

    /**
     * Extract userId from request attribute (set by AuthFilter)
     */
    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /**
     * Get today's tasks
     * GET /api/v1/tasks/today
     */
    @GetMapping("/today")
    @Operation(summary = "Get today's tasks")
    public ResponseEntity<List<DailyTaskResponse>> getTodayTasks(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /tasks/today - User {}", userId);

        List<DailyTaskResponse> tasks = taskService.getTodayTasks(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Complete a task
     * POST /api/v1/tasks/complete?taskType=COMPLETE_CHECKIN
     */
    @PostMapping("/complete")
    @Operation(summary = "Complete a task")
    public ResponseEntity<DailyTaskResponse> completeTask(
            HttpServletRequest request,
            @RequestParam DailyTask.TaskType taskType
    ) {
        Long userId = getUserId(request);
        log.info("POST /tasks/complete?taskType={} - User {}", taskType, userId);

        DailyTaskResponse task = taskService.completeTask(userId, taskType);
        return ResponseEntity.ok(task);
    }

    /**
     * Get task completion statistics
     * GET /api/v1/tasks/stats
     */
    @GetMapping("/stats")
    @Operation(summary = "Get task completion statistics")
    public ResponseEntity<DailyTaskService.TaskStatsResponse> getTaskStats(
            HttpServletRequest request
    ) {
        Long userId = getUserId(request);
        log.info("GET /tasks/stats - User {}", userId);

        DailyTaskService.TaskStatsResponse stats = taskService.getTaskStats(userId);
        return ResponseEntity.ok(stats);
    }
}