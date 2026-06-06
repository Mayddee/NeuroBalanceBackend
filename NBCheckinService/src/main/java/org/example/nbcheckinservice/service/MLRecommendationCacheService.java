package org.example.nbcheckinservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.client.AuthServiceClient;
import org.example.nbcheckinservice.dto.MLMetricsRequest;
import org.example.nbcheckinservice.dto.MLRecommendationResponse;
import org.example.nbcheckinservice.entity.DailyCheckIn;
import org.example.nbcheckinservice.entity.DailyMLRecommendation;
import org.example.nbcheckinservice.repository.DailyCheckInRepository;
import org.example.nbcheckinservice.repository.DailyMLRecommendationRepository;
import org.example.nbcheckinservice.repository.UserGameStatsRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache + persistence layer for personalized ML recommendations.
 *
 * Three-tier data source priority (per GET request):
 *
 *  1. In-memory cache (30-min TTL, fastest — avoids DB round-trip on every GET)
 *  2. DB table daily_ml_recommendation (today's record — survives server restarts)
 *  3. Fresh computation from real user data (check-ins, game stats, auth-service onboarding)
 *
 * Real-time update flow:
 *   Kafka event → asyncRefresh() → computeAndCache() → saves to DB + in-memory cache
 *
 * Frontend params (dailyScreenTime, caffeineIntake, dietType) from GET requests:
 *   - Stored in userPrefsCache (PATCH semantics — never resets previously set values)
 *   - Used as optional inputs on the NEXT Kafka-triggered recomputation
 *   - Never trigger an immediate recompute — frontend always gets data from DB/cache
 *   → This is the "masking": frontend sends any params, we return real data from DB.
 *
 * age and gender always from NBAuthService /api/v1/onboarding (24h cached).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MLRecommendationCacheService {

    // ── Config ────────────────────────────────────────────────────
    private static final ZoneId ALMATY = ZoneId.of("Asia/Almaty");

    /** In-memory TTL — also used to decide whether to trigger async refresh on DB hits */
    private static final Duration ML_TTL         = Duration.ofMinutes(30);
    /** Kafka debounce — skip refresh if last computation was within this window */
    private static final Duration DEBOUNCE       = Duration.ofMinutes(2);
    /** Onboarding cache TTL */
    private static final Duration ONBOARDING_TTL = Duration.ofHours(24);

    // ── Default fallbacks ─────────────────────────────────────────
    private static final int    DEF_AGE      = 25;
    private static final String DEF_GENDER   = "Male";
    private static final String DEF_DIET     = "Non-Vegetarian";
    private static final double DEF_SCREEN   = 8.0;
    private static final int    DEF_CAFFEINE = 2;

    // ── Dependencies ─────────────────────────────────────────────
    private final MLService mlService;
    private final DailyCheckInRepository checkInRepository;
    private final UserGameStatsRepository gameStatsRepository;
    private final AuthServiceClient authServiceClient;
    private final DailyMLRecommendationRepository mlRecommendationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── In-memory layers ─────────────────────────────────────────
    private final ConcurrentHashMap<Long, MLCacheEntry>         mlCache         = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, OnboardingCacheEntry> onboardingCache = new ConcurrentHashMap<>();
    /** PATCH storage: optional params sent by user on GET, reused on Kafka refreshes */
    private final ConcurrentHashMap<Long, MLUserPrefs>          userPrefsCache  = new ConcurrentHashMap<>();

    // ═════════════════════════════════════════════════════════════
    // READ — called from MLRecommendationController
    // ═════════════════════════════════════════════════════════════

    /**
     * Returns today's ML recommendations for the user.
     *
     * Source priority:
     *   1. In-memory cache — if fresh (same day + <30 min)
     *   2. DB record for today — if exists (warm in-memory cache, return immediately)
     *   3. Fresh computation from real data — saves to DB + in-memory cache
     *
     * Frontend params (dailyScreenTime / caffeineIntake / dietType):
     *   Stored for future Kafka-triggered refreshes (PATCH — doesn't reset prior values).
     *   NOT used to trigger immediate recompute — we always serve from DB/cache.
     *
     * @param authorizationHeader  "Bearer ..." JWT from the HTTP request — used to fetch onboarding
     */
    public MLRecommendationResponse getOrCompute(Long userId,
                                                  String authorizationHeader,
                                                  Double dailyScreenTime,
                                                  Integer caffeineIntake,
                                                  String dietType) {
        // 1. Refresh real age/gender from NBAuthService (24h cached).
        //    Returns true if onboarding was just fetched for the first time (or after 24h expiry).
        boolean onboardingJustLoaded = refreshOnboardingIfNeeded(userId, authorizationHeader);

        // 2. PATCH: store optional params so the next Kafka refresh uses them, not defaults
        updateUserPrefs(userId, dailyScreenTime, caffeineIntake, dietType);

        // 3. If onboarding was just loaded — recompute immediately so the DB record contains
        //    the real age/gender, not the defaults that may have been used by prior Kafka events.
        if (onboardingJustLoaded) {
            log.info("Onboarding freshly loaded for user {} — recomputing to apply real age/gender", userId);
            mlCache.remove(userId);   // evict any stale in-memory entry
            return computeAndCache(userId, "onboarding-refresh");
        }

        // 4. In-memory cache
        MLCacheEntry cached = mlCache.get(userId);
        if (cached != null && cached.isFresh()) {
            log.debug("ML in-memory cache hit for user {} ({}s ago)", userId,
                    Duration.between(cached.computedAt(), LocalDateTime.now()).toSeconds());
            return cached.response();
        }

        // 5. DB record for today
        LocalDate today = LocalDate.now(ALMATY);
        Optional<DailyMLRecommendation> dbRecord =
                mlRecommendationRepository.findByUserIdAndRecommendationDate(userId, today);

        if (dbRecord.isPresent()) {
            MLRecommendationResponse fromDb = deserialize(dbRecord.get().getRecommendationsJson());
            if (fromDb != null) {
                // Warm in-memory cache so next GET is served from memory
                mlCache.put(userId, new MLCacheEntry(fromDb, dbRecord.get().getUpdatedAt()));
                log.info("ML DB hit for user {} (trigger={}, updatedAt={})",
                        userId, dbRecord.get().getTriggerSource(), dbRecord.get().getUpdatedAt());
                return fromDb;
            }
        }

        // 6. Nothing in cache or DB — compute from real data, save to DB
        log.info("No cache/DB record for user {} today — computing from real data", userId);
        return computeAndCache(userId, "on-demand");
    }

    // ═════════════════════════════════════════════════════════════
    // HISTORY — called from controller for Swagger / table access
    // ═════════════════════════════════════════════════════════════

    /** Returns all daily ML recommendation records for the user, newest first. */
    public List<DailyMLRecommendation> getHistory(Long userId) {
        return mlRecommendationRepository.findByUserIdOrderByRecommendationDateDesc(userId);
    }

    // ═════════════════════════════════════════════════════════════
    // ASYNC REFRESH — called from Kafka consumers + internal refresh endpoint
    // ═════════════════════════════════════════════════════════════

    /**
     * Async, debounced refresh triggered by:
     *   - checkin.created / sleep.logged / game.completed  (Kafka → MLRecommendationConsumer)
     *   - POST /api/v1/ml/recommendations/refresh          (manual / internal endpoint)
     *
     * Always recomputes from real user data (DB check-ins, game stats, onboarding cache).
     * Saves result to DB so it survives server restarts.
     * Reuses onboarding + userPrefs caches (PATCH — never loses previously set params).
     */
    @Async
    public void asyncRefresh(Long userId, String triggerSource) {
        MLCacheEntry existing = mlCache.get(userId);
        if (existing != null && existing.isDebounced()) {
            log.debug("ML refresh debounced for user {} (trigger={})", userId, triggerSource);
            return;
        }

        log.info("ML async refresh triggered for user {} by '{}'", userId, triggerSource);
        try {
            computeAndCache(userId, triggerSource);
            log.info("ML recommendations updated in DB + cache for user {} (trigger={})",
                    userId, triggerSource);
        } catch (Exception e) {
            log.warn("ML async refresh failed for user {} (trigger={}): {}",
                    userId, triggerSource, e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════
    // CORE COMPUTATION
    // ═════════════════════════════════════════════════════════════

    private MLRecommendationResponse computeAndCache(Long userId, String triggerSource) {
        MLMetricsRequest request = buildRequest(userId);
        // Pass userId so Python caches result → frontend's direct /recommend/top3 call gets real data
        MLRecommendationResponse response = mlService.getTop3Recommendations(request, userId);

        // Save to in-memory cache
        mlCache.put(userId, new MLCacheEntry(response, LocalDateTime.now()));

        // Save to DB (upsert by userId + date) — graceful degradation on failure
        saveOrUpdateDb(userId, response, triggerSource);

        return response;
    }

    /**
     * Assembles the ML request from all available data sources.
     *
     * Core metrics (always from real data):
     *   sleep_duration      ← avg check-in sleepHours last 7 days    (DB)
     *   stress_level        ← avg check-in stressLevel last 7 days   (DB)
     *   exercise_frequency  ← days with didExercise=true last 7 days (DB)
     *   memory_test_score   ← cognitive game win-rate proxy          (DB)
     *   reaction_time       ← NUMBER_SEQUENCE best-time proxy        (DB)
     *   age                 ← onboarding birthDate → years           (auth-service, 24h cached)
     *   gender              ← onboarding sex                         (auth-service, 24h cached)
     *
     * Optional (from userPrefsCache, set via GET params, PATCH semantics):
     *   daily_screen_time   screen hours/day  (default 8.0)
     *   caffeine_intake     cups/day          (default 2)
     *   diet_type           diet type         (default "Non-Vegetarian")
     */
    public MLMetricsRequest buildRequest(Long userId) {
        LocalDate today   = LocalDate.now(ALMATY);
        LocalDate weekAgo = today.minusDays(6);

        List<DailyCheckIn> recent = checkInRepository
                .findByUserIdAndCheckInDateBetweenOrderByCheckInDateDesc(userId, weekAgo, today);

        double sleepDuration = recent.stream()
                .filter(c -> c.getSleepHours() != null)
                .mapToDouble(c -> c.getSleepHours().doubleValue())
                .average()
                .orElse(7.0);
        sleepDuration = Math.round(sleepDuration * 10.0) / 10.0;

        int stressLevel = (int) Math.round(
                recent.stream()
                        .filter(c -> c.getStressLevel() != null)
                        .mapToDouble(DailyCheckIn::getStressLevel)
                        .average()
                        .orElse(5.0));

        int exerciseDays = (int) recent.stream()
                .filter(c -> Boolean.TRUE.equals(c.getDidExercise()))
                .count();

        int memoryTestScore = gameStatsRepository.findByUserId(userId)
                .map(s -> {
                    if (s.getTotalGamesPlayed() == null || s.getTotalGamesPlayed() == 0) return 70;
                    int wins = s.getTotalWins() != null ? s.getTotalWins() : 0;
                    return (int) Math.round((wins * 100.0) / s.getTotalGamesPlayed());
                })
                .orElse(70);

        int reactionTime = gameStatsRepository.findByUserId(userId)
                .map(s -> {
                    Integer best = s.getNumberSequenceBestTime();
                    if (best == null) return 300;
                    if (best < 30)   return 220;
                    if (best < 60)   return 270;
                    if (best < 90)   return 310;
                    return 360;
                })
                .orElse(300);

        OnboardingCacheEntry onboarding = onboardingCache.get(userId);
        int age       = (onboarding != null) ? onboarding.age()    : DEF_AGE;
        String gender = (onboarding != null) ? onboarding.gender() : DEF_GENDER;

        MLUserPrefs prefs = userPrefsCache.getOrDefault(userId, MLUserPrefs.defaults());

        log.info("ML payload for user {}: sleep={:.1f}h, stress={}/10, exercise={}/7days, " +
                        "memory={}%, reaction={}ms | age={}, gender={}, diet={}, screen={}h, caffeine={}",
                userId, sleepDuration, stressLevel, exerciseDays,
                memoryTestScore, reactionTime, age, gender, prefs.dietType(),
                prefs.dailyScreenTime(), prefs.caffeineIntake());

        return MLMetricsRequest.builder()
                .sleepDuration(sleepDuration)
                .stressLevel(stressLevel)
                .dailyScreenTime(prefs.dailyScreenTime())
                .exerciseFrequency(exerciseDays)
                .caffeineIntake(prefs.caffeineIntake())
                .reactionTime(reactionTime)
                .memoryTestScore(memoryTestScore)
                .age(age)
                .gender(gender)
                .dietType(prefs.dietType())
                .build();
    }

    // ═════════════════════════════════════════════════════════════
    // DB PERSISTENCE
    // ═════════════════════════════════════════════════════════════

    private void saveOrUpdateDb(Long userId, MLRecommendationResponse response, String triggerSource) {
        try {
            LocalDate today = LocalDate.now(ALMATY);
            String json = objectMapper.writeValueAsString(response);

            DailyMLRecommendation record = mlRecommendationRepository
                    .findByUserIdAndRecommendationDate(userId, today)
                    .orElseGet(() -> DailyMLRecommendation.builder()
                            .userId(userId)
                            .recommendationDate(today)
                            .build());

            record.setRecommendationsJson(json);
            record.setCognitiveScore(response.getCognitiveScore());
            record.setTriggerSource(triggerSource);

            mlRecommendationRepository.save(record);
            log.debug("ML recommendation persisted to DB for user {} (trigger={}, date={})",
                    userId, triggerSource, today);
        } catch (Exception e) {
            // Graceful degradation — ML still served from cache even if DB write fails
            log.warn("Failed to persist ML recommendation to DB for user {}: {}", userId, e.getMessage());
        }
    }

    private MLRecommendationResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, MLRecommendationResponse.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize ML recommendation from DB: {}", e.getMessage());
            return null;
        }
    }

    // ═════════════════════════════════════════════════════════════
    // ONBOARDING CACHE
    // ═════════════════════════════════════════════════════════════

    /**
     * Fetches onboarding data from NBAuthService and caches it (24h TTL).
     *
     * @return true if data was just fetched (cache was empty or stale), false if cache was already fresh.
     *         Callers use this to force ML recompute when real age/gender becomes available.
     */
    private boolean refreshOnboardingIfNeeded(Long userId, String authHeader) {
        if (authHeader == null || authHeader.isBlank()) return false;

        OnboardingCacheEntry existing = onboardingCache.get(userId);
        if (existing != null && !existing.isStale()) return false;  // still fresh — no fetch needed

        boolean[] fetched = {false};
        authServiceClient.getMyOnboarding(authHeader).ifPresent(data -> {
            onboardingCache.put(userId,
                    new OnboardingCacheEntry(data.age(), data.gender(), LocalDateTime.now()));
            log.info("Onboarding cached for user {}: age={}, gender={}", userId, data.age(), data.gender());
            fetched[0] = true;
        });
        return fetched[0];
    }

    // ═════════════════════════════════════════════════════════════
    // USER PREFS (PATCH semantics)
    // ═════════════════════════════════════════════════════════════

    private void updateUserPrefs(Long userId, Double dailyScreenTime,
                                  Integer caffeineIntake, String dietType) {
        MLUserPrefs current = userPrefsCache.getOrDefault(userId, MLUserPrefs.defaults());

        MLUserPrefs updated = new MLUserPrefs(
                dailyScreenTime != null ? dailyScreenTime : current.dailyScreenTime(),
                caffeineIntake  != null ? caffeineIntake  : current.caffeineIntake(),
                dietType        != null ? dietType        : current.dietType()
        );

        if (!updated.equals(current)) {
            userPrefsCache.put(userId, updated);
            log.debug("User prefs updated for user {}: screen={}h, caffeine={}, diet={}",
                    userId, updated.dailyScreenTime(), updated.caffeineIntake(), updated.dietType());
        }
    }

    // ═════════════════════════════════════════════════════════════
    // INNER VALUE TYPES
    // ═════════════════════════════════════════════════════════════

    private record MLCacheEntry(MLRecommendationResponse response, LocalDateTime computedAt) {

        /** Fresh = same calendar day (Asia/Almaty) AND within 30 minutes */
        boolean isFresh() {
            boolean sameDay   = computedAt.toLocalDate().equals(LocalDate.now(ZoneId.of("Asia/Almaty")));
            boolean withinTtl = Duration.between(computedAt, LocalDateTime.now()).compareTo(ML_TTL) < 0;
            return sameDay && withinTtl;
        }

        /** Skip Kafka-triggered refresh if computed less than 2 minutes ago */
        boolean isDebounced() {
            return Duration.between(computedAt, LocalDateTime.now()).compareTo(DEBOUNCE) < 0;
        }
    }

    private record OnboardingCacheEntry(int age, String gender, LocalDateTime cachedAt) {

        boolean isStale() {
            return Duration.between(cachedAt, LocalDateTime.now()).compareTo(ONBOARDING_TTL) >= 0;
        }
    }

    /**
     * User-provided optional ML params.
     * Persisted between GET calls so Kafka refreshes never reset them (PATCH behaviour).
     */
    private record MLUserPrefs(double dailyScreenTime, int caffeineIntake, String dietType) {

        static MLUserPrefs defaults() {
            return new MLUserPrefs(DEF_SCREEN, DEF_CAFFEINE, DEF_DIET);
        }
    }
}
