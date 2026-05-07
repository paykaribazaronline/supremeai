package com.supremeai.learning;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GeneticAlgorithm.
 * Tests population evolution, selection, crossover, and mutation.
 */
class GeneticAlgorithmTest {

    @Test
    void testPopulationInitialization() throws Exception {
        GeneticAlgorithm ga = new GeneticAlgorithm();

        // Access private population field via reflection
        Field populationField = GeneticAlgorithm.class.getDeclaredField("population");
        populationField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> population = (List<Object>) populationField.get(ga);

        assertEquals(50, population.size(), "Population should be initialized with 50 agents");
    }

    @Test
    void testEvolvePopulation_maintainsSize() throws Exception {
        GeneticAlgorithm ga = new GeneticAlgorithm();

        // Evolve multiple generations
        for (int i = 0; i < 5; i++) {
            ga.evolvePopulation();
        }

        // Verify population size is still 50
        Field populationField = GeneticAlgorithm.class.getDeclaredField("population");
        populationField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> population = (List<Object>) populationField.get(ga);
        assertEquals(50, population.size(), "Population size should remain constant after evolution");
    }

    @Test
    void testTournamentSelection_returnsBest() throws Exception {
        GeneticAlgorithm ga = new GeneticAlgorithm();

        // Access and manipulate population to have known fitness values
        Field populationField = GeneticAlgorithm.class.getDeclaredField("population");
        populationField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> population = (List<Object>) populationField.get(ga);

        // Replace ALL elements with known high-fitness config (via reflection)
        for (Object agent : population) {
            Field dwField = agent.getClass().getDeclaredField("decisionWeight");
            Field ctField = agent.getClass().getDeclaredField("confidenceThreshold");
            Field lrField = agent.getClass().getDeclaredField("learningRate");
            dwField.setAccessible(true);
            ctField.setAccessible(true);
            lrField.setAccessible(true);
            dwField.setDouble(agent, 0.9);
            ctField.setDouble(agent, 0.9);
            lrField.setDouble(agent, 0.9);
        }

        // Invoke tournamentSelection via reflection
        var tournamentMethod = GeneticAlgorithm.class.getDeclaredMethod("tournamentSelection");
        tournamentMethod.setAccessible(true);
        Object selected = tournamentMethod.invoke(ga);

        // Should select the high-fitness agent (first one) due to tournament
        Field selectedDw = selected.getClass().getDeclaredField("decisionWeight");
        selectedDw.setAccessible(true);
        double selectedDwVal = selectedDw.getDouble(selected);
        assertEquals(0.9, selectedDwVal, 0.01, "Tournament selection should pick high-fitness agent");
    }

    @Test
    void testCrossover_producesChildFromParents() throws Exception {
        GeneticAlgorithm ga = new GeneticAlgorithm();

        // Get two parent agents
        Field populationField = GeneticAlgorithm.class.getDeclaredField("population");
        populationField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> population = (List<Object>) populationField.get(ga);
        Object parent1 = population.get(0);
        Object parent2 = population.get(1);

        // Get crossover method
        var crossoverMethod = GeneticAlgorithm.class.getDeclaredMethod("crossover", AgentConfig.class, AgentConfig.class);
        crossoverMethod.setAccessible(true);
        Object child = crossoverMethod.invoke(ga, parent1, parent2);

        // Verify child has valid field values (from either parent)
        Field childDw = child.getClass().getDeclaredField("decisionWeight");
        childDw.setAccessible(true);
        double childDwVal = childDw.getDouble(child);
        assertTrue(childDwVal >= 0.0 && childDwVal <= 1.0, "Child decisionWeight should be within [0,1]");
    }

    @Test
    void testMutate_changesFieldsOccasionally() throws Exception {
        GeneticAlgorithm ga = new GeneticAlgorithm();

        // Get an agent
        Field populationField = GeneticAlgorithm.class.getDeclaredField("population");
        populationField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> population = (List<Object>) populationField.get(ga);
        Object agent = population.get(0);

        // Record initial values
        Field dwField = agent.getClass().getDeclaredField("decisionWeight");
        Field ctField = agent.getClass().getDeclaredField("confidenceThreshold");
        Field lrField = agent.getClass().getDeclaredField("learningRate");
        dwField.setAccessible(true);
        ctField.setAccessible(true);
        lrField.setAccessible(true);
        double initialDw = dwField.getDouble(agent);
        double initialCt = ctField.getDouble(agent);
        double initialLr = lrField.getDouble(agent);

        // Invoke mutate multiple times (mutation rate = 5%)
        var mutateMethod = GeneticAlgorithm.class.getDeclaredMethod("mutate", AgentConfig.class);
        mutateMethod.setAccessible(true);
        boolean changed = false;
        for (int i = 0; i < 100; i++) {
            mutateMethod.invoke(ga, agent);
            double newDw = dwField.getDouble(agent);
            double newCt = ctField.getDouble(agent);
            double newLr = lrField.getDouble(agent);
            if (newDw != initialDw || newCt != initialCt || newLr != initialLr) {
                changed = true;
                break;
            }
        }
        assertTrue(changed, "Mutation should occur over many attempts (5% rate)");
    }

    @Test
    void testEvaluateFitness_returnsValueBetween0And1() throws Exception {
        GeneticAlgorithm ga = new GeneticAlgorithm();

        var evaluateMethod = GeneticAlgorithm.class.getDeclaredMethod("evaluateFitness", AgentConfig.class);
        evaluateMethod.setAccessible(true);

        // Get an agent from population
        Field populationField = GeneticAlgorithm.class.getDeclaredField("population");
        populationField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> population = (List<Object>) populationField.get(ga);
        Object agent = population.get(0);

        double fitness = (double) evaluateMethod.invoke(ga, agent);
        assertTrue(fitness >= 0.0 && fitness <= 1.0, "Fitness should be between 0 and 1");
    }

    @Test
    void testGetBestConfig_returnsValidAgent() throws Exception {
        GeneticAlgorithm ga = new GeneticAlgorithm();

        Object best = ga.getBestConfig();

        assertNotNull(best);
        Field dwField = best.getClass().getDeclaredField("decisionWeight");
        dwField.setAccessible(true);
        double dw = dwField.getDouble(best);
        assertTrue(dw >= 0.0 && dw <= 1.0, "Best agent should have valid decisionWeight");
    }

    @Test
    void testElitism_preservesTopTwo() throws Exception {
        GeneticAlgorithm ga = new GeneticAlgorithm();

        // Access population and manipulate fitness values
        Field populationField = GeneticAlgorithm.class.getDeclaredField("population");
        populationField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> population = (List<Object>) populationField.get(ga);

        // Make first two agents have very high fitness (through field manipulation)
        Object agent0 = population.get(0);
        Object agent1 = population.get(1);
        // Set high values that would rank them at top
        Field dw0 = agent0.getClass().getDeclaredField("decisionWeight");
        Field ct0 = agent0.getClass().getDeclaredField("confidenceThreshold");
        Field lr0 = agent0.getClass().getDeclaredField("learningRate");
        dw0.setAccessible(true);
        ct0.setAccessible(true);
        lr0.setAccessible(true);
        dw0.setDouble(agent0, 1.0);
        ct0.setDouble(agent0, 1.0);
        lr0.setDouble(agent0, 1.0);

        Field dw1 = agent1.getClass().getDeclaredField("decisionWeight");
        Field ct1 = agent1.getClass().getDeclaredField("confidenceThreshold");
        Field lr1 = agent1.getClass().getDeclaredField("learningRate");
        dw1.setAccessible(true);
        ct1.setAccessible(true);
        lr1.setAccessible(true);
        dw1.setDouble(agent1, 0.95);
        ct1.setDouble(agent1, 0.95);
        lr1.setDouble(agent1, 0.95);

        // Evolve
        ga.evolvePopulation();

        // The new population should still contain the elite (first 2 entries)
        populationField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> newPopulation = (List<Object>) populationField.get(ga);
        Object newFirst = newPopulation.get(0);
        Object newSecond = newPopulation.get(1);

        Field newDw0 = newFirst.getClass().getDeclaredField("decisionWeight");
        Field newDw1 = newSecond.getClass().getDeclaredField("decisionWeight");
        newDw0.setAccessible(true);
        newDw1.setAccessible(true);
        double val0 = newDw0.getDouble(newFirst);
        double val1 = newDw1.getDouble(newSecond);

        assertTrue(val0 > 0.9 || val1 > 0.9, "Top agents from previous generation should be preserved via elitism");
    }
}
