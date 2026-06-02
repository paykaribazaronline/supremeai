package com.supremeai.intelligence.vision;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RippleEffectPredictorTest {

    private RippleEffectPredictor predictor;

    @BeforeEach
    void setUp() {
        predictor = new RippleEffectPredictor();
    }

    @Test
    void analyzeCodeChange_EntityWithColumnChange_ReturnsWarnings() {
        String oldCode = "private String name;";
        String newCode = "@Column(name=\"user_name\") private String name;";
        
        RippleWarning warning = predictor.analyzeCodeChange("UserEntity.java", oldCode, newCode);
        
        assertNotNull(warning);
        assertTrue(warning.hasWarnings());
        String alert = warning.generateAlertMessage();
        assertTrue(alert.contains("Database Schema"));
        assertTrue(alert.contains("Flyway/Liquibase"));
    }

    @Test
    void analyzeCodeChange_ServiceWithInterfaceChange_ReturnsWarnings() {
        String oldCode = "public interface UserService { }";
        String newCode = "public interface UserService { void newMethod(); }";
        
        RippleWarning warning = predictor.analyzeCodeChange("UserService.java", oldCode, newCode);
        
        assertNotNull(warning);
        assertTrue(warning.hasWarnings());
        String alert = warning.generateAlertMessage();
        assertTrue(alert.contains("Implementations"));
        assertTrue(alert.contains("Unit Tests"));
    }

    @Test
    void analyzeCodeChange_SafeChange_ReturnsNull() {
        String oldCode = "int x = 1;";
        String newCode = "int x = 2;";
        
        assertNull(predictor.analyzeCodeChange("Calculator.java", oldCode, newCode));
        assertNull(predictor.analyzeCodeChange(null, null, null)); // Edge case
    }
}