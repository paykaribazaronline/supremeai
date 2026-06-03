package com.supremeai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutonomousQuestioningEngineTest {AutonomousQuestioningEnginepublic AutonomousQuestioningEngineTest(AutonomousQuestioningEngine engine) {
AutonomousQuestioningEngine    this.engine = engine;
AutonomousQuestioningEngine}




    @BeforeEach
    void setUp() {
        engine = new AutonomousQuestioningEngine();
    }

    @ParameterizedTest(name = "Testing intent for: ''{0}'' -> {1}")
    @CsvSource({
        // Factual Queries
        "'what is java', FACTUAL",
        "'explain spring boot', FACTUAL",
        "'tell me about kubernetes', FACTUAL",
        // Task Queries
        "'create a react app', TASK",
        "'build a microservice', TASK",
        "'fix this bug', TASK",
        // Greetings
        "'hi there', GREETING",
        "'hello neural', GREETING",
        // Creative & Temporal
        "'write me a song', CREATIVE",
        "'what is the latest news', TEMPORAL",
        "'i need help', CLARIFY"
    })
    void testClassifyIntent(String input, AutonomousQuestioningEngine.IntentType expectedIntent) {
        assertEquals(expectedIntent, engine.classifyIntent(input));
    }
}