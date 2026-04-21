package com.supremeai.service;

import com.supremeai.model.User;
import com.supremeai.repository.UserRepository;
import com.supremeai.exception.SimulatorQuotaExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class QuotaService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Check if a user has quota remaining for the current month
     */
    public boolean hasQuotaRemaining(String userId) {
        User user = userRepository.findByFirebaseUid(userId).block();
        if (user == null || !user.getIsActive()) {
            return false;
        }
        return user.hasQuotaRemaining();
    }

    /**
     * Increment usage for a user
     * Returns true if successful, false if quota exceeded
     */
    public boolean incrementUsage(String userId) {
        User user = userRepository.findByFirebaseUid(userId).block();
        if (user == null || !user.getIsActive()) {
            return false;
        }
        
        if (user.hasQuotaRemaining()) {
            user.setCurrentUsage(user.getCurrentUsage() + 1);
            user.setLastUsedAt(LocalDateTime.now());
            userRepository.save(user).block();
            return true;
        }
        return false;
    }

    /**
     * Validate and increment usage atomically
     * @throws SimulatorQuotaExceededException if quota is exceeded
     */
    public void validateAndIncrement(String userId) throws SimulatorQuotaExceededException {
        User user = userRepository.findByFirebaseUid(userId).block();
        if (user == null || !user.getIsActive()) {
            throw new IllegalArgumentException("Invalid or inactive user");
        }
        
        if (!user.hasQuotaRemaining()) {
            throw new SimulatorQuotaExceededException("Monthly quota exceeded for user: " + userId);
        }

        user.setCurrentUsage(user.getCurrentUsage() + 1);
        user.setLastUsedAt(LocalDateTime.now());
        userRepository.save(user).block();
    }

    /**
     * Get current usage for a user
     */
    public Long getCurrentUsage(String userId) {
        User user = userRepository.findByFirebaseUid(userId).block();
        return user != null ? user.getCurrentUsage() : 0L;
    }

    /**
     * Get monthly quota for a user
     */
    public Long getMonthlyQuota(String userId) {
        User user = userRepository.findByFirebaseUid(userId).block();
        return user != null ? user.getMonthlyQuota() : 0L;
    }

    /**
     * Reset monthly usage for all users - runs on the 1st of every month at midnight
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void resetMonthlyUsage() {
        userRepository.findAll()
            .doOnNext(user -> {
                user.resetMonthlyUsage();
                userRepository.save(user).subscribe();
            })
            .subscribe();
    }

    /**
     * Manually reset usage for a specific user (admin function)
     */
    public boolean resetUserUsage(String userId) {
        User user = userRepository.findByFirebaseUid(userId).block();
        if (user == null) {
            return false;
        }

        user.resetMonthlyUsage();
        userRepository.save(user).block();
        return true;
    }

    /**
     * Get usage statistics for a user
     */
    public UserUsageStats getUsageStats(String userId) {
        User user = userRepository.findByFirebaseUid(userId).block();
        if (user == null) {
            return null;
        }

        return new UserUsageStats(
            user.getCurrentUsage(),
            user.getMonthlyQuota(),
            user.getLastUsedAt(),
            user.hasQuotaRemaining()
        );
    }

    public static class UserUsageStats {
        private final Long currentUsage;
        private final Long monthlyQuota;
        private final LocalDateTime lastUsedAt;
        private final boolean hasQuotaRemaining;

        public UserUsageStats(Long currentUsage, Long monthlyQuota, LocalDateTime lastUsedAt, boolean hasQuotaRemaining) {
            this.currentUsage = currentUsage;
            this.monthlyQuota = monthlyQuota;
            this.lastUsedAt = lastUsedAt;
            this.hasQuotaRemaining = hasQuotaRemaining;
        }

        public Long getCurrentUsage() { return currentUsage; }
        public Long getMonthlyQuota() { return monthlyQuota; }
        public LocalDateTime getLastUsedAt() { return lastUsedAt; }
        public boolean isHasQuotaRemaining() { return hasQuotaRemaining; }

        public double getUsagePercentage() {
            if (monthlyQuota == 0) return 0.0;
            if (monthlyQuota == Long.MAX_VALUE) return 0.0;
            return (double) currentUsage / monthlyQuota * 100.0;
        }
    }
}
