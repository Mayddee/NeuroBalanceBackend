package org.example.nbcheckinservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.DailyTaskResponse;
import org.example.nbcheckinservice.entity.DailyTask;
import org.example.nbcheckinservice.service.DailyTaskService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

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

    /**
     * Get tasks for a specific date (Asia/Almaty)
     * GET /api/v1/tasks/date/2026-05-11
     */
    @GetMapping("/date/{date}")
    @Operation(summary = "Get tasks for a specific date")
    public ResponseEntity<List<DailyTaskResponse>> getTasksByDate(
            HttpServletRequest request,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserId(request);
        log.info("GET /tasks/date/{} - User {}", date, userId);
        return ResponseEntity.ok(taskService.getTasksByDate(userId, date));
    }

    /**
     * Get task history for a date range (Asia/Almaty)
     * GET /api/v1/tasks/history?startDate=2026-05-01&endDate=2026-05-11
     */
    @GetMapping("/history")
    @Operation(summary = "Get task completion history grouped by date")
    public ResponseEntity<?> getTaskHistory(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = getUserId(request);
        LocalDate end = endDate != null ? endDate : LocalDate.now(ZoneId.of("Asia/Almaty"));
        log.info("GET /tasks/history [{} → {}] - User {}", startDate, end, userId);
        return ResponseEntity.ok(taskService.getTaskHistory(userId, startDate, end));
    }

    /**
     * Called by NoteAI-backend (internal) when a user writes a note.
     * Autocompletes the WRITE_NOTE daily task and awards XP.
     *
     * POST /api/v1/tasks/note-written
     * Header: Authorization: Bearer <jwt>
     */
    @PostMapping("/note-written")
    @Operation(summary = "Mark WRITE_NOTE task as completed (called after note is written)")
    public ResponseEntity<?> noteWritten(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "Unauthorized"));
        }
        log.info("POST /tasks/note-written - auto-completing WRITE_NOTE for user {}", userId);
        taskService.autoCompleteTask(userId, DailyTask.TaskType.WRITE_NOTE);
        return ResponseEntity.ok(java.util.Map.of(
                "message", "WRITE_NOTE task completed",
                "userId", userId
        ));
    }
}