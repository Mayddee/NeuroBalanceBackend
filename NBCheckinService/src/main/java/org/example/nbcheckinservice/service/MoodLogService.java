package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.MoodLogRequest;
import org.example.nbcheckinservice.dto.MoodLogResponse;
import org.example.nbcheckinservice.entity.DailyTask;
import org.example.nbcheckinservice.entity.MoodLog;
import org.example.nbcheckinservice.repository.MoodLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing mood logs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoodLogService {

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    private final MoodLogRepository moodLogRepository;
    private final DailyTaskService dailyTaskService;

    @Transactional
    public MoodLogResponse createMoodLog(Long userId, MoodLogRequest request) {
        log.info("Creating mood log for user {}", userId);

        MoodLog moodLog = MoodLog.builder()
                .userId(userId)
                .logTimestamp(request.getLogTimestamp() != null
                        ? request.getLogTimestamp()
                        : LocalDateTime.now(ALMATY_ZONE))
                .moodValue(request.getMoodValue())
                .moodLabel(request.getMoodLabel())
                .intensity(request.getIntensity())
                .contextNote(request.getContextNote())
                .location(request.getLocation())
                .activity(request.getActivity())
                .triggers(request.getTriggers())
                .physicalSensations(request.getPhysicalSensations())
                .build();

        MoodLog savedLog = moodLogRepository.save(moodLog);
        log.info("Mood log created with ID: {}", savedLog.getId());

        dailyTaskService.autoCompleteTask(userId, DailyTask.TaskType.LOG_MOOD);

        return mapToResponse(savedLog);
    }

    @Transactional(readOnly = true)
    public MoodLogResponse getMoodLog(Long userId, Long id) {
        log.debug("Fetching mood log {} for user {}", id, userId);

        MoodLog moodLog = moodLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mood log not found or access denied"
                ));

        return mapToResponse(moodLog);
    }

    @Transactional(readOnly = true)
    public List<MoodLogResponse> getAllMoodLogs(Long userId) {
        log.debug("Fetching all mood logs for user {}", userId);

        List<MoodLog> logs = moodLogRepository.findByUserIdOrderByLogTimestampDesc(userId);

        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MoodLogResponse> getRecentMoodLogs(Long userId) {
        log.debug("Fetching recent mood logs for user {}", userId);

        List<MoodLog> logs = moodLogRepository.findTop20ByUserIdOrderByLogTimestampDesc(userId);

        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MoodLogResponse> getTodayMoodLogs(Long userId) {
        log.debug("Fetching today's mood logs for user {}", userId);

        LocalDate today = LocalDate.now(ALMATY_ZONE);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        List<MoodLog> logs = moodLogRepository.findByUserIdAndLogTimestampBetweenOrderByLogTimestampDesc(
                userId, startOfDay, endOfDay
        );

        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MoodLogResponse> getMoodLogsInRange(
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        log.debug("Fetching mood logs for user {} from {} to {}", userId, startTime, endTime);

        List<MoodLog> logs = moodLogRepository.findByUserIdAndLogTimestampBetweenOrderByLogTimestampDesc(
                userId, startTime, endTime
        );

        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MoodLogResponse updateMoodLog(Long userId, Long id, MoodLogRequest request) {
        log.info("Updating mood log {} for user {}", id, userId);

        MoodLog moodLog = moodLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mood log not found or access denied"
                ));

        if (request.getMoodValue() != null) {
            moodLog.setMoodValue(request.getMoodValue());
            moodLog.setMoodEmoji(MoodLog.getMoodEmojiByValue(request.getMoodValue()));
        }
        if (request.getMoodLabel() != null) {
            moodLog.setMoodLabel(request.getMoodLabel());
        }
        if (request.getIntensity() != null) {
            moodLog.setIntensity(request.getIntensity());
        }
        if (request.getContextNote() != null) {
            moodLog.setContextNote(request.getContextNote());
        }
        if (request.getLocation() != null) {
            moodLog.setLocation(request.getLocation());
        }
        if (request.getActivity() != null) {
            moodLog.setActivity(request.getActivity());
        }
        if (request.getTriggers() != null) {
            moodLog.setTriggers(request.getTriggers());
        }
        if (request.getPhysicalSensations() != null) {
            moodLog.setPhysicalSensations(request.getPhysicalSensations());
        }

        MoodLog updatedLog = moodLogRepository.save(moodLog);
        log.info("Mood log {} updated successfully", id);

        return mapToResponse(updatedLog);
    }

    @Transactional
    public void deleteMoodLog(Long userId, Long id) {
        log.info("Deleting mood log {} for user {}", id, userId);

        MoodLog moodLog = moodLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mood log not found or access denied"
                ));

        moodLogRepository.delete(moodLog);
        log.info("Mood log {} deleted successfully", id);
    }

    @Transactional(readOnly = true)
    public Double getAverageMood(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Calculating average mood for user {} from {} to {}", userId, startTime, endTime);

        List<MoodLog> logs = moodLogRepository.findByUserIdAndLogTimestampBetweenOrderByLogTimestampDesc(
                userId, startTime, endTime
        );

        if (logs.isEmpty()) {
            return 0.0;
        }

        double sum = logs.stream()
                .mapToInt(MoodLog::getMoodValue)
                .sum();

        return sum / logs.size();
    }

    @Transactional(readOnly = true)
    public List<MoodLogResponse> getMoodLogsByTrigger(Long userId, String trigger) {
        log.debug("Fetching mood logs for user {} with trigger {}", userId, trigger);

        List<MoodLog> logs = moodLogRepository.findByUserIdOrderByLogTimestampDesc(userId)
                .stream()
                .filter(log -> log.getTriggers() != null && log.getTriggers().contains(trigger))
                .collect(Collectors.toList());

        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========== HELPER METHODS ==========

    private MoodLogResponse mapToResponse(MoodLog moodLog) {
        return MoodLogResponse.builder()
                .id(moodLog.getId())
                .userId(moodLog.getUserId())
                .logTimestamp(moodLog.getLogTimestamp())
                .moodValue(moodLog.getMoodValue())
                .moodEmoji(moodLog.getMoodEmoji())
                .moodLabel(moodLog.getMoodLabel())
                .intensity(moodLog.getIntensity())
                .contextNote(moodLog.getContextNote())
                .location(moodLog.getLocation())
                .activity(moodLog.getActivity())
                .triggers(moodLog.getTriggers())
                .physicalSensations(moodLog.getPhysicalSensations())
                .createdAt(moodLog.getCreatedAt())
                .build();
    }
}