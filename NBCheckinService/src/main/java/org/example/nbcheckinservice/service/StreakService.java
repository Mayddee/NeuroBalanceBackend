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

    private static final int BASE_XP_PER_CHECKIN = 10;

    /**
     * Get or create streak for user
     */
    @Transactional
    public UserStreak getOrCreateStreak(Long userId) {
        return streakRepository.findByUserId(userId)
                .orElseGet(() -> createNewStreak(userId));
    }

    /**
     * Update streak after check-in
     */
    @Transactional
    public UserStreak updateStreak(Long userId, LocalDate checkInDate) {
        log.info("Updating streak for user {} on date {}", userId, checkInDate);

        UserStreak streak = getOrCreateStreak(userId);

        // Calculate days since last check-in
        LocalDate lastCheckIn = streak.getLastCheckinDate();

        if (lastCheckIn == null) {
            // First check-in ever
            streak.setCurrentStreak(1);
            streak.setLongestStreak(1);
            streak.setTotalCheckins(1);
            streak.setLastCheckinDate(checkInDate);

            int xpEarned = BASE_XP_PER_CHECKIN;
            streak.setTotalXpEarned(streak.getTotalXpEarned() + xpEarned);

            log.info("First check-in for user {}, streak started", userId);
        } else {
            long daysSinceLastCheckIn = ChronoUnit.DAYS.between(lastCheckIn, checkInDate);

            if (daysSinceLastCheckIn == 0) {
                // Same day, don't update streak
                log.warn("Check-in on same day for user {}, streak not updated", userId);
                return streak;
            } else if (daysSinceLastCheckIn == 1) {
                // Consecutive day! Increase streak
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                streak.setTotalCheckins(streak.getTotalCheckins() + 1);
                streak.setLastCheckinDate(checkInDate);

                // Update longest streak if current is higher
                if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                    streak.setLongestStreak(streak.getCurrentStreak());
                    log.info("New longest streak for user {}: {}",
                            userId, streak.getCurrentStreak());
                }

                // Calculate XP with bonus
                int baseXp = BASE_XP_PER_CHECKIN;
                int bonusXp = streak.calculateStreakBonusXP();
                int totalXp = baseXp + bonusXp;

                streak.setTotalXpEarned(streak.getTotalXpEarned() + totalXp);

                log.info("Streak continued for user {}: {} days, +{} XP",
                        userId, streak.getCurrentStreak(), totalXp);

                if (bonusXp > 0) {
                    log.info("🎉 Milestone bonus! User {} earned {} bonus XP", userId, bonusXp);
                }
            } else {
                // Streak broken! Reset to 1
                log.warn("Streak broken for user {}! Was {} days, resetting to 1",
                        userId, streak.getCurrentStreak());

                streak.setCurrentStreak(1);
                streak.setTotalCheckins(streak.getTotalCheckins() + 1);
                streak.setLastCheckinDate(checkInDate);

                int xpEarned = BASE_XP_PER_CHECKIN;
                streak.setTotalXpEarned(streak.getTotalXpEarned() + xpEarned);
            }
        }

        UserStreak savedStreak = streakRepository.save(streak);
        log.info("Streak updated successfully for user {}", userId);

        return savedStreak;
    }

    /**
     * Get streak response for user
     */
    @Transactional(readOnly = true)
    public StreakResponse getStreakResponse(Long userId) {
        UserStreak streak = getOrCreateStreak(userId);

        boolean canCheckinToday = !checkInRepository.existsByUserIdAndCheckInDate(
                userId, LocalDate.now()
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

    /**
     * Recalculate streak from scratch (useful after deletions)
     */
    @Transactional
    public UserStreak recalculateStreak(Long userId) {
        log.info("Recalculating streak for user {}", userId);

        UserStreak streak = getOrCreateStreak(userId);

        // Get all check-ins ordered by date
        List<DailyCheckIn> checkIns = checkInRepository
                .findByUserIdOrderByCheckInDateDesc(userId);

        if (checkIns.isEmpty()) {
            // No check-ins, reset streak
            streak.setCurrentStreak(0);
            streak.setTotalCheckins(0);
            streak.setLastCheckinDate(null);
            streak.setTotalXpEarned(0);
            return streakRepository.save(streak);
        }

        // Calculate current streak
        int currentStreak = 0;
        int longestStreak = 0;
        int totalXp = 0;
        LocalDate previousDate = null;

        // Reverse to go chronologically
        for (int i = checkIns.size() - 1; i >= 0; i--) {
            DailyCheckIn checkIn = checkIns.get(i);
            LocalDate checkInDate = checkIn.getCheckInDate();

            if (previousDate == null) {
                // First check-in
                currentStreak = 1;
                totalXp += BASE_XP_PER_CHECKIN;
            } else {
                long daysDiff = ChronoUnit.DAYS.between(previousDate, checkInDate);

                if (daysDiff == 1) {
                    // Consecutive day
                    currentStreak++;

                    // Calculate milestone bonus
                    int bonusXp = calculateMilestoneBonus(currentStreak);
                    totalXp += BASE_XP_PER_CHECKIN + bonusXp;
                } else if (daysDiff > 1) {
                    // Streak broken, start new one
                    if (currentStreak > longestStreak) {
                        longestStreak = currentStreak;
                    }
                    currentStreak = 1;
                    totalXp += BASE_XP_PER_CHECKIN;
                }
                // daysDiff == 0 means same day, skip
            }

            previousDate = checkInDate;
        }

        // Final longest streak check
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }

        // Update streak
        streak.setCurrentStreak(currentStreak);
        streak.setLongestStreak(longestStreak);
        streak.setTotalCheckins(checkIns.size());
        streak.setLastCheckinDate(checkIns.get(0).getCheckInDate());
        streak.setTotalXpEarned(totalXp);

        log.info("Recalculated streak for user {}: current={}, longest={}, totalXP={}",
                userId, currentStreak, longestStreak, totalXp);

        return streakRepository.save(streak);
    }

    /**
     * Get leaderboard by current streak
     */
    @Transactional(readOnly = true)
    public List<UserStreak> getTopStreaks(int limit) {
        return streakRepository.findTop10ByOrderByCurrentStreakDesc();
    }

    /**
     * Get leaderboard by total XP
     */
    @Transactional(readOnly = true)
    public List<UserStreak> getTopXP(int limit) {
        return streakRepository.findTop10ByOrderByTotalXpEarnedDesc();
    }

    /**
     * Get user's rank by streak
     */
    @Transactional(readOnly = true)
    public Long getUserRankByStreak(Long userId) {
        return streakRepository.getUserRankByCurrentStreak(userId);
    }

    /**
     * Get user's rank by XP
     */
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

    private int calculateMilestoneBonus(int streak) {
        return switch (streak) {
            case 7 -> 50;
            case 14 -> 100;
            case 30 -> 300;
            case 100 -> 1000;
            default -> 0;
        };
    }
}