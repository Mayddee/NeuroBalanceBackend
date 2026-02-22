package org.example.nbcheckinservice.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.SleepLogRequest;
import org.example.nbcheckinservice.dto.SleepLogResponse;
import org.example.nbcheckinservice.entity.SleepLog;
import org.example.nbcheckinservice.repository.SleepLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing sleep logs
 * Path: mental-health-service/src/main/java/com/neuralbalance/mentalhealth/service/SleepLogService.java
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SleepLogService {

    private final SleepLogRepository sleepLogRepository;

    /**
     * Create a new sleep log
     */
    @Transactional
    public SleepLogResponse createSleepLog(Long userId, SleepLogRequest request) {
        LocalDate sleepDate = request.getSleepDate() != null
                ? request.getSleepDate()
                : LocalDate.now();

        log.info("Creating sleep log for user {} on date {}", userId, sleepDate);

        // Check if sleep log already exists
        if (sleepLogRepository.existsByUserIdAndSleepDate(userId, sleepDate)) {
            throw new IllegalArgumentException(
                    "Sleep log already exists for date: " + sleepDate
            );
        }

        SleepLog sleepLog = buildSleepLogFromRequest(userId, request, sleepDate);

        SleepLog savedLog = sleepLogRepository.save(sleepLog);
        log.info("Sleep log created with ID: {}", savedLog.getId());

        return mapToResponse(savedLog);
    }

    /**
     * Get sleep log by ID
     */
    @Transactional(readOnly = true)
    public SleepLogResponse getSleepLog(Long id) {
        log.debug("Fetching sleep log {}", id);

        SleepLog sleepLog = sleepLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sleep log not found: " + id));

        return mapToResponse(sleepLog);
    }

    /**
     * Get sleep log by user and date
     */
    @Transactional(readOnly = true)
    public SleepLogResponse getSleepLogByDate(Long userId, LocalDate date) {
        log.debug("Fetching sleep log for user {} on date {}", userId, date);

        SleepLog sleepLog = sleepLogRepository.findByUserIdAndSleepDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sleep log not found for date: " + date
                ));

        return mapToResponse(sleepLog);
    }

    /**
     * Get today's sleep log
     */
    @Transactional(readOnly = true)
    public SleepLogResponse getTodaySleepLog(Long userId) {
        return getSleepLogByDate(userId, LocalDate.now());
    }

    /**
     * Check if sleep log exists for today
     */
    @Transactional(readOnly = true)
    public boolean hasSleepLogToday(Long userId) {
        return sleepLogRepository.existsByUserIdAndSleepDate(userId, LocalDate.now());
    }

    /**
     * Get all sleep logs for user
     */
    @Transactional(readOnly = true)
    public List<SleepLogResponse> getAllSleepLogs(Long userId) {
        log.debug("Fetching all sleep logs for user {}", userId);

        List<SleepLog> logs = sleepLogRepository.findByUserIdOrderBySleepDateDesc(userId);

        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get recent sleep logs (last 30 days)
     */
    @Transactional(readOnly = true)
    public List<SleepLogResponse> getRecentSleepLogs(Long userId) {
        log.debug("Fetching recent sleep logs for user {}", userId);

        List<SleepLog> logs = sleepLogRepository.findTop30ByUserIdOrderBySleepDateDesc(userId);

        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get sleep logs within date range
     */
    @Transactional(readOnly = true)
    public List<SleepLogResponse> getSleepLogsInRange(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        log.debug("Fetching sleep logs for user {} from {} to {}", userId, startDate, endDate);

        List<SleepLog> logs = sleepLogRepository
                .findByUserIdAndSleepDateBetweenOrderBySleepDateDesc(
                        userId, startDate, endDate
                );

        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update sleep log
     */
    @Transactional
    public SleepLogResponse updateSleepLog(Long id, SleepLogRequest request) {
        log.info("Updating sleep log {}", id);

        SleepLog sleepLog = sleepLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sleep log not found: " + id));

        updateSleepLogFields(sleepLog, request);

        SleepLog updatedLog = sleepLogRepository.save(sleepLog);
        log.info("Sleep log {} updated", id);

        return mapToResponse(updatedLog);
    }

    /**
     * Update sleep log by date
     */
    @Transactional
    public SleepLogResponse updateSleepLogByDate(
            Long userId,
            LocalDate date,
            SleepLogRequest request
    ) {
        log.info("Updating sleep log for user {} on date {}", userId, date);

        SleepLog sleepLog = sleepLogRepository.findByUserIdAndSleepDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sleep log not found for date: " + date
                ));

        updateSleepLogFields(sleepLog, request);

        SleepLog updatedLog = sleepLogRepository.save(sleepLog);
        log.info("Sleep log updated for date {}", date);

        return mapToResponse(updatedLog);
    }

    /**
     * Delete sleep log
     */
    @Transactional
    public void deleteSleepLog(Long id) {
        log.info("Deleting sleep log {}", id);

        if (!sleepLogRepository.existsById(id)) {
            throw new IllegalArgumentException("Sleep log not found: " + id);
        }

        sleepLogRepository.deleteById(id);
        log.info("Sleep log {} deleted", id);
    }

    /**
     * Delete sleep log by date
     */
    @Transactional
    public void deleteSleepLogByDate(Long userId, LocalDate date) {
        log.info("Deleting sleep log for user {} on date {}", userId, date);

        SleepLog sleepLog = sleepLogRepository.findByUserIdAndSleepDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sleep log not found for date: " + date
                ));

        sleepLogRepository.delete(sleepLog);
        log.info("Sleep log deleted for date {}", date);
    }

    // ========== HELPER METHODS ==========

    private SleepLog buildSleepLogFromRequest(Long userId, SleepLogRequest request, LocalDate sleepDate) {
        return SleepLog.builder()
                .userId(userId)
                .sleepDate(sleepDate)
                .bedtime(request.getBedtime())
                .wakeTime(request.getWakeTime())
                .fellAsleepTime(request.getFellAsleepTime())
                .totalHours(request.getTotalHours())
                .actualSleepHours(request.getActualSleepHours())
                .timeToFallAsleepMinutes(request.getTimeToFallAsleepMinutes())
                .qualityScore(request.getQualityScore())
                .feltRested(request.getFeltRested())
                .interruptionsCount(request.getInterruptionsCount())
                .awakeDurationMinutes(request.getAwakeDurationMinutes())
                .bathroomTrips(request.getBathroomTrips())
                .deepSleepMinutes(request.getDeepSleepMinutes())
                .lightSleepMinutes(request.getLightSleepMinutes())
                .remSleepMinutes(request.getRemSleepMinutes())
                .awakeMinutes(request.getAwakeMinutes())
                .hadDreams(request.getHadDreams())
                .dreamRecall(request.getDreamRecall())
                .dreamNotes(request.getDreamNotes())
                .nightmares(request.getNightmares())
                .roomTemperature(request.getRoomTemperature())
                .noiseLevel(request.getNoiseLevel())
                .lightLevel(request.getLightLevel())
                .bedComfort(request.getBedComfort())
                .caffeineBeforeBed(request.getCaffeineBeforeBed())
                .screenTimeBeforeBedMinutes(request.getScreenTimeBeforeBedMinutes())
                .exerciseBeforeBed(request.getExerciseBeforeBed())
                .alcohol(request.getAlcohol())
                .heavyMeal(request.getHeavyMeal())
                .morningMood(request.getMorningMood())
                .morningEnergy(request.getMorningEnergy())
                .notes(request.getNotes())
                .build();
    }

    private void updateSleepLogFields(SleepLog sleepLog, SleepLogRequest request) {
        if (request.getBedtime() != null) {
            sleepLog.setBedtime(request.getBedtime());
        }
        if (request.getWakeTime() != null) {
            sleepLog.setWakeTime(request.getWakeTime());
        }
        if (request.getFellAsleepTime() != null) {
            sleepLog.setFellAsleepTime(request.getFellAsleepTime());
        }
        if (request.getTotalHours() != null) {
            sleepLog.setTotalHours(request.getTotalHours());
        }
        if (request.getActualSleepHours() != null) {
            sleepLog.setActualSleepHours(request.getActualSleepHours());
        }
        if (request.getTimeToFallAsleepMinutes() != null) {
            sleepLog.setTimeToFallAsleepMinutes(request.getTimeToFallAsleepMinutes());
        }
        if (request.getQualityScore() != null) {
            sleepLog.setQualityScore(request.getQualityScore());
        }
        if (request.getFeltRested() != null) {
            sleepLog.setFeltRested(request.getFeltRested());
        }
        if (request.getInterruptionsCount() != null) {
            sleepLog.setInterruptionsCount(request.getInterruptionsCount());
        }
        if (request.getAwakeDurationMinutes() != null) {
            sleepLog.setAwakeDurationMinutes(request.getAwakeDurationMinutes());
        }
        if (request.getBathroomTrips() != null) {
            sleepLog.setBathroomTrips(request.getBathroomTrips());
        }
        if (request.getDeepSleepMinutes() != null) {
            sleepLog.setDeepSleepMinutes(request.getDeepSleepMinutes());
        }
        if (request.getLightSleepMinutes() != null) {
            sleepLog.setLightSleepMinutes(request.getLightSleepMinutes());
        }
        if (request.getRemSleepMinutes() != null) {
            sleepLog.setRemSleepMinutes(request.getRemSleepMinutes());
        }
        if (request.getAwakeMinutes() != null) {
            sleepLog.setAwakeMinutes(request.getAwakeMinutes());
        }
        if (request.getHadDreams() != null) {
            sleepLog.setHadDreams(request.getHadDreams());
        }
        if (request.getDreamRecall() != null) {
            sleepLog.setDreamRecall(request.getDreamRecall());
        }
        if (request.getDreamNotes() != null) {
            sleepLog.setDreamNotes(request.getDreamNotes());
        }
        if (request.getNightmares() != null) {
            sleepLog.setNightmares(request.getNightmares());
        }
        if (request.getRoomTemperature() != null) {
            sleepLog.setRoomTemperature(request.getRoomTemperature());
        }
        if (request.getNoiseLevel() != null) {
            sleepLog.setNoiseLevel(request.getNoiseLevel());
        }
        if (request.getLightLevel() != null) {
            sleepLog.setLightLevel(request.getLightLevel());
        }
        if (request.getBedComfort() != null) {
            sleepLog.setBedComfort(request.getBedComfort());
        }
        if (request.getCaffeineBeforeBed() != null) {
            sleepLog.setCaffeineBeforeBed(request.getCaffeineBeforeBed());
        }
        if (request.getScreenTimeBeforeBedMinutes() != null) {
            sleepLog.setScreenTimeBeforeBedMinutes(request.getScreenTimeBeforeBedMinutes());
        }
        if (request.getExerciseBeforeBed() != null) {
            sleepLog.setExerciseBeforeBed(request.getExerciseBeforeBed());
        }
        if (request.getAlcohol() != null) {
            sleepLog.setAlcohol(request.getAlcohol());
        }
        if (request.getHeavyMeal() != null) {
            sleepLog.setHeavyMeal(request.getHeavyMeal());
        }
        if (request.getMorningMood() != null) {
            sleepLog.setMorningMood(request.getMorningMood());
        }
        if (request.getMorningEnergy() != null) {
            sleepLog.setMorningEnergy(request.getMorningEnergy());
        }
        if (request.getNotes() != null) {
            sleepLog.setNotes(request.getNotes());
        }
    }

    private SleepLogResponse mapToResponse(SleepLog sleepLog) {
        return SleepLogResponse.builder()
                .id(sleepLog.getId())
                .userId(sleepLog.getUserId())
                .sleepDate(sleepLog.getSleepDate())
                .bedtime(sleepLog.getBedtime())
                .wakeTime(sleepLog.getWakeTime())
                .fellAsleepTime(sleepLog.getFellAsleepTime())
                .totalHours(sleepLog.getTotalHours())
                .actualSleepHours(sleepLog.getActualSleepHours())
                .timeToFallAsleepMinutes(sleepLog.getTimeToFallAsleepMinutes())
                .qualityScore(sleepLog.getQualityScore())
                .sleepEfficiency(sleepLog.getSleepEfficiency())
                .feltRested(sleepLog.getFeltRested())
                .sleepScore(sleepLog.calculateSleepScore())
                .interruptionsCount(sleepLog.getInterruptionsCount())
                .awakeDurationMinutes(sleepLog.getAwakeDurationMinutes())
                .bathroomTrips(sleepLog.getBathroomTrips())
                .deepSleepMinutes(sleepLog.getDeepSleepMinutes())
                .lightSleepMinutes(sleepLog.getLightSleepMinutes())
                .remSleepMinutes(sleepLog.getRemSleepMinutes())
                .awakeMinutes(sleepLog.getAwakeMinutes())
                .hadDreams(sleepLog.getHadDreams())
                .dreamRecall(sleepLog.getDreamRecall())
                .dreamNotes(sleepLog.getDreamNotes())
                .nightmares(sleepLog.getNightmares())
                .roomTemperature(sleepLog.getRoomTemperature())
                .noiseLevel(sleepLog.getNoiseLevel())
                .lightLevel(sleepLog.getLightLevel())
                .bedComfort(sleepLog.getBedComfort())
                .caffeineBeforeBed(sleepLog.getCaffeineBeforeBed())
                .screenTimeBeforeBedMinutes(sleepLog.getScreenTimeBeforeBedMinutes())
                .exerciseBeforeBed(sleepLog.getExerciseBeforeBed())
                .alcohol(sleepLog.getAlcohol())
                .heavyMeal(sleepLog.getHeavyMeal())
                .morningMood(sleepLog.getMorningMood())
                .morningEnergy(sleepLog.getMorningEnergy())
                .notes(sleepLog.getNotes())
                .createdAt(sleepLog.getCreatedAt())
                .updatedAt(sleepLog.getUpdatedAt())
                .build();
    }
}
