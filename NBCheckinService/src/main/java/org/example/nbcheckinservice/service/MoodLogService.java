package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.MoodLogRequest;
import org.example.nbcheckinservice.dto.MoodLogResponse;
import org.example.nbcheckinservice.entity.MoodLog;
import org.example.nbcheckinservice.repository.MoodLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing mood logs
 * Path: mental-health-service/src/main/java/com/neuralbalance/mentalhealth/service/MoodLogService.java
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoodLogService {

    private final MoodLogRepository moodLogRepository;

    /**
     * Create a new mood log
     */
    @Transactional
    public MoodLogResponse createMoodLog(Long userId, MoodLogRequest request) {
        log.info("Creating mood log for user {}", userId);

        MoodLog moodLog = MoodLog.builder()
                .userId(userId)
                .logTimestamp(request.getLogTimestamp() != null
                        ? request.getLogTimestamp()
                        : LocalDateTime.now())
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

        return mapToResponse(savedLog);
    }

    /**
     * Get mood log by ID
     */
    @Transactional(readOnly = true)
    public MoodLogResponse getMoodLog(Long id) {
        log.debug("Fetching mood log {}", id);

        MoodLog moodLog = moodLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mood log not found: " + id));

        return mapToResponse(moodLog);
    }

    /**
     * Get all mood logs for user
     */
    @Transactional(readOnly = true)
    public List<MoodLogResponse> getAllMoodLogs(Long userId) {
        log.debug("Fetching all mood logs for user {}", userId);

        List<MoodLog> logs = moodLogRepository.findByUserIdOrderByLogTimestampDesc(userId);

        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get recent mood logs (last 20)
     */
    @Transactional(readOnly = true)
    public List<MoodLogResponse> getRecentMoodLogs(Long userId) {
        log.debug("Fetching recent mood logs for user {}", userId);

        List<MoodLog> logs = moodLogRepository.findTop20ByUserIdOrderByLogTimestampDesc(userId);

        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get mood logs within time range
     */
    @Transactional(readOnly = true)
    public List<MoodLogResponse> getMoodLogsInRange(
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        log.debug("Fetching mood logs for user {} from {} to {}", userId, startTime, endTime);

        List<MoodLog> logs = moodLogRepository
                .findByUserIdAndLogTimestampBetweenOrderByLogTimestampDesc(
                        userId, startTime, endTime
                );

        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get today's mood logs
     */
    @Transactional(readOnly = true)
    public List<MoodLogResponse> getTodayMoodLogs(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        return getMoodLogsInRange(userId, startOfDay, endOfDay);
    }

    /**
     * Update mood log
     */
    @Transactional
    public MoodLogResponse updateMoodLog(Long id, MoodLogRequest request) {
        log.info("Updating mood log {}", id);

        MoodLog moodLog = moodLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mood log not found: " + id));

        // Update fields
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
        log.info("Mood log {} updated", id);

        return mapToResponse(updatedLog);
    }

    /**
     * Delete mood log
     */
    @Transactional
    public void deleteMoodLog(Long id) {
        log.info("Deleting mood log {}", id);

        if (!moodLogRepository.existsById(id)) {
            throw new IllegalArgumentException("Mood log not found: " + id);
        }

        moodLogRepository.deleteById(id);
        log.info("Mood log {} deleted", id);
    }

    /**
     * Get average mood in time range
     */
    @Transactional(readOnly = true)
    public Double getAverageMood(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        Double avg = moodLogRepository.getAverageMood(userId, startTime, endTime);
        return avg != null ? avg : 0.0;
    }

    /**
     * Get mood logs by trigger
     */
    @Transactional(readOnly = true)
    public List<MoodLogResponse> getMoodLogsByTrigger(Long userId, String trigger) {
        log.debug("Fetching mood logs with trigger '{}' for user {}", trigger, userId);

        List<MoodLog> logs = moodLogRepository.findByUserIdAndTrigger(userId, trigger);

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