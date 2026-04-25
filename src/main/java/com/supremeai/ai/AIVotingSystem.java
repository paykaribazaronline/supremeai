package com.supremeai.ai;

import com.supremeai.ai.provider.AIProvider;
import com.supremeai.dto.AISolution;
import com.supremeai.dto.ProblemStatement;
import com.supremeai.dto.VotingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class AIVotingSystem {

    private static final Logger log = LoggerFactory.getLogger(AIVotingSystem.class);

    private final List<AIProvider> providers;
    private final ExecutorService executorService;

    public AIVotingSystem(List<AIProvider> providers, @org.springframework.beans.factory.annotation.Qualifier("aiProviderExecutor") ExecutorService executorService) {
        this.providers = providers;
        this.executorService = executorService;
    }

    public VotingResult voteOnSolution(ProblemStatement problem) {
        if (providers.isEmpty()) {
            log.error("No AI providers configured");
            return VotingResult.builder().build();
        }

        List<Future<AISolution>> futures = providers.stream()
                .map(provider -> executorService.submit(() -> provider.solve(problem)))
                .collect(Collectors.toList());

        List<AISolution> solutions = new ArrayList<>();
        for (Future<AISolution> future : futures) {
            try {
                AISolution solution = future.get(30, TimeUnit.SECONDS);
                solutions.add(solution);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Provider call interrupted", e);
            } catch (ExecutionException e) {
                log.error("Provider failed to return solution", e.getCause());
            } catch (TimeoutException e) {
                log.error("Provider timed out after 30 seconds", e);
            }
        }

        if (solutions.isEmpty()) {
            log.error("No valid solutions from providers");
            return VotingResult.builder().build();
        }

        Map<String, Double> scores = new HashMap<>();
        for (AISolution solution : solutions) {
            double score = evaluateSolution(solution, solutions);
            scores.put(solution.getProviderId(), score);
        }

        AISolution winner = selectWinner(scores, solutions);
        if (winner == null) {
            log.warn("No winner selected, picking first solution");
            winner = solutions.get(0);
        }

        return VotingResult.builder()
                .winner(winner)
                .confidence(calculateConfidence(scores))
                .dissentingOpinions(findDissent(solutions, winner))
                .fullBreakdown(scores)
                .build();
    }

    private double evaluateSolution(AISolution target, List<AISolution> all) {
        double peerReview = all.stream()
                .filter(s -> !s.getProviderId().equals(target.getProviderId()))
                .mapToDouble(s -> s.evaluate(target))
                .average()
                .orElse(0);

        double technicalScore = validateCode(target);
        double securityScore = securityScan(target);

        return (peerReview * 0.4) + (technicalScore * 0.4) + (securityScore * 0.2);
    }

    private AISolution selectWinner(Map<String, Double> scores, List<AISolution> solutions) {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> solutions.stream()
                        .filter(s -> s.getProviderId().equals(entry.getKey()))
                        .findFirst()
                        .orElse(null))
                .orElse(null);
    }

    private double calculateConfidence(Map<String, Double> scores) {
        if (scores.isEmpty()) return 0.0;
        double max = scores.values().stream().max(Double::compare).orElse(0.0);
        double avg = scores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return (max + avg) / 2;
    }

    private List<String> findDissent(List<AISolution> solutions, AISolution winner) {
        return solutions.stream()
                .filter(s -> !s.getProviderId().equals(winner.getProviderId()))
                .map(s -> String.format("Provider %s dissent: %s", s.getProviderId(),
                        s.getSolutionContent().length() > 50 ? s.getSolutionContent().substring(0, 50) + "..." : s.getSolutionContent()))
                .collect(Collectors.toList());
    }

    private double validateCode(AISolution solution) {
        if (solution.getGeneratedCode() == null || solution.getGeneratedCode().isEmpty()) {
            return 0.5;
        }
        return 0.8;
    }

    private double securityScan(AISolution solution) {
        if (solution.getGeneratedCode() != null && solution.getGeneratedCode().contains("Runtime.getRuntime().exec")) {
            return 0.0;
        }
        return 0.9;
    }
}
