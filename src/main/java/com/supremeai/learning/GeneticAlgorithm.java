package com.supremeai.learning;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Service
public class GeneticAlgorithm {

    private List<AgentConfig> population;
    private final Random random = new Random();
    private static final int POPULATION_SIZE = 50;
    private static final double MUTATION_RATE = 0.05; // 5% mutation chance

    public GeneticAlgorithm() {
        this.population = new ArrayList<>();
        initializePopulation();
    }

    private void initializePopulation() {
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population.add(new AgentConfig(random.nextDouble(), random.nextDouble(), random.nextDouble()));
        }
    }

    public void evolvePopulation() {
        List<AgentConfig> newPopulation = new ArrayList<>();

        // Elitism: Keep the top 2 best configurations
        population.sort(Comparator.comparingDouble(this::evaluateFitness).reversed());
        newPopulation.add(population.get(0));
        newPopulation.add(population.get(1));

        // Generate the rest of the new population
        while (newPopulation.size() < POPULATION_SIZE) {
            AgentConfig parent1 = tournamentSelection();
            AgentConfig parent2 = tournamentSelection();
            
            AgentConfig child = crossover(parent1, parent2);
            mutate(child);
            
            newPopulation.add(child);
        }
        this.population = newPopulation;
    }

    private double evaluateFitness(AgentConfig config) {
        // In a real scenario, this evaluates actual AI model performance 
        // (success rate, code coverage, speed) based on previous real runs
        double successRate = config.decisionWeight * 0.4;
        double coverage = config.confidenceThreshold * 0.4;
        double speed = config.learningRate * 0.2;
        return successRate + coverage + speed; 
    }

    private AgentConfig tournamentSelection() {
        AgentConfig best = null;
        for (int i = 0; i < 5; i++) { // Tournament size of 5
            AgentConfig randomConfig = population.get(random.nextInt(POPULATION_SIZE));
            if (best == null || evaluateFitness(randomConfig) > evaluateFitness(best)) {
                best = randomConfig;
            }
        }
        return best;
    }

    private AgentConfig crossover(AgentConfig p1, AgentConfig p2) {
        return new AgentConfig(
            random.nextBoolean() ? p1.decisionWeight : p2.decisionWeight,
            random.nextBoolean() ? p1.confidenceThreshold : p2.confidenceThreshold,
            random.nextBoolean() ? p1.learningRate : p2.learningRate
        );
    }

    private void mutate(AgentConfig config) {
        if (random.nextDouble() < MUTATION_RATE) config.decisionWeight = random.nextDouble();
        if (random.nextDouble() < MUTATION_RATE) config.confidenceThreshold = random.nextDouble();
        if (random.nextDouble() < MUTATION_RATE) config.learningRate = random.nextDouble();
    }
    
    public AgentConfig getBestConfig() {
        return population.stream()
                .max(Comparator.comparingDouble(this::evaluateFitness))
                .orElse(population.get(0));
    }
}

class AgentConfig {
    public double decisionWeight;
    public double confidenceThreshold;
    public double learningRate;

    public AgentConfig(double dw, double ct, double lr) {
        this.decisionWeight = dw;
        this.confidenceThreshold = ct;
        this.learningRate = lr;
    }
}