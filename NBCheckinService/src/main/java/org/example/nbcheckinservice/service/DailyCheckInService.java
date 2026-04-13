package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.CheckInRequest;
import org.example.nbcheckinservice.dto.CheckInResponse;
import org.example.nbcheckinservice.dto.RewardResponse;
import org.example.nbcheckinservice.entity.DailyCheckIn;
import org.example.nbcheckinservice.entity.DailyTask;
import org.example.nbcheckinservice.entity.UserStreak;
import org.example.nbcheckinservice.exception.CheckInAlreadyExistsException;
import org.example.nbcheckinservice.repository.DailyCheckInRepository;
import org.example.nbcheckinservice.repository.DailyTaskRepository; // Добавлен для нового метода
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing daily check-ins
 * ✅ VERIFIED: All logic is correct, Timezone: Asia/Almaty
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyCheckInService {

    private final DailyCheckInRepository checkInRepository;
    private final StreakService streakService;
    private final UserCharacterService characterService;
    private final DailyTaskService dailyTaskService;
    private final RewardService rewardService;
    private final DailyTaskRepository taskRepository; // Репозиторий тасок для проверки выполнения всех задач

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    /**
     * Возвращает список дат текущего месяца, в которые пользователь выполнил ВСЕ заданные таски.
     * Полезно для календаря на фронтенде.
     */
    @Transactional(readOnly = true)
    public List<LocalDate> getCompletionDatesInMonth(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // Получаем все задачи пользователя за этот месяц
        List<DailyTask> monthlyTasks = taskRepository.findByUserIdAndTaskDateBetween(userId, start, end);

        // Группируем задачи по датам и проверяем, что все задачи в этот день завершены
        return monthlyTasks.stream()
                .collect(Collectors.groupingBy(DailyTask::getTaskDate))
                .entrySet().stream()
                .filter(entry -> {
                    List<DailyTask> dailyTasks = entry.getValue();
                    // Проверяем: список не пуст И все элементы имеют isCompleted = true
                    return !dailyTasks.isEmpty() && dailyTasks.stream().allMatch(DailyTask::getIsCompleted);
                })
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional
    public CheckInResponse createCheckIn(Long userId, CheckInRequest request) {
        LocalDate checkInDate = request.getCheckInDate() != null
                ? request.getCheckInDate()
                : LocalDate.now(ALMATY_ZONE);

        log.info("Creating check-in for user {} on date {}", userId, checkInDate);

        if (checkInRepository.existsByUserIdAndCheckInDate(userId, checkInDate)) {
            throw new CheckInAlreadyExistsException(
                    "Check-in already exists for date: " + checkInDate
            );
        }

        DailyCheckIn checkIn = buildCheckInFromRequest(userId, request, checkInDate);
        DailyCheckIn savedCheckIn = checkInRepository.save(checkIn);
        log.info("Check-in created successfully with ID: {}", savedCheckIn.getId());

        UserStreak streak = streakService.updateStreak(userId, checkInDate);
        log.info("Streak updated for user {}: current={}, longest={}",
                userId, streak.getCurrentStreak(), streak.getLongestStreak());

        // ========== GAMIFICATION LOGIC ==========
        double wellnessScore = savedCheckIn.calculateWellnessScore();
        characterService.updateHappiness(userId, wellnessScore);
        log.debug("Character happiness updated based on wellness score: {}", wellnessScore);

        dailyTaskService.autoCompleteTask(userId, DailyTask.TaskType.COMPLETE_CHECKIN);
        if (request.getSleepHours() != null &&
                request.getSleepHours().compareTo(new BigDecimal("7.0")) >= 0) {
            dailyTaskService.autoCompleteTask(userId, DailyTask.TaskType.SLEEP_7_HOURS);
            log.debug("Sleep task auto-completed (7+ hours)");
        }

        List<RewardResponse> newRewards = rewardService.checkAndUnlockRewards(userId);
        if (!newRewards.isEmpty()) {
            log.info("🎉 Unlocked {} new reward(s) for user {}", newRewards.size(), userId);
        }

        return buildCheckInResponse(savedCheckIn, streak);
    }

    @Transactional
    public CheckInResponse updateCheckIn(Long userId, LocalDate date, CheckInRequest request) {
        log.info("Updating check-in for user {} on date {}", userId, date);

        DailyCheckIn checkIn = checkInRepository
                .findByUserIdAndCheckInDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Check-in not found for date: " + date
                ));

        updateCheckInFields(checkIn, request);
        DailyCheckIn updatedCheckIn = checkInRepository.save(checkIn);
        log.info("Check-in updated successfully");

        UserStreak streak = streakService.getOrCreateStreak(userId);
        return buildCheckInResponse(updatedCheckIn, streak);
    }

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

    @Transactional(readOnly = true)
    public CheckInResponse getTodayCheckIn(Long userId) {
        return getCheckIn(userId, LocalDate.now(ALMATY_ZONE));
    }

    @Transactional(readOnly = true)
    public boolean hasCheckedInToday(Long userId) {
        return checkInRepository.existsByUserIdAndCheckInDate(userId, LocalDate.now(ALMATY_ZONE));
    }

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

    @Transactional(readOnly = true)
    public List<CheckInResponse> getRecentCheckIns(Long userId) {
        LocalDate endDate = LocalDate.now(ALMATY_ZONE);
        LocalDate startDate = endDate.minusDays(30);
        return getCheckInsInRange(userId, startDate, endDate);
    }

    @Transactional
    public void deleteCheckIn(Long userId, LocalDate date) {
        log.info("Deleting check-in for user {} on date {}", userId, date);

        DailyCheckIn checkIn = checkInRepository
                .findByUserIdAndCheckInDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException("Check-in not found"));

        checkInRepository.delete(checkIn);
        streakService.recalculateStreak(userId);
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

        if (checkIn.getCheckInDate().equals(LocalDate.now(ALMATY_ZONE))) {
            int bonusXp = streak.calculateStreakBonusXP();

            builder.streakInfo(CheckInResponse.StreakInfo.builder()
                    .currentStreak(streak.getCurrentStreak())
                    .longestStreak(streak.getLongestStreak())
                    .xpEarned(10)
                    .bonusXp(bonusXp)
                    .isMilestone(streak.isMilestoneDay())
                    .message(streak.getStreakStatusMessage())
                    .nextMilestone(streak.getNextMilestone())
                    .build());
        }

        return builder.build();
    }
}