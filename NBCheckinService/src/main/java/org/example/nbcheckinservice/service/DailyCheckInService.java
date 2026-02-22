package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.CheckInRequest;
import org.example.nbcheckinservice.dto.CheckInResponse;
import org.example.nbcheckinservice.entity.DailyCheckIn;
import org.example.nbcheckinservice.entity.UserStreak;
import org.example.nbcheckinservice.exception.CheckInAlreadyExistsException;
import org.example.nbcheckinservice.repository.DailyCheckInRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing daily check-ins
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyCheckInService {

    private final DailyCheckInRepository checkInRepository;
    private final StreakService streakService;

    /**
     * Create a new check-in for today
     */
    @Transactional
    public CheckInResponse createCheckIn(Long userId, CheckInRequest request) {
        LocalDate checkInDate = request.getCheckInDate() != null
                ? request.getCheckInDate()
                : LocalDate.now();

        log.info("Creating check-in for user {} on date {}", userId, checkInDate);

        // Check if check-in already exists
        if (checkInRepository.existsByUserIdAndCheckInDate(userId, checkInDate)) {
            throw new CheckInAlreadyExistsException(
                    "Check-in already exists for date: " + checkInDate
            );
        }

        // Create check-in entity
        DailyCheckIn checkIn = buildCheckInFromRequest(userId, request, checkInDate);

        // Save check-in
        DailyCheckIn savedCheckIn = checkInRepository.save(checkIn);
        log.info("Check-in created successfully with ID: {}", savedCheckIn.getId());

        // Update streak
        UserStreak streak = streakService.updateStreak(userId, checkInDate);
        log.info("Streak updated for user {}: current={}, longest={}",
                userId, streak.getCurrentStreak(), streak.getLongestStreak());

        // Convert to response
        return buildCheckInResponse(savedCheckIn, streak);
    }

    /**
     * Update existing check-in
     */
    @Transactional
    public CheckInResponse updateCheckIn(Long userId, LocalDate date, CheckInRequest request) {
        log.info("Updating check-in for user {} on date {}", userId, date);

        DailyCheckIn checkIn = checkInRepository
                .findByUserIdAndCheckInDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Check-in not found for date: " + date
                ));

        // Update fields
        updateCheckInFields(checkIn, request);
        DailyCheckIn updatedCheckIn = checkInRepository.save(checkIn);
        log.info("Check-in updated successfully");

        UserStreak streak = streakService.getOrCreateStreak(userId);
        return buildCheckInResponse(updatedCheckIn, streak);
    }

    /**
     * Get check-in for specific date
     */
    @Transactional(readOnly = true)
    public CheckInResponse getCheckIn(Long userId, LocalDate date) {
        log.debug("Fetching check-in for user {} on date {}", userId, date);

        DailyCheckIn checkIn = checkInRepository
                .findByUserIdAndCheckInDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Check-in not found for date: " + date
                ));

        UserStreak streak = streakService.getOrCreateStreak(userId);

        return buildCheckInResponse(checkIn, streak);
    }

    /**
     * Get check-in for today
     */
    @Transactional(readOnly = true)
    public CheckInResponse getTodayCheckIn(Long userId) {
        return getCheckIn(userId, LocalDate.now());
    }

    /**
     * Check if user has checked in today
     */
    @Transactional(readOnly = true)
    public boolean hasCheckedInToday(Long userId) {
        return checkInRepository.existsByUserIdAndCheckInDate(userId, LocalDate.now());
    }

    /**
     * Get all check-ins for user within date range
     */
    @Transactional(readOnly = true)
    public List<CheckInResponse> getCheckInsInRange(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        log.debug("Fetching check-ins for user {} from {} to {}",
                userId, startDate, endDate);

        List<DailyCheckIn> checkIns = checkInRepository
                .findByUserIdAndCheckInDateBetweenOrderByCheckInDateDesc(
                        userId, startDate, endDate
                );

        UserStreak streak = streakService.getOrCreateStreak(userId);

        return checkIns.stream()
                .map(checkIn -> buildCheckInResponse(checkIn, streak))
                .collect(Collectors.toList());
    }

    /**
     * Get recent check-ins (last 30 days)
     */
    @Transactional(readOnly = true)
    public List<CheckInResponse> getRecentCheckIns(Long userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        return getCheckInsInRange(userId, startDate, endDate);
    }

    /**
     * Delete check-in
     */
    @Transactional
    public void deleteCheckIn(Long userId, LocalDate date) {
        log.info("Deleting check-in for user {} on date {}", userId, date);

        DailyCheckIn checkIn = checkInRepository
                .findByUserIdAndCheckInDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Check-in not found for date: " + date
                ));

        checkInRepository.delete(checkIn);

        // Recalculate streak
        streakService.recalculateStreak(userId);

        log.info("Check-in deleted successfully");
    }

    /**
     * Mark cognitive game as played for today
     */
    @Transactional
    public void markCognitiveGamePlayed(Long userId, LocalDate date) {
        log.info("Marking cognitive game played for user {} on {}", userId, date);

        DailyCheckIn checkIn = checkInRepository
                .findByUserIdAndCheckInDate(userId, date)
                .orElse(null);

        if (checkIn != null) {
            checkIn.setPlayedCognitiveGameToday(true);
            checkIn.setCognitiveGameCount(checkIn.getCognitiveGameCount() + 1);
            checkInRepository.save(checkIn);
            log.info("Cognitive game marked as played");
        } else {
            log.warn("No check-in found for user {} on {}, cannot mark game played",
                    userId, date);
        }
    }

    // ========== HELPER METHODS ==========

    private DailyCheckIn buildCheckInFromRequest(
            Long userId,
            CheckInRequest request,
            LocalDate checkInDate
    ) {
        DailyCheckIn.DailyCheckInBuilder builder = DailyCheckIn.builder()
                .userId(userId)
                .checkInDate(checkInDate)
                .morningMood(request.getMorningMood())
                .eveningMood(request.getEveningMood())
                .sleepQuality(request.getSleepQuality())
                .sleepHours(request.getSleepHours())
                .sleepBedtime(request.getSleepBedtime())
                .sleepWaketime(request.getSleepWaketime())
                .energyLevel(request.getEnergyLevel())
                .stressLevel(request.getStressLevel())
                .physicalActivityMinutes(request.getPhysicalActivityMinutes())
                .physicalActivityType(request.getPhysicalActivityType())
                .didExercise(request.getDidExercise())
                .ateHealthy(request.getAteHealthy())
                .hadSocialInteraction(request.getHadSocialInteraction())
                .playedCognitiveGameToday(request.getPlayedCognitiveGameToday())
                .cognitiveGameCount(request.getCognitiveGameCount() != null
                        ? request.getCognitiveGameCount() : 0);

        DailyCheckIn checkIn = builder.build();

        // Set emojis based on mood values
        if (request.getMorningMood() != null) {
            checkIn.setMorningMoodEmoji(DailyCheckIn.getMoodEmoji(request.getMorningMood()));
        }
        if (request.getEveningMood() != null) {
            checkIn.setEveningMoodEmoji(DailyCheckIn.getMoodEmoji(request.getEveningMood()));
        }

        return checkIn;
    }

    private void updateCheckInFields(DailyCheckIn checkIn, CheckInRequest request) {
        if (request.getMorningMood() != null) {
            checkIn.setMorningMood(request.getMorningMood());
            checkIn.setMorningMoodEmoji(DailyCheckIn.getMoodEmoji(request.getMorningMood()));
        }
        if (request.getEveningMood() != null) {
            checkIn.setEveningMood(request.getEveningMood());
            checkIn.setEveningMoodEmoji(DailyCheckIn.getMoodEmoji(request.getEveningMood()));
        }
        if (request.getSleepQuality() != null) {
            checkIn.setSleepQuality(request.getSleepQuality());
        }
        if (request.getSleepHours() != null) {
            checkIn.setSleepHours(request.getSleepHours());
        }
        if (request.getSleepBedtime() != null) {
            checkIn.setSleepBedtime(request.getSleepBedtime());
        }
        if (request.getSleepWaketime() != null) {
            checkIn.setSleepWaketime(request.getSleepWaketime());
        }
        if (request.getEnergyLevel() != null) {
            checkIn.setEnergyLevel(request.getEnergyLevel());
        }
        if (request.getStressLevel() != null) {
            checkIn.setStressLevel(request.getStressLevel());
        }
        if (request.getPhysicalActivityMinutes() != null) {
            checkIn.setPhysicalActivityMinutes(request.getPhysicalActivityMinutes());
        }
        if (request.getPhysicalActivityType() != null) {
            checkIn.setPhysicalActivityType(request.getPhysicalActivityType());
        }
        if (request.getDidExercise() != null) {
            checkIn.setDidExercise(request.getDidExercise());
        }
        if (request.getAteHealthy() != null) {
            checkIn.setAteHealthy(request.getAteHealthy());
        }
        if (request.getHadSocialInteraction() != null) {
            checkIn.setHadSocialInteraction(request.getHadSocialInteraction());
        }
        if (request.getPlayedCognitiveGameToday() != null) {
            checkIn.setPlayedCognitiveGameToday(request.getPlayedCognitiveGameToday());
        }
        if (request.getCognitiveGameCount() != null) {
            checkIn.setCognitiveGameCount(request.getCognitiveGameCount());
        }
    }

    private CheckInResponse buildCheckInResponse(DailyCheckIn checkIn, UserStreak streak) {
        CheckInResponse.CheckInResponseBuilder builder = CheckInResponse.builder()
                .id(checkIn.getId())
                .userId(checkIn.getUserId())
                .checkInDate(checkIn.getCheckInDate())
                .morningMood(checkIn.getMorningMood())
                .eveningMood(checkIn.getEveningMood())
                .morningMoodEmoji(checkIn.getMorningMoodEmoji())
                .eveningMoodEmoji(checkIn.getEveningMoodEmoji())
                .sleepQuality(checkIn.getSleepQuality())
                .sleepHours(checkIn.getSleepHours())
                .sleepBedtime(checkIn.getSleepBedtime())
                .sleepWaketime(checkIn.getSleepWaketime())
                .energyLevel(checkIn.getEnergyLevel())
                .stressLevel(checkIn.getStressLevel())
                .physicalActivityMinutes(checkIn.getPhysicalActivityMinutes())
                .physicalActivityType(checkIn.getPhysicalActivityType())
                .didExercise(checkIn.getDidExercise())
                .ateHealthy(checkIn.getAteHealthy())
                .hadSocialInteraction(checkIn.getHadSocialInteraction())
                .playedCognitiveGameToday(checkIn.getPlayedCognitiveGameToday())
                .cognitiveGameCount(checkIn.getCognitiveGameCount())
                .wellnessScore(checkIn.calculateWellnessScore())
                .isComplete(checkIn.isComplete())
                .createdAt(checkIn.getCreatedAt())
                .updatedAt(checkIn.getUpdatedAt());

        // Add streak info if this is today's check-in
        if (checkIn.getCheckInDate().equals(LocalDate.now())) {
            int bonusXp = streak.calculateStreakBonusXP();

            builder.streakInfo(CheckInResponse.StreakInfo.builder()
                    .currentStreak(streak.getCurrentStreak())
                    .longestStreak(streak.getLongestStreak())
                    .xpEarned(10) // Base XP per check-in
                    .bonusXp(bonusXp)
                    .isMilestone(streak.isMilestoneDay())
                    .message(streak.getStreakStatusMessage())
                    .nextMilestone(streak.getNextMilestone())
                    .build());
        }

        return builder.build();
    }
}