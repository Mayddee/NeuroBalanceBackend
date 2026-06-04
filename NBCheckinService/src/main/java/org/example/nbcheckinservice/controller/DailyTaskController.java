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
     * Complete a task, optionally for a specific date (Asia/Almaty). Defaults to today.
     * POST /api/v1/tasks/complete?taskType=COMPLETE_CHECKIN
     * POST /api/v1/tasks/complete?taskType=COMPLETE_CHECKIN&date=2026-05-15
     */
    @PostMapping("/complete")
    @Operation(summary = "Complete a task (optional date, defaults to today Asia/Almaty)")
    public ResponseEntity<DailyTaskResponse> completeTask(
            HttpServletRequest request,
            @RequestParam DailyTask.TaskType taskType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserId(request);
        LocalDate targetDate = date != null ? date : LocalDate.now(ZoneId.of("Asia/Almaty"));
        log.info("POST /tasks/complete?taskType={}&date={} - User {}", taskType, targetDate, userId);

        DailyTaskResponse task = taskService.completeTask(userId, taskType, targetDate);
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
     * Get (or create) tasks for a specific date (Asia/Almaty).
     * Creates the full set of 5 tasks for that date if they don't exist yet.
     * GET /api/v1/tasks/date/2026-05-15
     */
    @GetMapping("/date/{date}")
    @Operation(summary = "Get tasks for a specific date (creates if not yet initialized)")
    public ResponseEntity<List<DailyTaskResponse>> getTasksByDate(
            HttpServletRequest request,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserId(request);
        log.info("GET /tasks/date/{} - User {}", date, userId);
        return ResponseEntity.ok(taskService.getTasksForDate(userId, date));
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
     * Called by NoteAI-backend when a user writes a note. Optional date (Asia/Almaty), defaults to today.
     *
     * POST /api/v1/tasks/note-written
     * POST /api/v1/tasks/note-written?date=2026-05-15
     */
    @PostMapping("/note-written")
    @Operation(summary = "Mark WRITE_NOTE task as completed (optional date, defaults to today)")
    public ResponseEntity<?> noteWritten(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserId(request);
        if (userId == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "Unauthorized"));
        }
        LocalDate targetDate = date != null ? date : LocalDate.now(ZoneId.of("Asia/Almaty"));
        log.info("POST /tasks/note-written?date={} - auto-completing WRITE_NOTE for user {}", targetDate, userId);

        // Ensure tasks exist for this date BEFORE trying to auto-complete.
        // Without this, autoCompleteTask silently does nothing (ifPresent finds nothing).
        // Same pattern as DailyCheckInService.createCheckIn().
        taskService.getTasksForDate(userId, targetDate);
        taskService.autoCompleteTask(userId, DailyTask.TaskType.WRITE_NOTE, targetDate);

        return ResponseEntity.ok(java.util.Map.of(
                "message", "WRITE_NOTE task completed",
                "userId", userId,
                "date", targetDate.toString()
        ));
    }
}