package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.CheckInStatsResponse;
import org.example.nbcheckinservice.entity.DailyCheckIn;
import org.example.nbcheckinservice.repository.DailyCheckInRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analytics and statistics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final DailyCheckInRepository checkInRepository;

    /**
     * Get comprehensive statistics for user in date range
     */
    @Transactional(readOnly = true)
    public CheckInStatsResponse getStats(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating stats for user {} from {} to {}", userId, startDate, endDate);

        List<DailyCheckIn> checkIns = checkInRepository
                .findByUserIdAndCheckInDateBetweenOrderByCheckInDateDesc(userId, startDate, endDate);

        if (checkIns.isEmpty()) {
            return buildEmptyStats(userId, startDate, endDate);
        }

        // Calculate averages
        Double avgMorningMood = calculateAverage(checkIns, DailyCheckIn::getMorningMood);
        Double avgEveningMood = calculateAverage(checkIns, DailyCheckIn::getEveningMood);
        Double avgSleepQuality = calculateAverage(checkIns, DailyCheckIn::getSleepQuality);
        Double avgSleepHours = calculateAverageBigDecimal(checkIns, DailyCheckIn::getSleepHours);
        Double avgEnergyLevel = calculateAverage(checkIns, DailyCheckIn::getEnergyLevel);
        Double avgStressLevel = calculateAverage(checkIns, DailyCheckIn::getStressLevel);
        Double avgWellnessScore = checkIns.stream()
                .mapToDouble(DailyCheckIn::calculateWellnessScore)
                .average()
                .orElse(0.0);

        int totalPhysicalActivityMinutes = checkIns.stream()
                .mapToInt(c -> c.getPhysicalActivityMinutes() != null ? c.getPhysicalActivityMinutes() : 0)
                .sum();

        int totalCognitiveGames = checkIns.stream()
                .mapToInt(c -> c.getCognitiveGameCount() != null ? c.getCognitiveGameCount() : 0)
                .sum();

        // Calculate percentages
        long totalDays = checkIns.size();
        double exercisePercentage = calculatePercentage(checkIns, DailyCheckIn::getDidExercise, totalDays);
        double healthyEatingPercentage = calculatePercentage(checkIns, DailyCheckIn::getAteHealthy, totalDays);
        double socialInteractionPercentage = calculatePercentage(checkIns, DailyCheckIn::getHadSocialInteraction, totalDays);
        double cognitiveGamePercentage = calculatePercentage(checkIns, DailyCheckIn::getPlayedCognitiveGameToday, totalDays);

        // Calculate trends
        String moodTrend = calculateTrend(checkIns, c -> {
            if (c.getMorningMood() != null && c.getEveningMood() != null) {
                return (c.getMorningMood() + c.getEveningMood()) / 2.0;
            }
            return c.getMorningMood() != null ? c.getMorningMood().doubleValue() :
                    c.getEveningMood() != null ? c.getEveningMood().doubleValue() : null;
        });

        String sleepTrend = calculateTrend(checkIns, c ->
                c.getSleepQuality() != null ? c.getSleepQuality().doubleValue() : null
        );

        String stressTrend = calculateTrendInverted(checkIns, c ->
                c.getStressLevel() != null ? c.getStressLevel().doubleValue() : null
        );

        String energyTrend = calculateTrend(checkIns, c ->
                c.getEnergyLevel() != null ? c.getEnergyLevel().doubleValue() : null
        );

        // Build daily data
        List<CheckInStatsResponse.DailyData> dailyData = buildDailyData(checkIns, startDate, endDate);

        // Generate insights
        List<String> insights = generateInsights(
                avgMorningMood, avgEveningMood, avgSleepQuality, avgSleepHours,
                avgEnergyLevel, avgStressLevel, exercisePercentage,
                healthyEatingPercentage, cognitiveGamePercentage,
                moodTrend, sleepTrend, stressTrend
        );

        // Mood distribution
        Map<String, Integer> moodDistribution = calculateMoodDistribution(checkIns);

        // Best/Worst days
        DailyCheckIn bestDay = checkIns.stream()
                .max(Comparator.comparingDouble(DailyCheckIn::calculateWellnessScore))
                .orElse(null);

        DailyCheckIn worstDay = checkIns.stream()
                .min(Comparator.comparingDouble(DailyCheckIn::calculateWellnessScore))
                .orElse(null);

        return CheckInStatsResponse.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .avgMorningMood(avgMorningMood)
                .avgEveningMood(avgEveningMood)
                .avgSleepQuality(avgSleepQuality)
                .avgSleepHours(avgSleepHours)
                .avgEnergyLevel(avgEnergyLevel)
                .avgStressLevel(avgStressLevel)
                .avgWellnessScore(avgWellnessScore)
                .totalCheckIns((int) totalDays)
                .totalPhysicalActivityMinutes(totalPhysicalActivityMinutes)
                .totalCognitiveGamesPlayed(totalCognitiveGames)
                .exercisePercentage(exercisePercentage)
                .healthyEatingPercentage(healthyEatingPercentage)
                .socialInteractionPercentage(socialInteractionPercentage)
                .cognitiveGamePercentage(cognitiveGamePercentage)
                .moodTrend(moodTrend)
                .sleepTrend(sleepTrend)
                .stressTrend(stressTrend)
                .energyTrend(energyTrend)
                .dailyData(dailyData)
                .insights(insights)
                .moodDistribution(moodDistribution)
                .bestDay(bestDay != null ? bestDay.getCheckInDate() : null)
                .worstDay(worstDay != null ? worstDay.getCheckInDate() : null)
                .bestWellnessScore(bestDay != null ? bestDay.calculateWellnessScore() : null)
                .worstWellnessScore(worstDay != null ? worstDay.calculateWellnessScore() : null)
                .build();
    }

    /**
     * Get weekly stats (last 7 days)
     */
    @Transactional(readOnly = true)
    public CheckInStatsResponse getWeeklyStats(Long userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        return getStats(userId, startDate, endDate);
    }

    /**
     * Get monthly stats (last 30 days)
     */
    @Transactional(readOnly = true)
    public CheckInStatsResponse getMonthlyStats(Long userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);
        return getStats(userId, startDate, endDate);
    }

    // ========== HELPER METHODS ==========

    private Double calculateAverage(List<DailyCheckIn> checkIns,
                                    java.util.function.Function<DailyCheckIn, Integer> getter) {
        return checkIns.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    private Double calculateAverageBigDecimal(List<DailyCheckIn> checkIns,
                                              java.util.function.Function<DailyCheckIn, java.math.BigDecimal> getter) {
        return checkIns.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double calculatePercentage(List<DailyCheckIn> checkIns,
                                       java.util.function.Function<DailyCheckIn, Boolean> getter,
                                       long total) {
        long count = checkIns.stream()
                .map(getter)
                .filter(Boolean.TRUE::equals)
                .count();
        return total > 0 ? (count * 100.0) / total : 0.0;
    }

    private String calculateTrend(List<DailyCheckIn> checkIns,
                                  java.util.function.Function<DailyCheckIn, Double> getter) {
        // Split into first half and second half
        int mid = checkIns.size() / 2;
        if (mid == 0) return "stable";

        List<DailyCheckIn> firstHalf = checkIns.subList(0, mid);
        List<DailyCheckIn> secondHalf = checkIns.subList(mid, checkIns.size());

        double firstAvg = firstHalf.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double secondAvg = secondHalf.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double diff = secondAvg - firstAvg;

        if (Math.abs(diff) < 0.5) return "stable";
        return diff > 0 ? "improving" : "declining";
    }

    private String calculateTrendInverted(List<DailyCheckIn> checkIns,
                                          java.util.function.Function<DailyCheckIn, Double> getter) {
        String trend = calculateTrend(checkIns, getter);
        // For stress, declining is good, improving is bad
        return switch (trend) {
            case "improving" -> "declining";
            case "declining" -> "improving";
            default -> "stable";
        };
    }

    private List<CheckInStatsResponse.DailyData> buildDailyData(
            List<DailyCheckIn> checkIns,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<LocalDate, DailyCheckIn> checkInMap = checkIns.stream()
                .collect(Collectors.toMap(DailyCheckIn::getCheckInDate, c -> c));

        List<CheckInStatsResponse.DailyData> dailyData = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DailyCheckIn checkIn = checkInMap.get(date);

            if (checkIn != null) {
                dailyData.add(CheckInStatsResponse.DailyData.builder()
                        .date(date)
                        .morningMood(checkIn.getMorningMood())
                        .eveningMood(checkIn.getEveningMood())
                        .sleepQuality(checkIn.getSleepQuality())
                        .energyLevel(checkIn.getEnergyLevel())
                        .stressLevel(checkIn.getStressLevel())
                        .wellnessScore(checkIn.calculateWellnessScore())
                        .hasCheckIn(true)
                        .build());
            } else {
                dailyData.add(CheckInStatsResponse.DailyData.builder()
                        .date(date)
                        .hasCheckIn(false)
                        .build());
            }
        }

        return dailyData;
    }

    private List<String> generateInsights(
            Double avgMood1, Double avgMood2, Double avgSleep, Double avgSleepHours,
            Double avgEnergy, Double avgStress, double exercisePercent,
            double healthyEatingPercent, double cognitiveGamePercent,
            String moodTrend, String sleepTrend, String stressTrend
    ) {
        List<String> insights = new ArrayList<>();

        // Mood insights
        double avgMood = (avgMood1 + avgMood2) / 2;
        if (avgMood >= 4.0) {
            insights.add("Great mood overall! Keep up the positive vibes!");
        } else if (avgMood < 3.0) {
            insights.add("Your mood has been low. Consider talking to someone or trying stress-relief activities.");
        }

        // Sleep insights
        if (avgSleepHours != null && avgSleepHours < 7.0) {
            insights.add("You're not getting enough sleep. Aim for 7-9 hours for better health!");
        } else if (avgSleepHours != null && avgSleepHours > 9.0) {
            insights.add("You might be oversleeping. Try maintaining a consistent sleep schedule.");
        }

        if (avgSleep != null && avgSleep >= 8.0) {
            insights.add("Excellent sleep quality! Your rest is paying off!");
        }

        // Stress insights
        if (avgStress != null && avgStress > 7.0) {
            insights.add("High stress levels detected. Consider meditation, exercise, or seeking support.");
        }

        // Exercise insights
        if (exercisePercent >= 70) {
            insights.add("Fantastic exercise routine! You're staying very active!");
        } else if (exercisePercent < 30) {
            insights.add("Try to increase physical activity - even 15 minutes a day helps!");
        }

        if (cognitiveGamePercent >= 50) {
            insights.add("Great job keeping your mind sharp with cognitive games!");
        } else if (cognitiveGamePercent < 20) {
            insights.add("Play more cognitive games to boost memory and focus!");
        }

        // Trends
        if ("improving".equals(moodTrend)) {
            insights.add("Your mood is improving! Whatever you're doing, keep it up!");
        }
        if ("declining".equals(moodTrend)) {
            insights.add("Your mood trend is declining. Pay attention to self-care.");
        }

        return insights;
    }

    private Map<String, Integer> calculateMoodDistribution(List<DailyCheckIn> checkIns) {
        Map<String, Integer> distribution = new HashMap<>();

        for (DailyCheckIn checkIn : checkIns) {
            if (checkIn.getMorningMoodEmoji() != null) {
                distribution.merge(checkIn.getMorningMoodEmoji(), 1, Integer::sum);
            }
            if (checkIn.getEveningMoodEmoji() != null) {
                distribution.merge(checkIn.getEveningMoodEmoji(), 1, Integer::sum);
            }
        }

        return distribution;
    }

    private CheckInStatsResponse buildEmptyStats(Long userId, LocalDate startDate, LocalDate endDate) {
        return CheckInStatsResponse.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .totalCheckIns(0)
                .insights(List.of("No check-ins yet. Start tracking your mental health today!"))
                .build();
    }
}
