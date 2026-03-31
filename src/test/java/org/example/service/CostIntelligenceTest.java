package org.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CostIntelligenceTest {

    @Autowired
    private DeltaCostAgent deltaAgent;

    @Autowired
    private EpsilonOptimizerAgent epsilonAgent;

    @Autowired
    private ZetaFinanceAgent zetaAgent;

    @Test
    public void testCostTracking() {
        Map<String, Object> report = deltaAgent.trackCosts();
        assertNotNull(report);
        assertEquals("ACTIVE", report.get("status"));
        assertTrue(report.containsKey("total_monthly_spend"));
        assertTrue(report.containsKey("cloud_breakdown"));
    }

    @Test
    public void testResourceOptimization() {
        Map<String, Object> recommendations = epsilonAgent.optimizeResources();
        assertNotNull(recommendations);
        assertEquals("COMPLETED", recommendations.get("status"));
        assertNotNull(recommendations.get("recommendations"));
    }

    @Test
    public void testBudgetPlanning() {
        Map<String, Object> budget = zetaAgent.planBudget();
        assertNotNull(budget);
        assertEquals("PUBLISHED", budget.get("status"));
        assertEquals(63500.00, budget.get("annual_total_limit"));
    }
}
