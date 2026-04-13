package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.StreakResponse;
import org.example.nbcheckinservice.entity.DailyCheckIn;
import org.example.nbcheckinservice.entity.UserStreak;
import org.example.nbcheckinservice.repository.DailyCheckInRepository;
import org.example.nbcheckinservice.repository.UserStreakRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for managing user streaks and XP
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {
    private final UserStreakRepository streakRepository;
    private final DailyCheckInRepository checkInRepository;
    private final RewardService rewardService;

    private static final int BASE_XP_PER_CHECKIN = 10;
    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    @Transactional
    public UserStreak getOrCreateStreak(Long userId) {
        return streakRepository.findByUserId(userId)
                .orElseGet(() -> createNewStreak(userId));
    }

    @Transactional
    public UserStreak updateStreak(Long userId, LocalDate checkInDate) {
        // ВАЖНО: Убедись, что checkInDate передается как LocalDate.now(ZoneId.of("Asia/Almaty"))
        // из вызывающего сервиса (например, из DailyCheckInService)
        log.info("Updating streak for user {} on date {}", userId, checkInDate);

        UserStreak streak = getOrCreateStreak(userId);
        LocalDate lastCheckIn = streak.getLastCheckinDate();

        if (lastCheckIn == null) {
            // Первый чекин в жизни пользователя
            streak.setCurrentStreak(1);
            streak.setLongestStreak(1);
            streak.setTotalCheckins(1);
            streak.setLastCheckinDate(checkInDate);
            streak.setTotalXpEarned(streak.getTotalXpEarned() + BASE_XP_PER_CHECKIN);
        } else {
            long daysSinceLastCheckIn = ChronoUnit.DAYS.between(lastCheckIn, checkInDate);

            if (daysSinceLastCheckIn == 0) {
                // Если юзер чекинится второй раз за тот же день — ничего не делаем
                log.info("User {} already checked in today. No streak update.", userId);
                return streak;
            } else if (daysSinceLastCheckIn == 1) {
                // Идеально: чекин на следующий день (стрик продолжается)
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                streak.setTotalCheckins(streak.getTotalCheckins() + 1);
                streak.setLastCheckinDate(checkInDate);

                if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                    streak.setLongestStreak(streak.getCurrentStreak());
                }

                // АВТОМАТИКА БАЛЛОВ: Базовые 10 + бонус (50, 100, 300) если сегодня юбилейный день
                int totalXp = BASE_XP_PER_CHECKIN + streak.calculateStreakBonusXP();
                streak.setTotalXpEarned(streak.getTotalXpEarned() + totalXp);

            } else {
                // Стрик прерван (прошло 2 дня или больше) — сбрасываем на 1
                log.warn("Streak broken for user {}! Resetting to 1.", userId);
                streak.setCurrentStreak(1);
                streak.setTotalCheckins(streak.getTotalCheckins() + 1);
                streak.setLastCheckinDate(checkInDate);
                streak.setTotalXpEarned(streak.getTotalXpEarned() + BASE_XP_PER_CHECKIN);
            }
        }

        UserStreak savedStreak = streakRepository.save(streak);

        // 🔥 ГЛАВНАЯ АВТОМАТИКА: Вызываем RewardService.
        // Он проверит новый стрейк и, если юзер достиг 7/14/30 дней,
        // САМ разблокирует соответствующий Badge (Reward) в базе.
        rewardService.checkAndUnlockRewards(userId);

        return savedStreak;
    }

    @Transactional(readOnly = true)
    public StreakResponse getStreakResponse(Long userId) {
        UserStreak streak = getOrCreateStreak(userId);

        // Исправлено время на Алматы
        boolean canCheckinToday = !checkInRepository.existsByUserIdAndCheckInDate(
                userId, LocalDate.now(ALMATY_ZONE)
        );

        return StreakResponse.builder()
                .userId(userId)
                .currentStreak(streak.getCurrentStreak())
                .longestStreak(streak.getLongestStreak())
                .totalCheckins(streak.getTotalCheckins())
                .totalXpEarned(streak.getTotalXpEarned())
                .lastCheckinDate(streak.getLastCheckinDate())
                .canCheckinToday(canCheckinToday)
                .statusMessage(streak.getStreakStatusMessage())
                .nextMilestone(streak.getNextMilestone())
                .isMilestoneDay(streak.isMilestoneDay())
                .milestoneBonus(streak.calculateStreakBonusXP())
                .build();
    }
    @Transactional
    public UserStreak recalculateStreak(Long userId) {
        log.info("Recalculating streak for user {}", userId);
        UserStreak streak = getOrCreateStreak(userId);

        List<DailyCheckIn> checkIns = checkInRepository.findByUserIdOrderByCheckInDateAsc(userId);

        if (checkIns.isEmpty()) {
            streak.setCurrentStreak(0);
            streak.setLastCheckinDate(null);
            streak.setTotalCheckins(0);
            return streakRepository.save(streak);
        }

        int currentStreak = 0;
        int longestStreak = 0;
        LocalDate lastDate = null;

        for (DailyCheckIn checkIn : checkIns) {
            LocalDate date = checkIn.getCheckInDate();
            if (lastDate == null) {
                currentStreak = 1;
            } else {
                long daysDiff = ChronoUnit.DAYS.between(lastDate, date);
                if (daysDiff == 1) {
                    currentStreak++;
                } else if (daysDiff > 1) {
                    currentStreak = 1;
                }
            }
            if (currentStreak > longestStreak) longestStreak = currentStreak;
            lastDate = date;
        }

        streak.setCurrentStreak(currentStreak);
        streak.setLongestStreak(longestStreak);
        streak.setTotalCheckins(checkIns.size());
        streak.setLastCheckinDate(lastDate);

        return streakRepository.save(streak);
    }

    @Transactional(readOnly = true)
    public List<UserStreak> getTopStreaks(int limit) {
        return streakRepository.findTop10ByOrderByCurrentStreakDesc();
    }

    @Transactional(readOnly = true)
    public List<UserStreak> getTopXP(int limit) {
        return streakRepository.findTop10ByOrderByTotalXpEarnedDesc();
    }

    @Transactional(readOnly = true)
    public Long getUserRankByStreak(Long userId) {
        return streakRepository.getUserRankByCurrentStreak(userId);
    }

    @Transactional(readOnly = true)
    public Long getUserRankByXP(Long userId) {
        return streakRepository.getUserRankByTotalXp(userId);
    }

    // ========== HELPER METHODS ==========

    private UserStreak createNewStreak(Long userId) {
        log.info("Creating new streak for user {}", userId);

        UserStreak streak = UserStreak.builder()
                .userId(userId)
                .currentStreak(0)
                .longestStreak(0)
                .totalCheckins(0)
                .totalXpEarned(0)
                .build();

        return streakRepository.save(streak);
    }
}