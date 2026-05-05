package com.supremeai.codeflow.analyzer;

import com.supremeai.codeflow.model.CodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dependency graph analyzer
 * Builds call graphs and computes blast radius
 */
@Component
public class DependencyAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalyzer.class);
    
    /**
     * Build dependency graph from code files
     */
    public CodeRepository.DependencyGraph buildDependencyGraph(List<CodeRepository.CodeFile> files) {
        List<CodeRepository.DependencyGraph.Node> nodes = new ArrayList<>();
        List<CodeRepository.DependencyGraph.Edge> edges = new ArrayList<>();
        Map<String, Double> centralityScores = new HashMap<>();
        
        // Create nodes for each file
        for (CodeRepository.CodeFile file : files) {
            CodeRepository.DependencyGraph.Node node = createNode(file);
            nodes.add(node);
        }
        
        // Create edges from call references
        for (CodeRepository.CodeFile file : files) {
            if (file.getCallReferences() != null) {
                for (CodeRepository.CallReference ref : file.getCallReferences()) {
                    CodeRepository.DependencyGraph.Edge edge = createEdge(ref, files);
                    if (edge != null) {
                        edges.add(edge);
                    }
                }
            }
        }
        
        // Calculate centrality scores
        centralityScores = calculateCentralityScores(nodes, edges);
        
        // Update nodes with centrality scores
        for (CodeRepository.DependencyGraph.Node node : nodes) {
            Double score = centralityScores.get(node.getId());
            if (score != null) {
                node.setCentralityScore(score);
            }
        }
        
        // Find critical path
        List<String> criticalPath = findCriticalPath(nodes, edges);
        
        // Calculate blast radius
        int blastRadius = calculateBlastRadius(nodes, edges);
        
        return CodeRepository.DependencyGraph.builder()
            .nodes(nodes)
            .edges(edges)
            .centralityScores(centralityScores)
            .criticalPath(criticalPath)
            .blastRadius(blastRadius)
            .build();
    }
    
    /**
     * Create node from code file
     */
    private CodeRepository.DependencyGraph.Node createNode(CodeRepository.CodeFile file) {
        int complexity = file.getComplexity();
        int fanIn = 0;
        int fanOut = 0;
        
        if (file.getCallReferences() != null) {
            fanOut = (int) file.getCallReferences().stream()
                .filter(ref -> ref.getFromFunction() != null)
                .count();
        }
        
        return CodeRepository.DependencyGraph.Node.builder()
            .id(file.getPath())
            .file(file.getPath())
            .type("FILE")
            .linesOfCode(file.getLinesOfCode())
            .complexity(complexity)
            .fanIn(fanIn)
            .fanOut(fanOut)
            .centralityScore(0.0)
            .metadata(new HashMap<>())
            .build();
    }
    
    /**
     * Create edge from call reference
     */
    private CodeRepository.DependencyGraph.Edge createEdge(
            CodeRepository.CallReference ref, List<CodeRepository.CodeFile> files) {
        
        String sourceFile = findFileForFunction(ref.getFromFunction(), files);
        String targetFile = findFileForFunction(ref.getToFunction(), files);
        
        if (sourceFile == null || targetFile == null) {
            return null;
        }
        
        return CodeRepository.DependencyGraph.Edge.builder()
            .source(sourceFile)
            .target(targetFile)
            .type("CALLS")
            .weight(1)
            .line(ref.getLine())
            .build();
    }
    
    /**
     * Find file containing function
     */
    private String findFileForFunction(String functionName, List<CodeRepository.CodeFile> files) {
        for (CodeRepository.CodeFile file : files) {
            if (file.getFunctions() != null) {
                boolean found = file.getFunctions().stream()
                    .anyMatch(f -> f.getName().equals(functionName));
                if (found) {
                    return file.getPath();
                }
            }
        }
        return null;
    }
    
    /**
     * Calculate centrality scores using PageRank-like algorithm
     */
    private Map<String, Double> calculateCentralityScores(
            List<CodeRepository.DependencyGraph.Node> nodes,
            List<CodeRepository.DependencyGraph.Edge> edges) {
        
        Map<String, Double> scores = new HashMap<>();
        Map<String, List<String>> incomingEdges = new HashMap<>();
        Map<String, List<String>> outgoingEdges = new HashMap<>();
        
        // Initialize
        for (CodeRepository.DependencyGraph.Node node : nodes) {
            scores.put(node.getId(), 1.0);
            incomingEdges.put(node.getId(), new ArrayList<>());
            outgoingEdges.put(node.getId(), new ArrayList<>());
        }
        
        // Build adjacency lists
        for (CodeRepository.DependencyGraph.Edge edge : edges) {
            outgoingEdges.get(edge.getSource()).add(edge.getTarget());
            incomingEdges.get(edge.getTarget()).add(edge.getSource());
        }
        
        // Iterative calculation (simplified PageRank)
        for (int iteration = 0; iteration < 20; iteration++) {
            Map<String, Double> newScores = new HashMap<>();
            
            for (CodeRepository.DependencyGraph.Node node : nodes) {
                String nodeId = node.getId();
                double score = 0.15; // Damping factor
                
                // Add contributions from incoming edges
                for (String incoming : incomingEdges.get(nodeId)) {
                    double incomingScore = scores.get(incoming);
                    int outDegree = outgoingEdges.get(incoming).size();
                    if (outDegree > 0) {
                        score += 0.85 * (incomingScore / outDegree);
                    }
                }
                
                newScores.put(nodeId, score);
            }
            
            scores = newScores;
        }
        
        // Normalize scores
        double maxScore = scores.values().stream()
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(1.0);
        
        if (maxScore > 0) {
            for (String nodeId : scores.keySet()) {
                scores.put(nodeId, scores.get(nodeId) / maxScore);
            }
        }
        
        return scores;
    }
    
    /**
     * Find critical path in dependency graph
     */
    private List<String> findCriticalPath(
            List<CodeRepository.DependencyGraph.Node> nodes,
            List<CodeRepository.DependencyGraph.Edge> edges) {
        
        // Simple heuristic: nodes with highest centrality and fan-out
        return nodes.stream()
            .sorted((n1, n2) -> {
                double score1 = n1.getCentralityScore() * (1 + n1.getFanOut() * 0.1);
                double score2 = n2.getCentralityScore() * (1 + n2.getFanOut() * 0.1);
                return Double.compare(score2, score1);
            })
            .limit(5)
            .map(CodeRepository.DependencyGraph.Node::getId)
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate blast radius (max impact of a change)
     */
    private int calculateBlastRadius(
            List<CodeRepository.DependencyGraph.Node> nodes,
            List<CodeRepository.DependencyGraph.Edge> edges) {
        
        if (nodes.isEmpty()) {
            return 0;
        }
        
        // Build adjacency map
        Map<String, List<String>> adjacency = new HashMap<>();
        for (CodeRepository.DependencyGraph.Node node : nodes) {
            adjacency.put(node.getId(), new ArrayList<>());
        }
        
        for (CodeRepository.DependencyGraph.Edge edge : edges) {
            adjacency.get(edge.getSource()).add(edge.getTarget());
        }
        
        // Find maximum reachable nodes from any single node
        int maxReachable = 0;
        
        for (CodeRepository.DependencyGraph.Node node : nodes) {
            Set<String> visited = new HashSet<>();
            int reachable = dfs(node.getId(), adjacency, visited);
            maxReachable = Math.max(maxReachable, reachable);
        }
        
        return maxReachable;
    }
    
    /**
     * Depth-first search for blast radius calculation
     */
    private int dfs(String nodeId, Map<String, List<String>> adjacency, Set<String> visited) {
        if (visited.contains(nodeId)) {
            return 0;
        }
        
        visited.add(nodeId);
        int count = 1;
        
        for (String neighbor : adjacency.getOrDefault(nodeId, new ArrayList<>())) {
            count += dfs(neighbor, adjacency, visited);
        }
        
        return count;
    }
    
    /**
     * Detect circular dependencies
     */
    public List<CodeRepository.CircularDependency> detectCircularDependencies(
            CodeRepository.DependencyGraph graph) {
        
        List<CodeRepository.CircularDependency> circularDeps = new ArrayList<>();
        
        if (graph == null || graph.getEdges() == null) {
            return circularDeps;
        }
        
        // Build adjacency map
        Map<String, List<String>> adjacency = new HashMap<>();
        for (CodeRepository.DependencyGraph.Node node : graph.getNodes()) {
            adjacency.put(node.getId(), new ArrayList<>());
        }
        
        for (CodeRepository.DependencyGraph.Edge edge : graph.getEdges()) {
            adjacency.get(edge.getSource()).add(edge.getTarget());
        }
        
        // Detect cycles using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (CodeRepository.DependencyGraph.Node node : graph.getNodes()) {
            if (!visited.contains(node.getId())) {
                List<String> cycle = new ArrayList<>();
                if (detectCycle(node.getId(), adjacency, visited, recursionStack, cycle)) {
                    if (!cycle.isEmpty()) {
                        circularDeps.add(CodeRepository.CircularDependency.builder()
                            .files(new ArrayList<>(cycle))
                            .description("Circular dependency detected: " + String.join(" -> ", cycle))
                            .severity(cycle.size() > 5 ? 8 : 5)
                            .suggestion("Refactor to remove circular dependencies. Consider using dependency injection or interfaces.")
                            .build());
                    }
                }
            }
        }
        
        return circularDeps;
    }
    
    /**
     * Detect cycle using DFS
     */
    private boolean detectCycle(String nodeId, Map<String, List<String>> adjacency,
                                Set<String> visited, Set<String> recursionStack,
                                List<String> cycle) {
        
        visited.add(nodeId);
        recursionStack.add(nodeId);
        
        for (String neighbor : adjacency.getOrDefault(nodeId, new ArrayList<>())) {
            if (!visited.contains(neighbor)) {
                if (detectCycle(neighbor, adjacency, visited, recursionStack, cycle)) {
                    if (!cycle.contains(nodeId)) {
                        cycle.add(nodeId);
                    }
                    return true;
                }
            } else if (recursionStack.contains(neighbor)) {
                cycle.add(nodeId);
                return true;
            }
        }
        
        recursionStack.remove(nodeId);
        return false;
    }
}