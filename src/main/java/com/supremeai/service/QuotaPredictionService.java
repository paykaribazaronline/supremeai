package com.supremeai.service;

import com.supremeai.model.User;
import com.supremeai.repository.UserRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Predicts quota exhaustion using moving average of daily usage.
 * Warns users 3 days before quota runs out.
 */
@Service
public class QuotaPredictionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuotaService quotaService;

    // Track daily usage per user: userId -> Map<date, usageCount>
    private final Map<String, Map<LocalDate, Integer>> dailyUsage = new ConcurrentHashMap<>();

    // Track predictions per user: userId -> daysRemaining
    private final Map<String, Double> predictions = new ConcurrentHashMap<>();

    /**
     * Record usage for a user on the current day.
     */
    public void recordUsage(String userId) {
        LocalDate today = LocalDate.now();
        dailyUsage.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .merge(today, 1, Integer::sum);
        
        // Update prediction
        updatePrediction(userId);
    }

    /**
     * Calculate 7-day moving average of daily usage.
     */
    public double calculateMovingAverage(String userId, int days) {
        Map<LocalDate, Integer> userDaily = dailyUsage.get(userId);
        if (userDaily == null || userDaily.isEmpty()) {
            return 0.0;
        }

        LocalDate today = LocalDate.now();
        double total = 0.0;
        int count = 0;
        for (int i = 0; i < days; i++) {
            LocalDate date = today.minusDays(i);
            Integer usage = userDaily.get(date);
            if (usage != null) {
                total += usage;
                count++;
            }
        }

        return count == 0 ? 0.0 : total / count;
    }

    /**
     * Predict days remaining until quota exhaustion.
     * Returns days remaining, or -1 if quota is unlimited.
     */
    public Mono<Double> predictDaysRemaining(String userId) {
        return userRepository.findByFirebaseUid(userId)
            .map(user -> {
                if (user == null || user.getTier().hasUnlimitedQuota()) {
                    return -1.0; // Unlimited
                }

                double dailyAverage = calculateMovingAverage(userId, 7);
                if (dailyAverage <= 0.0) {
                    return Double.POSITIVE_INFINITY; // No usage, can't predict
                }

                long remainingQuota = user.getMonthlyQuota() - user.getCurrentUsage();
                if (remainingQuota <= 0) {
                    return 0.0; // Already exhausted
                }

                return remainingQuota / dailyAverage;
            })
            .defaultIfEmpty(-1.0);
    }

    /**
     * Check if user should be warned (<=3 days remaining).
     */
    public Mono<Boolean> shouldWarn(String userId) {
        return predictDaysRemaining(userId)
            .map(daysRemaining -> daysRemaining >= 0 && daysRemaining <= 3);
    }

    /**
     * Get prediction details for a user.
     */
    public Mono<Map<String, Object>> getPrediction(String userId) {
        return Mono.zip(
            predictDaysRemaining(userId),
            userRepository.findByFirebaseUid(userId)
        ).map(tuple -> {
            double daysRemaining = tuple.getT1();
            User user = tuple.getT2();
            double dailyAvg = calculateMovingAverage(userId, 7);
            long remainingQuota = user != null ? user.getMonthlyQuota() - user.getCurrentUsage() : 0;
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("daysRemaining", daysRemaining);
            result.put("dailyAverageUsage", dailyAvg);
            result.put("remainingQuota", remainingQuota);
            result.put("shouldWarn", daysRemaining >= 0 && daysRemaining <= 3);
            result.put("timestamp", System.currentTimeMillis());
            
            return result;
        });
    }

    private void updatePrediction(String userId) {
        predictDaysRemaining(userId).subscribe(daysRemaining -> {
            predictions.put(userId, daysRemaining);
        });
    }

    /**
     * Scheduled task to update predictions daily.
     */
    @Scheduled(cron = "0 0 0 * * ?") // Every day at midnight
    public void updateAllPredictions() {
        userRepository.findAll().subscribe(user -> {
            updatePrediction(user.getFirebaseUid());
        });
    }

    /**
     * Get all users with low quota warnings.
     */
    public Mono<List<Map<String, Object>>> getUsersNeedingWarning() {
        return userRepository.findAll()
            .flatMap(user -> shouldWarn(user.getFirebaseUid())
                .flatMap(warn -> warn ? getPrediction(user.getFirebaseUid()) : Mono.empty()))
            .collectList();
    }
}
