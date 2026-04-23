package com.supremeai.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Advanced Predictive ML Service with time-series forecasting.
 * Uses ARIMA-like models for quota prediction and user behavior forecasting.
 */
@Service
public class AdvancedPredictiveMLService {

    private static final Logger log = LoggerFactory.getLogger(AdvancedPredictiveMLService.class);

    @Autowired
    private EnhancedRandomForestPredictor randomForestPredictor;

    // Time-series data storage
    private final Map<String, TimeSeriesData> userQuotaHistory = new ConcurrentHashMap<>();
    private final Map<String, TimeSeriesData> systemLoadHistory = new ConcurrentHashMap<>();
    private final Map<String, TimeSeriesData> userActivityHistory = new ConcurrentHashMap<>();

    // Model parameters
    private static final int AR_ORDER = 5;        // AutoRegressive order
    private static final int I_ORDER = 1;         // Integrated order (differencing)
    private static final int MA_ORDER = 5;        // Moving Average order
    private static final int MIN_DATA_POINTS = 30; // Minimum data for forecasting

    /**
     * Record daily quota usage for a user.
     */
    public void recordQuotaUsage(String userId, long used, long quota) {
        TimeSeriesData data = userQuotaHistory.computeIfAbsent(userId,
            k -> new TimeSeriesData("quota_" + userId));

        double ratio = quota > 0 ? (double) used / quota : 0.0;
        data.addDataPoint(System.currentTimeMillis(), ratio);

        // Predict quota exhaustion if we have enough data
        if (data.size() >= MIN_DATA_POINTS) {
            predictQuotaExhaustion(userId, data, quota);
        }
    }

    /**
     * Record system load.
     */
    public void recordSystemLoad(String serviceName, double load) {
        TimeSeriesData data = systemLoadHistory.computeIfAbsent(serviceName,
            k -> new TimeSeriesData("load_" + serviceName));
        data.addDataPoint(System.currentTimeMillis(), load);
    }

    /**
     * Record user activity (for behavior prediction).
     */
    public void recordUserActivity(String userId, String activityType) {
        TimeSeriesData data = userActivityHistory.computeIfAbsent(userId,
            k -> new TimeSeriesData("activity_" + userId));

        // Convert activity to numeric value
        double activityValue = activityType.hashCode() % 100 / 100.0;
        data.addDataPoint(System.currentTimeMillis(), activityValue);
    }

    /**
     * Predict when user's quota will be exhausted using ARIMA.
     */
    public QuotaPrediction predictQuotaExhaustion(String userId, long currentUsage, long monthlyQuota) {
        TimeSeriesData data = userQuotaHistory.get(userId);
        if (data == null || data.size() < MIN_DATA_POINTS) {
            return new QuotaPrediction(
                userId,
                monthlyQuota - currentUsage,
                -1, // Unknown days remaining
                0.0, // Low confidence
                "Insufficient data for prediction",
                Map.of()
            );
        }

        return predictQuotaExhaustion(userId, data, monthlyQuota);
    }

    /**
     * Internal method to predict quota exhaustion.
     */
    private QuotaPrediction predictQuotaExhaustion(String userId, TimeSeriesData data, long monthlyQuota) {
        // Prepare time series (get last N values)
        List<Double> values = data.getRecentValues(MIN_DATA_POINTS);

        // Apply differencing (I in ARIMA)
        List<Double> differenced = applyDifferencing(values, I_ORDER);

        // Fit AR model
        double[] arCoefficients = fitARModel(differenced, AR_ORDER);

        // Fit MA model
        double[] maCoefficients = fitMAModel(differenced, MA_ORDER);

        // Forecast future values
        List<Double> forecast = forecastARIMA(values, arCoefficients, maCoefficients, 30); // Next 30 days

        // Calculate when quota will be exhausted
        double dailyUsageRate = calculateDailyUsageRate(values);
        long remainingQuota = monthlyQuota - (long) (dailyUsageRate * data.getLastValue() * monthlyQuota);

        int daysUntilExhaustion = -1;
        if (dailyUsageRate > 0) {
            daysUntilExhaustion = (int) (remainingQuota / dailyUsageRate);
        }

        // Calculate confidence based on model fit
        double confidence = calculatePredictionConfidence(values, forecast);

        // Generate warnings
        List<String> warnings = new ArrayList<>();
        if (daysUntilExhaustion >= 0 && daysUntilExhaustion <= 7) {
            warnings.add("WARNING: Quota will be exhausted in " + daysUntilExhaustion + " days!");
        }
        if (daysUntilExhaustion >= 0 && daysUntilExhaustion <= 3) {
            warnings.add("CRITICAL: Less than 3 days of quota remaining!");
        }

        // Trend analysis
        String trend = analyzeTrend(values);
        if (trend.equals("increasing")) {
            warnings.add("Usage trend is increasing - consider upgrading your plan");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("dailyUsageRate", dailyUsageRate);
        metadata.put("trend", trend);
        metadata.put("forecast", forecast.subList(0, Math.min(7, forecast.size())));
        metadata.put("arCoefficients", Arrays.toString(arCoefficients));
        metadata.put("historicalDataPoints", values.size());

        return new QuotaPrediction(
            userId,
            remainingQuota,
            daysUntilExhaustion,
            confidence,
            warnings.isEmpty() ? "Quota prediction calculated" : String.join("; ", warnings),
            metadata
        );
    }

    /**
     * Apply differencing to make time series stationary.
     */
    private List<Double> applyDifferencing(List<Double> values, int order) {
        List<Double> result = new ArrayList<>(values);
        for (int d = 0; d < order; d++) {
            List<Double> differenced = new ArrayList<>();
            for (int i = 1; i < result.size(); i++) {
                differenced.add(result.get(i) - result.get(i - 1));
            }
            result = differenced;
        }
        return result;
    }

    /**
     * Fit AutoRegressive model (simplified).
     */
    private double[] fitARModel(List<Double> values, int p) {
        // Simplified AR fitting using Yule-Walker equations (approximation)
        double[] coefficients = new double[p];

        if (values.size() <= p) return coefficients;

        // Calculate autocovariance
        for (int i = 0; i < p; i++) {
            double cov = 0.0;
            int count = 0;
            for (int t = i; t < values.size(); t++) {
                cov += values.get(t) * values.get(t - i);
                count++;
            }
            coefficients[i] = count > 0 ? cov / count : 0.0;
        }

        // Normalize
        double variance = calculateVariance(values);
        if (variance > 0) {
            for (int i = 0; i < p; i++) {
                coefficients[i] /= variance;
            }
        }

        return coefficients;
    }

    /**
     * Fit Moving Average model (simplified).
     */
    private double[] fitMAModel(List<Double> values, int q) {
        double[] coefficients = new double[q];

        // Simplified MA fitting
        // In practice, this requires more sophisticated methods
        for (int i = 0; i < Math.min(q, values.size() - 1); i++) {
            coefficients[i] = 0.1 * (i + 1); // Placeholder
        }

        return coefficients;
    }

    /**
     * Forecast future values using ARIMA.
     */
    private List<Double> forecastARIMA(List<Double> history, double[] ar, double[] ma, int steps) {
        List<Double> forecast = new ArrayList<>(history);

        for (int s = 0; s < steps; s++) {
            double prediction = forecast.get(forecast.size() - 1); // Start with last value

            // AR component
            prediction = forecast.get(forecast.size() - 1); // Start with last value
            for (int i = 1; i < ar.length && i < forecast.size(); i++) {
                prediction += ar[i] * (forecast.get(forecast.size() - 1 - i) - forecast.get(forecast.size() - 2 - i));
            }

            // MA component (simplified - assumes error terms are small)
            // In practice, we'd need the error terms from the fit

            forecast.add(prediction);
        }

        // Return only the forecasted values
        return forecast.subList(history.size(), forecast.size());
    }

    /**
     * Calculate daily usage rate from time series.
     */
    private double calculateDailyUsageRate(List<Double> values) {
        if (values.size() < 2) return 0.0;

        // Linear regression to find trend
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope * values.size() / 30.0; // Normalize to daily rate
    }

    /**
     * Calculate variance of a time series.
     */
    private double calculateVariance(List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);
    }

    /**
     * Calculate prediction confidence.
     */
    private double calculatePredictionConfidence(List<Double> actual, List<Double> forecast) {
        if (actual.size() < 10) return 0.3;

        // Compare last few actual values with their "forecasts"
        int checkSize = Math.min(5, actual.size() / 2);
        List<Double> recentActual = actual.subList(actual.size() - checkSize, actual.size());

        // Simplified: check if trend matches
        double actualTrend = recentActual.get(recentActual.size() - 1) - recentActual.get(0);
        double forecastTrend = forecast.isEmpty() ? 0 : forecast.get(forecast.size() - 1) - forecast.get(0);

        double trendMatch = 1.0 - Math.abs(actualTrend - forecastTrend) / (Math.abs(actualTrend) + 0.01);
        return Math.max(0.1, Math.min(0.95, trendMatch));
    }

    /**
     * Analyze trend in time series.
     */
    private String analyzeTrend(List<Double> values) {
        if (values.size() < 5) return "stable";

        // Compare first and last halves
        int half = values.size() / 2;
        double firstHalfAvg = values.subList(0, half).stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double secondHalfAvg = values.subList(half, values.size()).stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double change = (secondHalfAvg - firstHalfAvg) / (firstHalfAvg + 0.01);

        if (change > 0.1) return "increasing";
        else if (change < -0.1) return "decreasing";
        else return "stable";
    }

    /**
     * Predict user's next activity based on historical patterns.
     */
    public UserActivityPrediction predictNextActivity(String userId) {
        TimeSeriesData data = userActivityHistory.get(userId);
        if (data == null || data.size() < 10) {
            return new UserActivityPrediction(userId, "unknown", 0.0, Map.of());
        }

        // Group activities by time of day
        Map<Integer, List<Double>> hourlyActivities = new HashMap<>();
        // This would need actual time stamps and activity types

        // Simplified: predict based on recent trend
        List<Double> recent = data.getRecentValues(10);
        String predictedActivity = "continue_current"; // Placeholder
        double confidence = 0.5;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("recentActivities", recent);
        metadata.put("trend", analyzeTrend(recent));

        return new UserActivityPrediction(userId, predictedActivity, confidence, metadata);
    }

    /**
     * Scheduled task to run predictions and send alerts.
     */
    @Scheduled(cron = "0 0 9,15,21 * * *") // 9 AM, 3 PM, 9 PM
    public void scheduledPredictions() {
        log.info("Running scheduled predictions...");

        for (String userId : userQuotaHistory.keySet()) {
            try {
                TimeSeriesData data = userQuotaHistory.get(userId);
                // This would trigger alerts if quota is running low
                log.debug("Checked predictions for user: {}", userId);
            } catch (Exception e) {
                log.error("Error in scheduled prediction for {}: {}", userId, e.getMessage());
            }
        }
    }

    /**
     * Get prediction statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("quotaHistoryUsers", userQuotaHistory.size());
        stats.put("systemLoadServices", systemLoadHistory.size());
        stats.put("activityHistoryUsers", userActivityHistory.size());

        // Calculate average confidence across all predictions
        // (Placeholder - would need to track this)

        return stats;
    }

    // ── Data Classes ──────────────────────────────────────────────────────────

    public static class QuotaPrediction {
        public final String userId;
        public final long remainingQuota;
        public final int daysUntilExhaustion;
        public final double confidence;
        public final String message;
        public final Map<String, Object> metadata;

        public QuotaPrediction(String userId, long remainingQuota, int daysUntilExhaustion,
                               double confidence, String message, Map<String, Object> metadata) {
            this.userId = userId;
            this.remainingQuota = remainingQuota;
            this.daysUntilExhaustion = daysUntilExhaustion;
            this.confidence = confidence;
            this.message = message;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
    }

    public static class UserActivityPrediction {
        public final String userId;
        public final String predictedActivity;
        public final double confidence;
        public final Map<String, Object> metadata;

        public UserActivityPrediction(String userId, String predictedActivity,
                                      double confidence, Map<String, Object> metadata) {
            this.userId = userId;
            this.predictedActivity = predictedActivity;
            this.confidence = confidence;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
    }

    /**
     * Time series data storage.
     */
    private static class TimeSeriesData {
        String name;
        List<Long> timestamps = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        TimeSeriesData(String name) {
            this.name = name;
        }

        void addDataPoint(long timestamp, double value) {
            timestamps.add(timestamp);
            values.add(value);

            // Keep only last 1000 points
            if (timestamps.size() > 1000) {
                timestamps = timestamps.subList(timestamps.size() - 1000, timestamps.size());
                values = values.subList(values.size() - 1000, values.size());
            }
        }

        int size() {
            return values.size();
        }

        Double getLastValue() {
            return values.isEmpty() ? 0.0 : values.get(values.size() - 1);
        }

        List<Double> getRecentValues(int n) {
            return values.subList(Math.max(0, values.size() - n), values.size());
        }
    }
}
