package com.supremeai.ml;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Isolation Forest implementation for anomaly detection.
 * Used to detect unusual usage patterns or potential security threats.
 */
@Component
public class IsolationForest {

    private final List<ITree> forest = new ArrayList<>();
    private final int numberOfTrees = 100;
    private final int sampleSize = 256;

    public void train(double[][] data) {
        forest.clear();
        int n = data.length;
        if (n == 0) return;

        for (int i = 0; i < numberOfTrees; i++) {
            double[][] sample = getSubSample(data, sampleSize);
            forest.add(new ITree(sample, 0, (int) Math.ceil(Math.log(sampleSize) / Math.log(2))));
        }
    }

    public double computeAnomalyScore(double[] x) {
        if (forest.isEmpty()) return 0.5;
        double avgPathLength = 0;
        for (ITree tree : forest) {
            avgPathLength += tree.getPathLength(x, 0);
        }
        avgPathLength /= forest.size();
        return Math.pow(2, -avgPathLength / c(sampleSize));
    }

    private double c(int n) {
        if (n <= 1) return 0;
        if (n == 2) return 1;
        return 2 * (Math.log(n - 1) + 0.5772156649) - (2.0 * (n - 1) / n);
    }

    private double[][] getSubSample(double[][] data, int size) {
        int n = data.length;
        int actualSize = Math.min(n, size);
        double[][] sample = new double[actualSize][];
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) indices.add(i);
        Collections.shuffle(indices);
        for (int i = 0; i < actualSize; i++) {
            sample[i] = data[indices.get(i)];
        }
        return sample;
    }

    private static class ITree {
        private final Node root;

        public ITree(double[][] data, int currentHeight, int maxDepth) {
            this.root = buildTree(data, currentHeight, maxDepth);
        }

        private Node buildTree(double[][] data, int currentHeight, int maxDepth) {
            if (currentHeight >= maxDepth || data.length <= 1) {
                return new Node(data.length);
            }

            int numFeatures = data[0].length;
            int splitFeature = new Random().nextInt(numFeatures);
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;

            for (double[] row : data) {
                min = Math.min(min, row[splitFeature]);
                max = Math.max(max, row[splitFeature]);
            }

            if (min == max) return new Node(data.length);

            double splitValue = min + new Random().nextDouble() * (max - min);
            List<double[]> leftData = new ArrayList<>();
            List<double[]> rightData = new ArrayList<>();

            for (double[] row : data) {
                if (row[splitFeature] < splitValue) leftData.add(row);
                else rightData.add(row);
            }

            Node node = new Node(splitFeature, splitValue);
            node.left = buildTree(leftData.toArray(new double[0][]), currentHeight + 1, maxDepth);
            node.right = buildTree(rightData.toArray(new double[0][]), currentHeight + 1, maxDepth);
            return node;
        }

        public double getPathLength(double[] x, int currentHeight) {
            return traverse(root, x, currentHeight);
        }

        private double traverse(Node node, double[] x, int height) {
            if (node.isLeaf()) {
                return height + c(node.size);
            }
            if (x[node.splitFeature] < node.splitValue) {
                return traverse(node.left, x, height + 1);
            } else {
                return traverse(node.right, x, height + 1);
            }
        }

        private double c(int n) {
            if (n <= 1) return 0;
            if (n == 2) return 1;
            return 2 * (Math.log(n - 1) + 0.5772156649) - (2.0 * (n - 1) / n);
        }
    }

    private static class Node {
        int splitFeature;
        double splitValue;
        int size;
        Node left, right;

        Node(int size) { this.size = size; }
        Node(int splitFeature, double splitValue) {
            this.splitFeature = splitFeature;
            this.splitValue = splitValue;
        }
        boolean isLeaf() { return left == null && right == null; }
    }
}
