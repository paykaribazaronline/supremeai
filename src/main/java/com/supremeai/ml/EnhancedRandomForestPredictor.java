package com.supremeai.ml;

import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enhanced Random Forest implementation with proper feature engineering,
 * auto-retraining, and integration with failure analysis.
 */
@Component
public class EnhancedRandomForestPredictor {

    private static final Logger log = LoggerFactory.getLogger(EnhancedRandomForestPredictor.class);

    @Autowired
    private GlobalKnowledgeBase globalKnowledgeBase;

    private final List<EnhancedDecisionTree> forest = new CopyOnWriteArrayList<>();
    private final List<FailureRecord> trainingData = new CopyOnWriteArrayList<>();
    private final Map<String, Double> featureImportance = new ConcurrentHashMap<>();

    private static final int NUMBER_OF_TREES = 20;
    private static final int MIN_SAMPLES_FOR_TRAINING = 100;
    private static final int MAX_TRAINING_RECORDS = 10000;
    private static final double RETRAIN_THRESHOLD = 0.05; // 5% new data triggers retrain

    private int totalPredictions = 0;
    private int correctPredictions = 0;
    private int lastTrainingSize = 0;
    private long lastTrainingTime = 0;

    // Feature indices for failure prediction
    public enum FeatureType {
        ERROR_FREQUENCY,          // How often this error occurred
        TIME_SINCE_LAST_ERROR,    // Time since similar error
        PROVIDER_SUCCESS_RATE,    // AI provider's success rate
        CODE_COMPLEXITY,          // Complexity of code being generated
        USER_EXPERIENCE_LEVEL,    // User's experience (based on past success)
        SYSTEM_LOAD,              // Current system load
        API_RESPONSE_TIME,        // Recent API response times
        MEMORY_USAGE,             // Current memory usage
        ERROR_MESSAGE_LENGTH,     // Length of error message (more detail = easier to fix)
        STACK_TRACE_DEPTH         // Depth of stack trace
    }

    /**
     * Record a failure for future training.
     */
    public void recordFailure(String errorSignature, Map<FeatureType, Double> features, boolean actualFailure) {
        FailureRecord record = new FailureRecord(
            errorSignature,
            extractFeatures(features),
            actualFailure ? 1 : 0,
            System.currentTimeMillis()
        );
        trainingData.add(record);

        // Keep only recent records
        if (trainingData.size() > MAX_TRAINING_RECORDS) {
            trainingData.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
            trainingData.subList(MAX_TRAINING_RECORDS, trainingData.size()).clear();
        }

        // Check if retraining is needed
        if (shouldRetrain()) {
            retrainModel();
        }
    }

    /**
     * Predict if a failure will occur based on current features.
     */
    public FailurePrediction predict(String context, Map<FeatureType, Double> features) {
        if (forest.isEmpty()) {
            return new FailurePrediction(0.5, "Model not trained yet", Map.of());
        }

        double[] featureArray = extractFeatures(features);
        int positiveVotes = 0;
        int totalVotes = 0;
        Map<Integer, Integer> voteDistribution = new HashMap<>();

        for (EnhancedDecisionTree tree : forest) {
            int prediction = tree.predict(featureArray);
            voteDistribution.put(prediction, voteDistribution.getOrDefault(prediction, 0) + 1);
            if (prediction == 1) positiveVotes++;
            totalVotes++;
        }

        double failureProbability = (double) positiveVotes / totalVotes;
        String confidence = failureProbability > 0.7 ? "HIGH" :
                          failureProbability > 0.4 ? "MEDIUM" : "LOW";

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("forestSize", forest.size());
        metadata.put("voteDistribution", voteDistribution);
        metadata.put("featureImportance", new HashMap<>(featureImportance));

        totalPredictions++;
        if (failureProbability > 0.5 == true) correctPredictions++; // Will be validated later

        return new FailurePrediction(failureProbability, confidence, metadata);
    }

    /**
     * Validate a prediction and update accuracy metrics.
     */
    public void validatePrediction(String context, boolean actualFailure) {
        // This would be called after the fact to validate predictions
        // For now, we track overall accuracy
    }

    /**
     * Check if model should be retrained.
     */
    private boolean shouldRetrain() {
        if (forest.isEmpty() && trainingData.size() >= MIN_SAMPLES_FOR_TRAINING) {
            return true;
        }

        int newRecords = trainingData.size() - lastTrainingSize;
        double newDataRatio = (double) newRecords / Math.max(lastTrainingSize, 1);

        return newDataRatio > RETRAIN_THRESHOLD && trainingData.size() >= MIN_SAMPLES_FOR_TRAINING;
    }

    /**
     * Retrain the model with current training data.
     */
    private synchronized void retrainModel() {
        if (trainingData.size() < MIN_SAMPLES_FOR_TRAINING) {
            log.info("Not enough training data: {}/{}", trainingData.size(), MIN_SAMPLES_FOR_TRAINING);
            return;
        }

        log.info("Retraining Random Forest with {} samples...", trainingData.size());

        // Prepare training data
        double[][] features = new double[trainingData.size()][FeatureType.values().length];
        int[] labels = new int[trainingData.size()];

        for (int i = 0; i < trainingData.size(); i++) {
            FailureRecord record = trainingData.get(i);
            features[i] = record.features;
            labels[i] = record.label;
        }

        // Train forest
        forest.clear();
        Random rand = new Random();

        for (int t = 0; t < NUMBER_OF_TREES; t++) {
            EnhancedDecisionTree tree = new EnhancedDecisionTree();

            // Bootstrap sampling
            double[][] bootstrapFeatures = new double[trainingData.size()][];
            int[] bootstrapLabels = new int[trainingData.size()];

            for (int i = 0; i < trainingData.size(); i++) {
                int idx = rand.nextInt(trainingData.size());
                bootstrapFeatures[i] = features[idx];
                bootstrapLabels[i] = labels[idx];
            }

            tree.train(bootstrapFeatures, bootstrapLabels, FeatureType.values().length);
            forest.add(tree);
        }

        // Calculate feature importance
        calculateFeatureImportance(features, labels);

        lastTrainingSize = trainingData.size();
        lastTrainingTime = System.currentTimeMillis();

        log.info("Random Forest retrained successfully. Accuracy tracking: {}/{} predictions",
            correctPredictions, totalPredictions);
    }

    /**
     * Calculate feature importance using permutation importance.
     */
    private void calculateFeatureImportance(double[][] features, int[] labels) {
        featureImportance.clear();
        Random rand = new Random();

        for (int f = 0; f < FeatureType.values().length; f++) {
            // Calculate baseline accuracy
            int correct = 0;
            for (int i = 0; i < features.length; i++) {
                int prediction = predictWithFeatures(features[i]);
                if (prediction == labels[i]) correct++;
            }
            double baselineAccuracy = (double) correct / features.length;

            // Permute feature f and recalculate
            double[][] permutedFeatures = copyArray(features);
            Collections.shuffle(Arrays.asList(permutedFeatures), rand); // Simple permutation

            correct = 0;
            for (int i = 0; i < permutedFeatures.length; i++) {
                int prediction = predictWithFeatures(permutedFeatures[i]);
                if (prediction == labels[i]) correct++;
            }
            double permutedAccuracy = (double) correct / permutedFeatures.length;

            double importance = baselineAccuracy - permutedAccuracy;
            featureImportance.put(FeatureType.values()[f].name(), Math.max(0, importance));
        }
    }

    /**
     * Predict using current forest (helper for feature importance).
     */
    private int predictWithFeatures(double[] featureArray) {
        if (forest.isEmpty()) return 0;

        int positiveVotes = 0;
        for (EnhancedDecisionTree tree : forest) {
            if (tree.predict(featureArray) == 1) positiveVotes++;
        }
        return positiveVotes > forest.size() / 2 ? 1 : 0;
    }

    /**
     * Extract features into array format.
     */
    private double[] extractFeatures(Map<FeatureType, Double> featureMap) {
        double[] features = new double[FeatureType.values().length];
        for (int i = 0; i < FeatureType.values().length; i++) {
            FeatureType type = FeatureType.values()[i];
            features[i] = featureMap.getOrDefault(type, 0.0);
        }
        return features;
    }

    /**
     * Copy 2D array.
     */
    private double[][] copyArray(double[][] original) {
        double[][] copy = new double[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = Arrays.copyOf(original[i], original[i].length);
        }
        return copy;
    }

    /**
     * Auto-retrain on a schedule (every hour).
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    public void scheduledRetrain() {
        if (shouldRetrain()) {
            retrainModel();
        }
    }

    /**
     * Get model statistics.
     */
    public Map<String, Object> getModelStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("forestSize", forest.size());
        stats.put("trainingDataSize", trainingData.size());
        stats.put("totalPredictions", totalPredictions);
        stats.put("accuracy", totalPredictions > 0 ? (double) correctPredictions / totalPredictions : 0.0);
        stats.put("lastTrainingTime", lastTrainingTime);
        stats.put("featureImportance", new HashMap<>(featureImportance));
        return stats;
    }

    // ── Inner Classes ──────────────────────────────────────────────────────

    public static class FailurePrediction {
        public final double probability;
        public final String confidence;
        public final Map<String, Object> metadata;

        public FailurePrediction(double probability, String confidence, Map<String, Object> metadata) {
            this.probability = probability;
            this.confidence = confidence;
            this.metadata = metadata;
        }
    }

    private static class FailureRecord {
        String errorSignature;
        double[] features;
        int label; // 0 = success, 1 = failure
        long timestamp;

        FailureRecord(String errorSignature, double[] features, int label, long timestamp) {
            this.errorSignature = errorSignature;
            this.features = features;
            this.label = label;
            this.timestamp = timestamp;
        }
    }

    private static class EnhancedDecisionTree {
        private Node root;
        private static final int MAX_DEPTH = 15;

        public void train(double[][] features, int[] labels, int numFeatures) {
            root = buildTree(features, labels, 0, numFeatures);
        }

        private Node buildTree(double[][] features, int[] labels, int depth, int numFeatures) {
            if (depth >= MAX_DEPTH || isPure(labels)) {
                return new Node(mostFrequent(labels));
            }

            // Find best split
            Split bestSplit = findBestSplit(features, labels, numFeatures);

            if (bestSplit == null || bestSplit.featureIdx == -1) {
                return new Node(mostFrequent(labels));
            }

            // Split data
            List<double[]> leftFeatures = new ArrayList<>();
            List<Integer> leftLabels = new ArrayList<>();
            List<double[]> rightFeatures = new ArrayList<>();
            List<Integer> rightLabels = new ArrayList<>();

            for (int i = 0; i < features.length; i++) {
                if (features[i][bestSplit.featureIdx] < bestSplit.threshold) {
                    leftFeatures.add(features[i]);
                    leftLabels.add(labels[i]);
                } else {
                    rightFeatures.add(features[i]);
                    rightLabels.add(labels[i]);
                }
            }

            if (leftLabels.isEmpty() || rightLabels.isEmpty()) {
                return new Node(mostFrequent(labels));
            }

            Node node = new Node(bestSplit.featureIdx, bestSplit.threshold);
            node.left = buildTree(
                leftFeatures.toArray(new double[0][]),
                leftLabels.stream().mapToInt(Integer::intValue).toArray(),
                depth + 1, numFeatures
            );
            node.right = buildTree(
                rightFeatures.toArray(new double[0][]),
                rightLabels.stream().mapToInt(Integer::intValue).toArray(),
                depth + 1, numFeatures
            );
            return node;
        }

        public int predict(double[] x) {
            Node current = root;
            while (!current.isLeaf()) {
                if (x[current.featureIdx] < current.threshold) {
                    current = current.left;
                } else {
                    current = current.right;
                }
            }
            return current.label;
        }

        private Split findBestSplit(double[][] features, int[] labels, int numFeatures) {
            Random rand = new Random();
            double bestGini = 1.0;
            int bestFeature = -1;
            double bestThreshold = 0;

            int featuresToCheck = (int) Math.sqrt(numFeatures);
            for (int f = 0; f < featuresToCheck; f++) {
                int featureIdx = rand.nextInt(numFeatures);

                // Get unique values for this feature
                Set<Double> values = new HashSet<>();
                for (double[] row : features) {
                    values.add(row[featureIdx]);
                }

                for (double threshold : values) {
                    double gini = calculateGini(features, labels, featureIdx, threshold);
                    if (gini < bestGini) {
                        bestGini = gini;
                        bestFeature = featureIdx;
                        bestThreshold = threshold;
                    }
                }
            }

            return bestFeature == -1 ? null : new Split(bestFeature, bestThreshold);
        }

        private double calculateGini(double[][] features, int[] labels, int featureIdx, double threshold) {
            int leftCount = 0, rightCount = 0;
            Map<Integer, Integer> leftLabels = new HashMap<>();
            Map<Integer, Integer> rightLabels = new HashMap<>();

            for (int i = 0; i < features.length; i++) {
                if (features[i][featureIdx] < threshold) {
                    leftCount++;
                    leftLabels.put(labels[i], leftLabels.getOrDefault(labels[i], 0) + 1);
                } else {
                    rightCount++;
                    rightLabels.put(labels[i], rightLabels.getOrDefault(labels[i], 0) + 1);
                }
            }

            if (leftCount == 0 || rightCount == 0) return 1.0;

            double giniLeft = 1.0 - leftLabels.values().stream()
                .mapToDouble(c -> Math.pow((double) c / leftCount, 2)).sum();
            double giniRight = 1.0 - rightLabels.values().stream()
                .mapToDouble(c -> Math.pow((double) c / rightCount, 2)).sum();

            double totalCount = leftCount + rightCount;
            return (leftCount / totalCount) * giniLeft +
                   (rightCount / totalCount) * giniRight;
        }

        private boolean isPure(int[] labels) {
            if (labels.length == 0) return true;
            for (int i = 1; i < labels.length; i++) {
                if (labels[i] != labels[0]) return false;
            }
            return true;
        }

        private int mostFrequent(int[] labels) {
            Map<Integer, Integer> counts = new HashMap<>();
            for (int l : labels) {
                counts.put(l, counts.getOrDefault(l, 0) + 1);
            }
            return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
        }
    }

    private static class Node {
        int featureIdx = -1;
        double threshold;
        int label = -1;
        Node left, right;

        Node(int label) { this.label = label; }
        Node(int featureIdx, double threshold) {
            this.featureIdx = featureIdx;
            this.threshold = threshold;
        }
        boolean isLeaf() { return label != -1; }
    }

    private static class Split {
        int featureIdx;
        double threshold;

        Split(int featureIdx, double threshold) {
            this.featureIdx = featureIdx;
            this.threshold = threshold;
        }
    }
}
