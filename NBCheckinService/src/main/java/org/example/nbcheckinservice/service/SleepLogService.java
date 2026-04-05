package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.SleepLogRequest;
import org.example.nbcheckinservice.dto.SleepLogResponse;
import org.example.nbcheckinservice.entity.SleepLog;
import org.example.nbcheckinservice.repository.SleepLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing sleep logs
 * ✅ FIXED: Proper LocalTime handling and auto-calculation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SleepLogService {

    private final SleepLogRepository sleepLogRepository;

    @Transactional
    public SleepLogResponse createSleepLog(Long userId, SleepLogRequest request) {
        log.info("Creating sleep log for user {} on date {}", userId, request.getSleepDate());

        if (sleepLogRepository.existsByUserIdAndSleepDate(userId, request.getSleepDate())) {
            throw new IllegalArgumentException(
                    "Sleep log for date " + request.getSleepDate() + " already exists. Use PUT to update."
            );
        }

        SleepLog sleepLog = buildSleepLogFromRequest(userId, request);
        calculateDerivedFields(sleepLog);

        SleepLog savedLog = sleepLogRepository.save(sleepLog);
        log.info("Sleep log created with ID: {}", savedLog.getId());

        return mapToResponse(savedLog);
    }

    @Transactional(readOnly = true)
    public SleepLogResponse getSleepLog(Long userId, Long id) {
        log.debug("Fetching sleep log {} for user {}", id, userId);

        SleepLog sleepLog = sleepLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sleep log not found or access denied"
                ));

        return mapToResponse(sleepLog);
    }

    @Transactional(readOnly = true)
    public SleepLogResponse getSleepLogByDate(Long userId, LocalDate date) {
        log.debug("Fetching sleep log for user {} on date {}", userId, date);

        SleepLog sleepLog = sleepLogRepository.findByUserIdAndSleepDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sleep log not found for date " + date
                ));

        return mapToResponse(sleepLog);
    }

    @Transactional(readOnly = true)
    public List<SleepLogResponse> getAllSleepLogs(Long userId) {
        log.debug("Fetching all sleep logs for user {}", userId);
        List<SleepLog> logs = sleepLogRepository.findByUserIdOrderBySleepDateDesc(userId);
        return logs.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SleepLogResponse> getRecentSleepLogs(Long userId) {
        log.debug("Fetching recent sleep logs for user {}", userId);
        List<SleepLog> logs = sleepLogRepository.findTop30ByUserIdOrderBySleepDateDesc(userId);
        return logs.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SleepLogResponse getTodaySleepLog(Long userId) {
        LocalDate today = LocalDate.now();
        return getSleepLogByDate(userId, today);
    }

    @Transactional(readOnly = true)
    public boolean hasSleepLogToday(Long userId) {
        return sleepLogRepository.existsByUserIdAndSleepDate(userId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<SleepLogResponse> getSleepLogsInRange(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        log.debug("Fetching sleep logs for user {} from {} to {}", userId, startDate, endDate);
        List<SleepLog> logs = sleepLogRepository.findByUserIdAndSleepDateBetweenOrderBySleepDateDesc(
                userId, startDate, endDate
        );
        return logs.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public SleepLogResponse updateSleepLog(Long userId, Long id, SleepLogRequest request) {
        log.info("Updating sleep log {} for user {}", id, userId);

        SleepLog sleepLog = sleepLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sleep log not found or access denied"
                ));

        updateSleepLogFromRequest(sleepLog, request);
        calculateDerivedFields(sleepLog);

        SleepLog updatedLog = sleepLogRepository.save(sleepLog);
        log.info("Sleep log {} updated successfully", id);

        return mapToResponse(updatedLog);
    }

    @Transactional
    public SleepLogResponse updateSleepLogByDate(Long userId, LocalDate date, SleepLogRequest request) {
        log.info("Updating sleep log for user {} on date {}", userId, date);

        SleepLog sleepLog = sleepLogRepository.findByUserIdAndSleepDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sleep log not found for date " + date
                ));

        updateSleepLogFromRequest(sleepLog, request);
        calculateDerivedFields(sleepLog);

        SleepLog updatedLog = sleepLogRepository.save(sleepLog);
        log.info("Sleep log updated successfully for date {}", date);

        return mapToResponse(updatedLog);
    }

    @Transactional
    public void deleteSleepLog(Long userId, Long id) {
        log.info("Deleting sleep log {} for user {}", id, userId);

        SleepLog sleepLog = sleepLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sleep log not found or access denied"
                ));

        sleepLogRepository.delete(sleepLog);
        log.info("Sleep log {} deleted successfully", id);
    }

    @Transactional
    public void deleteSleepLogByDate(Long userId, LocalDate date) {
        log.info("Deleting sleep log for user {} on date {}", userId, date);

        SleepLog sleepLog = sleepLogRepository.findByUserIdAndSleepDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sleep log not found for date " + date
                ));

        sleepLogRepository.delete(sleepLog);
        log.info("Sleep log deleted successfully for date {}", date);
    }

    // ========== HELPER METHODS ==========

    private SleepLog buildSleepLogFromRequest(Long userId, SleepLogRequest request) {
        return SleepLog.builder()
                .userId(userId)
                .sleepDate(request.getSleepDate())
                .bedtime(request.getBedtime())
                .wakeTime(request.getWakeTime())
                .fellAsleepTime(request.getFellAsleepTime())
                .totalHours(request.getTotalHours() != null ?
                        java.math.BigDecimal.valueOf(request.getTotalHours()) : null)
                .actualSleepHours(request.getActualSleepHours() != null ?
                        java.math.BigDecimal.valueOf(request.getActualSleepHours()) : null)
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

    private void updateSleepLogFromRequest(SleepLog sleepLog, SleepLogRequest request) {
        if (request.getSleepDate() != null) sleepLog.setSleepDate(request.getSleepDate());
        if (request.getBedtime() != null) sleepLog.setBedtime(request.getBedtime());
        if (request.getWakeTime() != null) sleepLog.setWakeTime(request.getWakeTime());
        if (request.getFellAsleepTime() != null) sleepLog.setFellAsleepTime(request.getFellAsleepTime());
        if (request.getTotalHours() != null) sleepLog.setTotalHours(java.math.BigDecimal.valueOf(request.getTotalHours()));
        if (request.getActualSleepHours() != null) sleepLog.setActualSleepHours(java.math.BigDecimal.valueOf(request.getActualSleepHours()));
        if (request.getTimeToFallAsleepMinutes() != null) sleepLog.setTimeToFallAsleepMinutes(request.getTimeToFallAsleepMinutes());
        if (request.getQualityScore() != null) sleepLog.setQualityScore(request.getQualityScore());
        if (request.getFeltRested() != null) sleepLog.setFeltRested(request.getFeltRested());
        if (request.getInterruptionsCount() != null) sleepLog.setInterruptionsCount(request.getInterruptionsCount());
        if (request.getAwakeDurationMinutes() != null) sleepLog.setAwakeDurationMinutes(request.getAwakeDurationMinutes());
        if (request.getBathroomTrips() != null) sleepLog.setBathroomTrips(request.getBathroomTrips());
        if (request.getDeepSleepMinutes() != null) sleepLog.setDeepSleepMinutes(request.getDeepSleepMinutes());
        if (request.getLightSleepMinutes() != null) sleepLog.setLightSleepMinutes(request.getLightSleepMinutes());
        if (request.getRemSleepMinutes() != null) sleepLog.setRemSleepMinutes(request.getRemSleepMinutes());
        if (request.getAwakeMinutes() != null) sleepLog.setAwakeMinutes(request.getAwakeMinutes());
        if (request.getHadDreams() != null) sleepLog.setHadDreams(request.getHadDreams());
        if (request.getDreamRecall() != null) sleepLog.setDreamRecall(request.getDreamRecall());
        if (request.getDreamNotes() != null) sleepLog.setDreamNotes(request.getDreamNotes());
        if (request.getNightmares() != null) sleepLog.setNightmares(request.getNightmares());
        if (request.getRoomTemperature() != null) sleepLog.setRoomTemperature(request.getRoomTemperature());
        if (request.getNoiseLevel() != null) sleepLog.setNoiseLevel(request.getNoiseLevel());
        if (request.getLightLevel() != null) sleepLog.setLightLevel(request.getLightLevel());
        if (request.getBedComfort() != null) sleepLog.setBedComfort(request.getBedComfort());
        if (request.getCaffeineBeforeBed() != null) sleepLog.setCaffeineBeforeBed(request.getCaffeineBeforeBed());
        if (request.getScreenTimeBeforeBedMinutes() != null) sleepLog.setScreenTimeBeforeBedMinutes(request.getScreenTimeBeforeBedMinutes());
        if (request.getExerciseBeforeBed() != null) sleepLog.setExerciseBeforeBed(request.getExerciseBeforeBed());
        if (request.getAlcohol() != null) sleepLog.setAlcohol(request.getAlcohol());
        if (request.getHeavyMeal() != null) sleepLog.setHeavyMeal(request.getHeavyMeal());
        if (request.getMorningMood() != null) sleepLog.setMorningMood(request.getMorningMood());
        if (request.getMorningEnergy() != null) sleepLog.setMorningEnergy(request.getMorningEnergy());
        if (request.getNotes() != null) sleepLog.setNotes(request.getNotes());
    }

    /**
     * ✅ CRITICAL: Auto-calculate derived fields
     * NOTE: Entity uses @PrePersist/@PreUpdate to calculate totalHours and sleepEfficiency
     * This method handles timeToFallAsleepMinutes calculation
     */
    private void calculateDerivedFields(SleepLog sleepLog) {
        // Auto-calculate timeToFallAsleepMinutes if not provided
        if (sleepLog.getTimeToFallAsleepMinutes() == null
                && sleepLog.getBedtime() != null
                && sleepLog.getFellAsleepTime() != null) {

            LocalTime bedtime = sleepLog.getBedtime();
            LocalTime fellAsleep = sleepLog.getFellAsleepTime();

            // Handle overnight case (fell asleep after midnight)
            long minutes;
            if (fellAsleep.isBefore(bedtime)) {
                // Overnight: 23:30 -> 00:15 next day
                minutes = Duration.between(bedtime, LocalTime.MAX).toMinutes() + 1 +
                        Duration.between(LocalTime.MIN, fellAsleep).toMinutes();
            } else {
                // Same day
                minutes = Duration.between(bedtime, fellAsleep).toMinutes();
            }

            sleepLog.setTimeToFallAsleepMinutes((int) minutes);
        }

        // Note: totalHours and sleepEfficiency are calculated by Entity's @PrePersist/@PreUpdate
    }

    private SleepLogResponse mapToResponse(SleepLog sleepLog) {
        return SleepLogResponse.builder()
                .id(sleepLog.getId())
                .userId(sleepLog.getUserId())
                .sleepDate(sleepLog.getSleepDate())
                .bedtime(sleepLog.getBedtime())
                .wakeTime(sleepLog.getWakeTime())
                .fellAsleepTime(sleepLog.getFellAsleepTime())
                .totalHours(sleepLog.getTotalHours() != null ? sleepLog.getTotalHours().doubleValue() : null)
                .actualSleepHours(sleepLog.getActualSleepHours() != null ? sleepLog.getActualSleepHours().doubleValue() : null)
                .timeToFallAsleepMinutes(sleepLog.getTimeToFallAsleepMinutes())
                .sleepEfficiency(sleepLog.getSleepEfficiency() != null ? sleepLog.getSleepEfficiency().doubleValue() : null)
                .qualityScore(sleepLog.getQualityScore())
                .feltRested(sleepLog.getFeltRested())
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