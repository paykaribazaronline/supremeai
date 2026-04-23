package com.supremeai.service;

import com.supremeai.model.User;
import com.supremeai.repository.UserRepository;
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
    public double predictDaysRemaining(String userId) {
        User user = userRepository.findByFirebaseUid(userId).block();
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
    }

    /**
     * Check if user should be warned (<=3 days remaining).
     */
    public boolean shouldWarn(String userId) {
        Double daysRemaining = predictions.get(userId);
        return daysRemaining != null && daysRemaining >= 0 && daysRemaining <= 3;
    }

    /**
     * Get prediction details for a user.
     */
    public Map<String, Object> getPrediction(String userId) {
        Map<String, Object> result = new HashMap<>();
        double daysRemaining = predictDaysRemaining(userId);
        double dailyAvg = calculateMovingAverage(userId, 7);
        
        User user = userRepository.findByFirebaseUid(userId).block();
        long remainingQuota = user != null ? user.getMonthlyQuota() - user.getCurrentUsage() : 0;
        
        result.put("userId", userId);
        result.put("daysRemaining", daysRemaining);
        result.put("dailyAverageUsage", dailyAvg);
        result.put("remainingQuota", remainingQuota);
        result.put("shouldWarn", shouldWarn(userId));
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }

    private void updatePrediction(String userId) {
        double daysRemaining = predictDaysRemaining(userId);
        predictions.put(userId, daysRemaining);
    }

    /**
     * Scheduled task to update predictions daily.
     */
    @Scheduled(cron = "0 0 0 * * ?") // Every day at midnight
    public void updateAllPredictions() {
        userRepository.findAll().toIterable().forEach(user -> {
            updatePrediction(user.getFirebaseUid());
        });
    }

    /**
     * Get all users with low quota warnings.
     */
    public List<Map<String, Object>> getUsersNeedingWarning() {
        List<Map<String, Object>> warnings = new ArrayList<>();
        userRepository.findAll().toIterable().forEach(user -> {
            if (shouldWarn(user.getFirebaseUid())) {
                warnings.add(getPrediction(user.getFirebaseUid()));
            }
        });
        return warnings;
    }
}
