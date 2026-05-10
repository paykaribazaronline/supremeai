package com.supremeai.ml;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Random Forest implementation for predicting system or agent failures.
 */
@Component
public class RandomForestFailurePredictor {

    private final List<DecisionTree> forest = new ArrayList<>();
    private final int numberOfTrees = 10;

    public void train(double[][] features, int[] labels) {
        forest.clear();
        for (int i = 0; i < numberOfTrees; i++) {
            DecisionTree tree = new DecisionTree();
            // Bootstrapping
            Object[] bootstrapped = bootstrap(features, labels);
            tree.train((double[][]) bootstrapped[0], (int[]) bootstrapped[1]);
            forest.add(tree);
        }
    }

    public int predict(double[] x) {
        if (forest.isEmpty()) return 0;
        Map<Integer, Integer> votes = new HashMap<>();
        for (DecisionTree tree : forest) {
            int prediction = tree.predict(x);
            votes.put(prediction, votes.getOrDefault(prediction, 0) + 1);
        }
        return votes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    private Object[] bootstrap(double[][] features, int[] labels) {
        int n = features.length;
        double[][] bFeatures = new double[n][];
        int[] bLabels = new int[n];
        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            int idx = rand.nextInt(n);
            bFeatures[i] = features[idx];
            bLabels[i] = labels[idx];
        }
        return new Object[]{bFeatures, bLabels};
    }

    private static class DecisionTree {
        private Node root;

        public void train(double[][] features, int[] labels) {
            root = buildTree(features, labels, 0);
        }

        private Node buildTree(double[][] features, int[] labels, int depth) {
            if (depth > 10 || isPure(labels)) {
                return new Node(mostFrequent(labels));
            }

            int numFeatures = features[0].length;
            int bestFeature = -1;
            double bestThreshold = 0;
            double bestGini = 1.0;

            Random rand = new Random();
            for (int i = 0; i < Math.sqrt(numFeatures); i++) {
                int fIdx = rand.nextInt(numFeatures);
                for (double[] row : features) {
                    double threshold = row[fIdx];
                    double gini = calculateGini(features, labels, fIdx, threshold);
                    if (gini < bestGini) {
                        bestGini = gini;
                        bestFeature = fIdx;
                        bestThreshold = threshold;
                    }
                }
            }

            if (bestFeature == -1) return new Node(mostFrequent(labels));

            List<double[]> leftFeatures = new ArrayList<>();
            List<Integer> leftLabels = new ArrayList<>();
            List<double[]> rightFeatures = new ArrayList<>();
            List<Integer> rightLabels = new ArrayList<>();

            for (int i = 0; i < features.length; i++) {
                if (features[i][bestFeature] < bestThreshold) {
                    leftFeatures.add(features[i]);
                    leftLabels.add(labels[i]);
                } else {
                    rightFeatures.add(features[i]);
                    rightLabels.add(labels[i]);
                }
            }

            if (leftLabels.isEmpty() || rightLabels.isEmpty()) return new Node(mostFrequent(labels));

            Node node = new Node(bestFeature, bestThreshold);
            node.left = buildTree(leftFeatures.toArray(new double[0][]), toIntArray(leftLabels), depth + 1);
            node.right = buildTree(rightFeatures.toArray(new double[0][]), toIntArray(rightLabels), depth + 1);
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

        private double calculateGini(double[][] features, int[] labels, int fIdx, double threshold) {
            int leftCount = 0, rightCount = 0;
            Map<Integer, Integer> leftLabels = new HashMap<>();
            Map<Integer, Integer> rightLabels = new HashMap<>();

            for (int i = 0; i < features.length; i++) {
                if (features[i][fIdx] < threshold) {
                    leftCount++;
                    leftLabels.put(labels[i], leftLabels.getOrDefault(labels[i], 0) + 1);
                } else {
                    rightCount++;
                    rightLabels.put(labels[i], rightLabels.getOrDefault(labels[i], 0) + 1);
                }
            }

            final int finalLeftCount = leftCount;
            final int finalRightCount = rightCount;
            double giniLeft = 1.0 - leftLabels.values().stream().mapToDouble(c -> Math.pow((double) c / finalLeftCount, 2)).sum();
            double giniRight = 1.0 - rightLabels.values().stream().mapToDouble(c -> Math.pow((double) c / finalRightCount, 2)).sum();

            return ((double) leftCount / labels.length) * giniLeft + ((double) rightCount / labels.length) * giniRight;
        }

        private boolean isPure(int[] labels) {
            for (int i = 1; i < labels.length; i++) if (labels[i] != labels[0]) return false;
            return true;
        }

        private int mostFrequent(int[] labels) {
            Map<Integer, Integer> counts = new HashMap<>();
            for (int l : labels) counts.put(l, counts.getOrDefault(l, 0) + 1);
            return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0);
        }

        private int[] toIntArray(List<Integer> list) {
            return list.stream().mapToInt(i -> i).toArray();
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
}
