package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * User Behavior Profiling and Smart Suggestion Engine.
 * Learns from user interactions to provide personalized suggestions.
 */
@Service
public class UserBehaviorProfilingService {

    private static final Logger log = LoggerFactory.getLogger(UserBehaviorProfilingService.class);

    // User behavior profiles
    private final Map<String, UserProfile> userProfiles = new ConcurrentHashMap<>();

    // Global behavior patterns
    private final Map<String, BehaviorPattern> globalPatterns = new ConcurrentHashMap<>();

    // Session tracking
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Track a user action for behavior analysis.
     */
    public void trackAction(String userId, String actionType, Map<String, Object> metadata) {
        UserProfile profile = userProfiles.computeIfAbsent(userId, k -> new UserProfile(userId));
        UserSession session = activeSessions.computeIfAbsent(userId, k -> new UserSession(userId));

        UserAction action = new UserAction(
            actionType,
            System.currentTimeMillis(),
            metadata != null ? new HashMap<>(metadata) : new HashMap<>()
        );

        profile.addAction(action);
        session.addAction(action);

        // Update behavior patterns
        analyzeBehaviorPattern(profile, action);

        // Generate suggestions if enough data
        if (profile.getTotalActions() % 10 == 0) {
            generateSuggestions(userId);
        }

        log.debug("Tracked action {} for user {}", actionType, userId);
    }

    /**
     * Get personalized suggestions for a user.
     */
    public List<SmartSuggestion> getSuggestions(String userId, int maxSuggestions) {
        UserProfile profile = userProfiles.get(userId);
        if (profile == null || profile.getTotalActions() < 5) {
            return getDefaultSuggestions();
        }

        List<SmartSuggestion> suggestions = new ArrayList<>();

        // 1. Time-based suggestions
        suggestions.addAll(getTimeBasedSuggestions(profile));

        // 2. Pattern-based suggestions
        suggestions.addAll(getPatternBasedSuggestions(profile));

        // 3. Collaborative filtering (what similar users did)
        suggestions.addAll(getCollaborativeSuggestions(profile));

        // 4. Task prediction
        suggestions.addAll(predictNextTasks(profile));

        // Sort by confidence and return top N
        return suggestions.stream()
            .sorted((a, b) -> Double.compare(b.confidence, a.confidence))
            .limit(maxSuggestions)
            .collect(Collectors.toList());
    }

    /**
     * Get time-based suggestions based on usage patterns.
     */
    private List<SmartSuggestion> getTimeBasedSuggestions(UserProfile profile) {
        List<SmartSuggestion> suggestions = new ArrayList<>();
        int currentHour = LocalDateTime.now().getHour();
        DayOfWeek currentDay = LocalDateTime.now().getDayOfWeek();

        // Find most common actions at this time
        Map<String, Long> timeBasedActions = profile.actions.stream()
            .filter(a -> isSimilarTime(a.timestamp, currentHour))
            .collect(Collectors.groupingBy(a -> a.actionType, Collectors.counting()));

        timeBasedActions.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(3)
            .forEach(entry -> {
                suggestions.add(new SmartSuggestion(
                    "Continue with " + entry.getKey(),
                    "You usually do this at this time",
                    entry.getKey(),
                    Math.min(0.9, 0.5 + entry.getValue() * 0.1),
                    SuggestionType.TIME_BASED
                ));
            });

        return suggestions;
    }

    /**
     * Get pattern-based suggestions.
     */
    private List<SmartSuggestion> getPatternBasedSuggestions(UserProfile profile) {
        List<SmartSuggestion> suggestions = new ArrayList<>();

        // Detect common sequences
        List<String> recentActions = profile.actions.stream()
            .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
            .limit(5)
            .map(a -> a.actionType)
            .collect(Collectors.toList());

        if (recentActions.size() >= 2) {
            String lastAction = recentActions.get(0);
            String secondLastAction = recentActions.get(1);

            // Check if this sequence usually leads to a third action
            BehaviorPattern pattern = globalPatterns.get(secondLastAction + "->" + lastAction);
            if (pattern != null && pattern.followUpAction != null) {
                suggestions.add(new SmartSuggestion(
                    "Try " + pattern.followUpAction + " next",
                    "Based on your usage pattern " + secondLastAction + " -> " + lastAction,
                    pattern.followUpAction,
                    pattern.confidence,
                    SuggestionType.PATTERN_BASED
                ));
            }
        }

        return suggestions;
    }

    /**
     * Get collaborative filtering suggestions (what similar users did).
     */
    private List<SmartSuggestion> getCollaborativeSuggestions(UserProfile profile) {
        List<SmartSuggestion> suggestions = new ArrayList<>();

        // Find similar users based on action patterns
        List<UserProfile> similarUsers = findSimilarUsers(profile);

        if (!similarUsers.isEmpty()) {
            // Find actions that similar users do but this user doesn't
            Set<String> userActions = profile.actions.stream()
                .map(a -> a.actionType)
                .collect(Collectors.toSet());

            Map<String, Long> suggestedActions = new HashMap<>();

            for (UserProfile similarUser : similarUsers) {
                similarUser.actions.stream()
                    .filter(a -> !userActions.contains(a.actionType))
                    .forEach(a -> {
                        suggestedActions.merge(a.actionType, 1L, Long::sum);
                    });
            }

            suggestedActions.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(2)
                .forEach(entry -> {
                    suggestions.add(new SmartSuggestion(
                        "Try " + entry.getKey(),
                        "Users with similar patterns found this helpful",
                        entry.getKey(),
                        0.6 + entry.getValue() * 0.05,
                        SuggestionType.COLLABORATIVE
                    ));
                });
        }

        return suggestions;
    }

    /**
     * Predict next tasks based on user's history.
     */
    private List<SmartSuggestion> predictNextTasks(UserProfile profile) {
        List<SmartSuggestion> suggestions = new ArrayList<>();

        if (profile.actions.isEmpty()) return suggestions;

        // Get most frequent actions
        Map<String, Long> actionCounts = profile.actions.stream()
            .collect(Collectors.groupingBy(a -> a.actionType, Collectors.counting()));

        // Predict based on recency and frequency
        Map<String, Double> scores = new HashMap<>();

        for (Map.Entry<String, Long> entry : actionCounts.entrySet()) {
            String action = entry.getKey();
            long count = entry.getValue();

            // Recency score (more recent = higher score)
            long lastTime = profile.actions.stream()
                .filter(a -> a.actionType.equals(action))
                .mapToLong(a -> a.timestamp)
                .max()
                .orElse(0);
            double recencyScore = 1.0 / (1.0 + (System.currentTimeMillis() - lastTime) / 3600000.0);

            // Frequency score
            double frequencyScore = (double) count / profile.getTotalActions();

            scores.put(action, recencyScore * 0.4 + frequencyScore * 0.6);
        }

        scores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(2)
            .forEach(entry -> {
                suggestions.add(new SmartSuggestion(
                    "You might want to " + entry.getKey(),
                    "Based on your activity history",
                    entry.getKey(),
                    entry.getValue(),
                    SuggestionType.PREDICTIVE
                ));
            });

        return suggestions;
    }

    /**
     * Analyze behavior pattern and update global patterns.
     */
    private void analyzeBehaviorPattern(UserProfile profile, UserAction action) {
        List<UserAction> recent = profile.actions.stream()
            .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
            .limit(10)
            .collect(Collectors.toList());

        if (recent.size() >= 3) {
            String pattern = recent.get(2).actionType + "->" +
                           recent.get(1).actionType + "->" +
                           recent.get(0).actionType;

            globalPatterns.compute(pattern, (k, existing) -> {
                if (existing == null) {
                    return new BehaviorPattern(pattern, action.actionType, 0.5);
                } else {
                    existing.occurrences++;
                    existing.confidence = Math.min(0.95, existing.confidence + 0.05);
                    return existing;
                }
            });
        }
    }

    /**
     * Find users with similar behavior patterns.
     */
    private List<UserProfile> findSimilarUsers(UserProfile target) {
        List<UserProfile> similar = new ArrayList<>();

        for (UserProfile other : userProfiles.values()) {
            if (other.userId.equals(target.userId)) continue;

            double similarity = calculateSimilarity(target, other);
            if (similarity > 0.6) {
                similar.add(other);
            }
        }

        return similar;
    }

    /**
     * Calculate similarity between two user profiles.
     */
    private double calculateSimilarity(UserProfile a, UserProfile b) {
        if (a.actions.isEmpty() || b.actions.isEmpty()) return 0.0;

        Set<String> actionsA = a.actions.stream().map(ac -> ac.actionType).collect(Collectors.toSet());
        Set<String> actionsB = b.actions.stream().map(ac -> ac.actionType).collect(Collectors.toSet());

        // Jaccard similarity
        Set<String> intersection = new HashSet<>(actionsA);
        intersection.retainAll(actionsB);

        Set<String> union = new HashSet<>(actionsA);
        union.addAll(actionsB);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Check if a timestamp is at a similar time of day.
     */
    private boolean isSimilarTime(long timestamp, int targetHour) {
        LocalDateTime time = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC);
        int hourDiff = Math.abs(time.getHour() - targetHour);
        return hourDiff <= 2 || hourDiff >= 22; // Within 2 hours or wrap around
    }

    /**
     * Generate default suggestions for new users.
     */
    private List<SmartSuggestion> getDefaultSuggestions() {
        return List.of(
            new SmartSuggestion(
                "Try building a simple app",
                "Good starting point for new users",
                "build_simple_app",
                0.8,
                SuggestionType.DEFAULT
            ),
            new SmartSuggestion(
                "Explore available AI providers",
                "See what AI models you can use",
                "explore_providers",
                0.7,
                SuggestionType.DEFAULT
            )
        );
    }

    /**
     * Generate suggestions (background task).
     */
    private void generateSuggestions(String userId) {
        List<SmartSuggestion> suggestions = getSuggestions(userId, 5);
        UserProfile profile = userProfiles.get(userId);
        if (profile != null) {
            profile.cachedSuggestions = suggestions;
            profile.lastSuggestionUpdate = System.currentTimeMillis();
        }
    }

    /**
     * Get user profile statistics.
     */
    public Map<String, Object> getProfileStats(String userId) {
        UserProfile profile = userProfiles.get(userId);
        if (profile == null) {
            return Map.of("exists", false);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("exists", true);
        stats.put("totalActions", profile.getTotalActions());
        stats.put("mostFrequentAction", profile.getMostFrequentAction());
        stats.put("activeDays", profile.getActiveDays());
        stats.put("suggestionsCount", profile.cachedSuggestions.size());
        return stats;
    }

    // ── Data Classes ──────────────────────────────────────────────────────────

    public static class SmartSuggestion {
        public final String title;
        public final String description;
        public final String actionType;
        public final double confidence;
        public final SuggestionType type;

        public SmartSuggestion(String title, String description, String actionType,
                              double confidence, SuggestionType type) {
            this.title = title;
            this.description = description;
            this.actionType = actionType;
            this.confidence = confidence;
            this.type = type;
        }
    }

    public enum SuggestionType {
        TIME_BASED,
        PATTERN_BASED,
        COLLABORATIVE,
        PREDICTIVE,
        DEFAULT
    }

    private static class UserProfile {
        String userId;
        List<UserAction> actions = new ArrayList<>();
        List<SmartSuggestion> cachedSuggestions = new ArrayList<>();
        long lastSuggestionUpdate = 0;

        UserProfile(String userId) {
            this.userId = userId;
            this.actions = Collections.synchronizedList(new ArrayList<>());
        }

        void addAction(UserAction action) {
            actions.add(action);
            // Keep only last 1000 actions
            synchronized (actions) {
                if (actions.size() > 1000) {
                    List<UserAction> trimmed = new ArrayList<>(actions.subList(actions.size() - 1000, actions.size()));
                    actions.clear();
                    actions.addAll(trimmed);
                }
            }
        }

        int getTotalActions() {
            return actions.size();
        }

        String getMostFrequentAction() {
            synchronized (actions) {
                return actions.stream()
                    .collect(Collectors.groupingBy(a -> a.actionType, Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("none");
            }
        }

        long getActiveDays() {
            return actions.stream()
                .map(a -> LocalDateTime.ofEpochSecond(a.timestamp / 1000, 0, java.time.ZoneOffset.UTC).toLocalDate())
                .distinct()
                .count();
        }
    }

    private static class UserAction {
        String actionType;
        long timestamp;
        Map<String, Object> metadata;

        UserAction(String actionType, long timestamp, Map<String, Object> metadata) {
            this.actionType = actionType;
            this.timestamp = timestamp;
            this.metadata = metadata;
        }
    }

    private static class UserSession {
        String userId;
        List<UserAction> sessionActions = new ArrayList<>();
        long sessionStart = System.currentTimeMillis();

        UserSession(String userId) {
            this.userId = userId;
        }

        void addAction(UserAction action) {
            sessionActions.add(action);
        }
    }

    private static class BehaviorPattern {
        String pattern;
        String followUpAction;
        double confidence;
        int occurrences = 1;

        BehaviorPattern(String pattern, String followUpAction, double confidence) {
            this.pattern = pattern;
            this.followUpAction = followUpAction;
            this.confidence = confidence;
        }
    }
}
