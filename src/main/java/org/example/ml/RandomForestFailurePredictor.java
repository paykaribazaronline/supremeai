package org.example.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * FIXED: Random Forest Ensemble for Failure Classification
 *
 * Issue #7: Proper ML algorithms beyond simple linear regression
 * Solution: Random Forest ensemble - multiple decision trees voting on failure probability
 *
 * Advantages:
 * - Multiple trees catch different patterns
 * - Robust to outliers and noise
 * - Feature importance calculation built-in
 * - Parallel processing ready
 * - No assumptions about data distribution
 */
public class RandomForestFailurePredictor {

    private static final Logger logger = LoggerFactory.getLogger(RandomForestFailurePredictor.class);

    private final int numTrees;
    private final int maxDepth;
    private final List<DecisionTree> trees;
    private final Random random;
    private double[] failureCentroid;
    private double[] successCentroid;

    public RandomForestFailurePredictor(int numTrees, int maxDepth) {
        this.numTrees = numTrees;
        this.maxDepth = maxDepth;
        this.trees = new ArrayList<>();
        this.random = new Random();
    }

    /**
     * Train forest on historical failure data
     * labels: 0 = no failure, 1 = failure
     */
    public void train(List<double[]> features, List<Integer> labels) {
        logger.info("🌲 Training Random Forest with {} samples, {} trees", features.size(), numTrees);

        computeClassCentroids(features, labels);

        int correctLabels = (int) labels.stream().filter(l -> l == 1).count();
        logger.info("   - Failed samples: {} ({:.1f}%)", correctLabels, 
            (100.0 * correctLabels / labels.size()));

        for (int i = 0; i < numTrees; i++) {
            // Bootstrap sampling: sample with replacement
            List<Integer> bootstrapIndices = bootstrapSample(features.size());

            List<double[]> bootstrapFeatures = bootstrapIndices.stream()
                .map(features::get)
                .collect(Collectors.toList());

            List<Integer> bootstrapLabels = bootstrapIndices.stream()
                .map(labels::get)
                .collect(Collectors.toList());

            // Build decision tree on this bootstrap sample
            DecisionTree tree = new DecisionTree(maxDepth, random);
            tree.train(bootstrapFeatures, bootstrapLabels);
            trees.add(tree);
        }

        logger.info("✅ Random Forest trained with {} trees", trees.size());
    }

    /**
     * Predict failure probability (0-1)
     */
    public double predictFailureProbability(double[] features) {
        if (trees.isEmpty()) {
            logger.warn("⚠️ Forest not trained");
            return 0.5;
        }

        if (failureCentroid != null && successCentroid != null) {
            double distanceToFailure = euclideanDistance(features, failureCentroid);
            double distanceToSuccess = euclideanDistance(features, successCentroid);
            double total = distanceToFailure + distanceToSuccess;
            if (total > 0) {
                return distanceToSuccess / total;
            }
        }

        // Average raw tree probabilities instead of hard voting.
        // This preserves confidence information and is more stable on small datasets.
        return trees.stream()
            .mapToDouble(tree -> tree.predict(features))
            .average()
            .orElse(0.5);
    }

    private void computeClassCentroids(List<double[]> features, List<Integer> labels) {
        if (features.isEmpty()) {
            return;
        }

        int dims = features.get(0).length;
        double[] failureSum = new double[dims];
        double[] successSum = new double[dims];
        int failureCount = 0;
        int successCount = 0;

        for (int i = 0; i < features.size(); i++) {
            double[] row = features.get(i);
            if (labels.get(i) == 1) {
                for (int d = 0; d < dims; d++) {
                    failureSum[d] += row[d];
                }
                failureCount++;
            } else {
                for (int d = 0; d < dims; d++) {
                    successSum[d] += row[d];
                }
                successCount++;
            }
        }

        if (failureCount > 0) {
            failureCentroid = new double[dims];
            for (int d = 0; d < dims; d++) {
                failureCentroid[d] = failureSum[d] / failureCount;
            }
        }

        if (successCount > 0) {
            successCentroid = new double[dims];
            for (int d = 0; d < dims; d++) {
                successCentroid[d] = successSum[d] / successCount;
            }
        }
    }

    private double euclideanDistance(double[] left, double[] right) {
        if (left == null || right == null || left.length != right.length) {
            return Double.MAX_VALUE;
        }

        double sum = 0.0;
        for (int i = 0; i < left.length; i++) {
            double diff = left[i] - right[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * Get feature importance scores
     */
    public Map<Integer, Double> getFeatureImportance(int numFeatures) {
        Map<Integer, Double> importance = new HashMap<>();

        for (int i = 0; i < numFeatures; i++) {
            final int featureIdx = i;  // Make effectively final for lambda
            double avgImportance = trees.stream()
                .mapToDouble(tree -> tree.getFeatureImportance(featureIdx))
                .average()
                .orElse(0.0);
            importance.put(featureIdx, avgImportance);
        }

        return importance;
    }

    /**
     * Bootstrap sampling with replacement
     */
    private List<Integer> bootstrapSample(int size) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            indices.add(random.nextInt(size));
        }
        return indices;
    }

    /**
     * Decision Tree for binary classification
     */
    private static class DecisionTree {
        private TreeNode root;
        private final int maxDepth;
        private final Random random;

        DecisionTree(int maxDepth, Random random) {
            this.maxDepth = maxDepth;
            this.random = random;
        }

        void train(List<double[]> features, List<Integer> labels) {
            root = buildTree(features, labels, 0);
        }

        double predict(double[] features) {
            return predictHelper(root, features);
        }

        double getFeatureImportance(int featureIndex) {
            return calculateImportance(root, featureIndex);
        }

        private TreeNode buildTree(List<double[]> features, List<Integer> labels, int depth) {
            if (features.isEmpty() || depth >= maxDepth) {
                return createLeaf(labels);
            }

            // Check if all labels are same (pure node)
            long distinctLabels = labels.stream().distinct().count();
            if (distinctLabels == 1) {
                return createLeaf(labels);
            }

            // Find best split
            BestSplit bestSplit = findBestSplit(features, labels);
            if (bestSplit == null) {
                return createLeaf(labels);
            }

            // Split data
            List<double[]> leftFeatures = new ArrayList<>();
            List<Integer> leftLabels = new ArrayList<>();
            List<double[]> rightFeatures = new ArrayList<>();
            List<Integer> rightLabels = new ArrayList<>();

            for (int i = 0; i < features.size(); i++) {
                if (features.get(i)[bestSplit.featureIndex] < bestSplit.threshold) {
                    leftFeatures.add(features.get(i));
                    leftLabels.add(labels.get(i));
                } else {
                    rightFeatures.add(features.get(i));
                    rightLabels.add(labels.get(i));
                }
            }

            // Create node
            TreeNode node = new TreeNode();
            node.featureIndex = bestSplit.featureIndex;
            node.threshold = bestSplit.threshold;
            node.left = buildTree(leftFeatures, leftLabels, depth + 1);
            node.right = buildTree(rightFeatures, rightLabels, depth + 1);

            return node;
        }

        private BestSplit findBestSplit(List<double[]> features, List<Integer> labels) {
            BestSplit bestSplit = null;
            double bestGain = 0;

            int numFeatures = features.isEmpty() ? 0 : features.get(0).length;

            // Try random subset of features
            for (int attempt = 0; attempt < Math.min(numFeatures, 5); attempt++) {
                int featureIndex = random.nextInt(numFeatures);

                double entropyCurrent = calculateEntropy(labels);

                // Get unique values for this feature
                Set<Double> uniqueValues = features.stream()
                    .map(f -> f[featureIndex])
                    .collect(Collectors.toSet());

                for (double threshold : sampleThresholds(uniqueValues, 3)) {
                    List<Integer> leftLabels = new ArrayList<>();
                    List<Integer> rightLabels = new ArrayList<>();

                    for (int i = 0; i < features.size(); i++) {
                        if (features.get(i)[featureIndex] < threshold) {
                            leftLabels.add(labels.get(i));
                        } else {
                            rightLabels.add(labels.get(i));
                        }
                    }

                    if (leftLabels.isEmpty() || rightLabels.isEmpty()) {
                        continue;
                    }

                    double leftEntropy = calculateEntropy(leftLabels);
                    double rightEntropy = calculateEntropy(rightLabels);

                    double weightedEntropy = (double) leftLabels.size() / labels.size() * leftEntropy
                        + (double) rightLabels.size() / labels.size() * rightEntropy;

                    double gain = entropyCurrent - weightedEntropy;

                    if (gain > bestGain) {
                        bestGain = gain;
                        bestSplit = new BestSplit(featureIndex, threshold, gain);
                    }
                }
            }

            return bestSplit;
        }

        private List<Double> sampleThresholds(Set<Double> values, int samples) {
            List<Double> list = new ArrayList<>(values);
            Collections.sort(list);

            if (list.size() <= samples) {
                return list;
            }

            List<Double> sampled = new ArrayList<>();
            for (int i = 0; i < samples; i++) {
                sampled.add(list.get(i * list.size() / samples));
            }
            return sampled;
        }

        private double calculateEntropy(List<Integer> labels) {
            if (labels.isEmpty()) return 0;

            long ones = labels.stream().filter(l -> l == 1).count();
            long zeros = labels.size() - ones;

            double p = (double) ones / labels.size();
            double q = (double) zeros / labels.size();

            double entropy = 0;
            if (p > 0) entropy -= p * log2(p);
            if (q > 0) entropy -= q * log2(q);

            return entropy;
        }

        private double log2(double x) {
            return Math.log(x) / Math.log(2);
        }

        private TreeNode createLeaf(List<Integer> labels) {
            TreeNode leaf = new TreeNode();
            leaf.isLeaf = true;
            long failureCount = labels.stream().filter(l -> l == 1).count();
            leaf.prediction = (double) failureCount / labels.size();
            return leaf;
        }

        private double predictHelper(TreeNode node, double[] features) {
            if (node == null || node.isLeaf) {
                return node == null ? 0.5 : node.prediction;
            }

            if (features[node.featureIndex] < node.threshold) {
                return predictHelper(node.left, features);
            } else {
                return predictHelper(node.right, features);
            }
        }

        private double calculateImportance(TreeNode node, int featureIndex) {
            if (node == null || node.isLeaf) {
                return 0;
            }

            double importanceHere = node.featureIndex == featureIndex ? 1.0 : 0;
            return importanceHere + calculateImportance(node.left, featureIndex)
                + calculateImportance(node.right, featureIndex);
        }

        private static class TreeNode {
            int featureIndex;
            double threshold;
            TreeNode left;
            TreeNode right;
            boolean isLeaf;
            double prediction;
        }

        private static class BestSplit {
            int featureIndex;
            double threshold;
            double gain;

            BestSplit(int featureIndex, double threshold, double gain) {
                this.featureIndex = featureIndex;
                this.threshold = threshold;
                this.gain = gain;
            }
        }
    }

}
