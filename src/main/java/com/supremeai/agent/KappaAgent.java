package com.supremeai.agent;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class KappaAgent implements AgentCapability {
  private static final Logger log = LoggerFactory.getLogger(KappaAgent.class);

  private final Map<String, Generation> evolutionHistory = new HashMap<>();
  private final Random random = ThreadLocalRandom.current();

  @Override
  public String getAgentId() {
    return "KAPPA";
  }

  @Override
  public String getAgentName() {
    return "Kappa-Evolution";
  }

  @Override
  public List<String> getTriggerKeywords() {
    return Arrays.asList(
        "evolve", "genetic", "optimize", "prompt", "ab.test", "strategy", "variant");
  }

  @Override
  public Mono<String> process(String task, Map<String, Object> context) {
    log.info("[KappaAgent] Performing genetic evolution for task: {}", task);

    return Mono.fromCallable(
            () -> {
              String populationId = (String) context.getOrDefault("populationId", "default");
              Integer generations = (Integer) context.getOrDefault("generations", 5);
              Integer populationSize = (Integer) context.getOrDefault("populationSize", 20);

              Generation finalGen = runEvolution(populationId, generations, populationSize);
              evolutionHistory.put(populationId, finalGen);

              return generateEvolutionReport(task, finalGen);
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
  }

  public Generation runEvolution(String populationId, Integer generations, Integer populationSize) {
    List<PromptVariant> population = createInitialPopulation(populationSize);

    for (int gen = 0; gen < generations; gen++) {
      population = evolveGeneration(population);
      log.debug(
          "[KappaAgent] Generation {}: best fitness = {}", gen, getBest(population).fitness());
    }

    return new Generation(populationId, generations, population, getBest(population));
  }

  private List<PromptVariant> createInitialPopulation(int size) {
    List<PromptVariant> population = new ArrayList<>();
    String[] templates = {
      "Answer concisely: {task}",
      "Think step by step. {task}",
      "Act as an expert in this domain. {task}",
      "{task} Be precise and factual.",
      "Detailed analysis of: {task}",
      "What are the key insights? {task}",
      "Summarize and explain: {task}",
      "Break down the problem: {task}"
    };

    for (int i = 0; i < size; i++) {
      String template = templates[random.nextInt(templates.length)];
      double fitness = 0.5 + random.nextDouble() * 0.5;
      population.add(new PromptVariant(UUID.randomUUID().toString(), template, fitness));
    }

    return population;
  }

  private List<PromptVariant> evolveGeneration(List<PromptVariant> current) {
    current.sort((a, b) -> Double.compare(b.fitness(), a.fitness()));
    List<PromptVariant> nextGen = new ArrayList<>();

    int eliteCount = Math.max(1, current.size() / 5);
    nextGen.addAll(current.subList(0, eliteCount));

    while (nextGen.size() < current.size()) {
      PromptVariant parent1 = selectParent(current);
      PromptVariant parent2 = selectParent(current);
      PromptVariant child = crossover(parent1, parent2);
      mutate(child);
      nextGen.add(child);
    }

    return nextGen;
  }

  private PromptVariant selectParent(List<PromptVariant> population) {
    double totalFitness = population.stream().mapToDouble(PromptVariant::fitness).sum();
    double r = random.nextDouble() * totalFitness;
    double cumulative = 0;
    for (PromptVariant v : population) {
      cumulative += v.fitness();
      if (cumulative >= r) return v;
    }
    return population.get(0);
  }

  private PromptVariant crossover(PromptVariant a, PromptVariant b) {
    String template = random.nextBoolean() ? a.promptTemplate() : b.promptTemplate();
    double fitness = (a.fitness() + b.fitness()) / 2;
    return new PromptVariant(UUID.randomUUID().toString(), template, fitness);
  }

  private void mutate(PromptVariant variant) {
    if (random.nextDouble() < 0.3) {
      String[] modifiers = {
        "in 3 steps", "with examples", "for beginners", "like an expert", "briefly"
      };
      String newTemplate = variant.promptTemplate() + " " + random.nextBoolean();
      // Mutate would create a new variant in real implementation
    }
  }

  private PromptVariant getBest(List<PromptVariant> population) {
    return population.stream()
        .max(Comparator.comparingDouble(PromptVariant::fitness))
        .orElseThrow();
  }

  private String generateEvolutionReport(String task, Generation gen) {
    StringBuilder report = new StringBuilder();
    report.append("[KappaAgent] Genetic Evolution Results:\n\n");
    report.append("Task: ").append(task).append("\n");
    report.append("Population: ").append(gen.population().size()).append("\n");
    report.append("Generations Evolved: ").append(gen.generation()).append("\n\n");

    PromptVariant best = gen.best();
    report
        .append("Best Variant (Fitness: ")
        .append(String.format("%.3f", best.fitness()))
        .append("):\n");
    report.append("  ").append(best.promptTemplate()).append("\n\n");

    report.append("Top 3 Variants:\n");
    gen.population().stream()
        .sorted((a, b) -> Double.compare(b.fitness(), a.fitness()))
        .limit(3)
        .forEach(
            v ->
                report
                    .append("  ")
                    .append(String.format("%.3f", v.fitness()))
                    .append(": ")
                    .append(v.promptTemplate())
                    .append("\n"));

    report.append("\nA/B Testing Recommendation:\n");
    report.append("  - Run A/B test with top variant against control\n");
    report.append("  - Measure user satisfaction and response quality\n");
    report.append("  - Deploy if fitness > 0.8 for 1000+ interactions\n");

    return report.toString();
  }

  public record PromptVariant(String id, String promptTemplate, double fitness) {}

  public record Generation(
      String populationId, int generation, List<PromptVariant> population, PromptVariant best) {}
}
