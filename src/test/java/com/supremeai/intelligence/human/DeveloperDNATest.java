package com.supremeai.intelligence.human;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DeveloperDNA.
 * Tests developer preference tracking, prompt injection generation, and summary.
 */
class DeveloperDNATest {

    private int getExplanationScore(DeveloperDNA dna) {
        return (int) ReflectionTestUtils.getField(dna, "explanationScore");
    }

    private int getIndentStyle(DeveloperDNA dna) {
        return (int) ReflectionTestUtils.getField(dna, "indentStyle");
    }

    private boolean getUsesCamelCase(DeveloperDNA dna) {
        return (boolean) ReflectionTestUtils.getField(dna, "usesCamelCase");
    }

    @Test
    void testDefaultValues() {
        DeveloperDNA dna = new DeveloperDNA();

        assertEquals(50, getExplanationScore(dna));
        assertEquals(4, getIndentStyle(dna));
        assertTrue(getUsesCamelCase(dna));
        assertEquals("English", dna.getPreferredLanguage());
        assertFalse(dna.isPrefersSimpleLanguage());
    }

    @Test
    void testIncreaseExplanationPreference() {
        DeveloperDNA dna = new DeveloperDNA();

        dna.increaseExplanationPreference();
        assertEquals(60, getExplanationScore(dna));

        dna.increaseExplanationPreference();
        assertEquals(70, getExplanationScore(dna));
    }

    @Test
    void testIncreaseExplanationPreference_maxCap() {
        DeveloperDNA dna = new DeveloperDNA();

        for (int i = 0; i < 10; i++) {
            dna.increaseExplanationPreference();
        }

        assertEquals(100, getExplanationScore(dna), "Explanation score should max at 100");
    }

    @Test
    void testIncreaseDirectCodePreference() {
        DeveloperDNA dna = new DeveloperDNA();

        dna.increaseDirectCodePreference();
        assertEquals(40, getExplanationScore(dna));

        dna.increaseDirectCodePreference();
        assertEquals(30, getExplanationScore(dna));
    }

    @Test
    void testIncreaseDirectCodePreference_minCap() {
        DeveloperDNA dna = new DeveloperDNA();

        for (int i = 0; i < 10; i++) {
            dna.increaseDirectCodePreference();
        }

        assertEquals(0, getExplanationScore(dna), "Explanation score should min at 0");
    }

    @Test
    void testSetIndentStyle() {
        DeveloperDNA dna = new DeveloperDNA();

        dna.setIndentStyle(2);
        assertEquals(2, getIndentStyle(dna));

        dna.setIndentStyle(8);
        assertEquals(8, getIndentStyle(dna));
    }

    @Test
    void testSetUsesCamelCase() {
        DeveloperDNA dna = new DeveloperDNA();

        dna.setUsesCamelCase(false);
        assertFalse(getUsesCamelCase(dna));

        dna.setUsesCamelCase(true);
        assertTrue(getUsesCamelCase(dna));
    }

    @Test
    void testSetPreferredLanguage() {
        DeveloperDNA dna = new DeveloperDNA();

        dna.setPreferredLanguage("Bengali");
        assertEquals("Bengali", dna.getPreferredLanguage());

        dna.setPreferredLanguage("Hindi");
        assertEquals("Hindi", dna.getPreferredLanguage());

        dna.setPreferredLanguage("Spanish");
        assertEquals("Spanish", dna.getPreferredLanguage());
    }

    @Test
    void testSetPrefersSimpleLanguage() {
        DeveloperDNA dna = new DeveloperDNA();

        dna.setPrefersSimpleLanguage(true);
        assertTrue(dna.isPrefersSimpleLanguage());

        dna.setPrefersSimpleLanguage(false);
        assertFalse(dna.isPrefersSimpleLanguage());
    }

    @Test
    void testGeneratePromptInjection_nonEnglishLanguage() {
        DeveloperDNA dna = new DeveloperDNA();
        dna.setPreferredLanguage("Bengali");

        String injection = dna.generatePromptInjection();

        assertNotNull(injection);
        assertTrue(injection.contains("Bengali"));
        assertTrue(injection.contains("CRITICAL: ALWAYS communicate"));
    }

    @Test
    void testGeneratePromptInjection_englishLanguageNoSpecialInstruction() {
        DeveloperDNA dna = new DeveloperDNA();
        dna.setPreferredLanguage("English");

        String injection = dna.generatePromptInjection();
        assertFalse(injection.contains("CRITICAL: ALWAYS communicate"));
    }

    @Test
    void testGeneratePromptInjection_prefersSimpleLanguage() {
        DeveloperDNA dna = new DeveloperDNA();
        dna.setPrefersSimpleLanguage(true);

        String injection = dna.generatePromptInjection();

        assertTrue(injection.contains("extremely simple"));
        assertTrue(injection.contains("non-technical"));
    }

    @Test
    void testGeneratePromptInjection_highExplanationScore() {
        DeveloperDNA dna = new DeveloperDNA();
        for (int i = 0; i < 3; i++) dna.increaseExplanationPreference();

        String injection = dna.generatePromptInjection();
        assertTrue(injection.contains("detailed explanations"));
        assertTrue(injection.contains("step-by-step"));
    }

    @Test
    void testGeneratePromptInjection_lowExplanationScore() {
        DeveloperDNA dna = new DeveloperDNA();
        for (int i = 0; i < 3; i++) dna.increaseDirectCodePreference();

        String injection = dna.generatePromptInjection();
        assertTrue(injection.contains("ONLY the raw code"));
        assertTrue(injection.contains("NO explanations"));
    }

    @Test
    void testGeneratePromptInjection_neutralExplanationScore() {
        DeveloperDNA dna = new DeveloperDNA();
        // Default score is 50, which is neutral

        String injection = dna.generatePromptInjection();
        assertFalse(injection.contains("detailed explanations"));
        assertFalse(injection.contains("ONLY the raw code"));
    }

    @Test
    void testGeneratePromptInjection_formattingInstructions() {
        DeveloperDNA dna = new DeveloperDNA();
        dna.setIndentStyle(2);
        ReflectionTestUtils.setField(dna, "usesCamelCase", false);

        String injection = dna.generatePromptInjection();

        assertTrue(injection.contains("2 spaces"));
        assertTrue(injection.contains("snake_case"));
    }

    @Test
    void testGeneratePromptInjection_combinedPreferences() {
        DeveloperDNA dna = new DeveloperDNA();
        dna.setPreferredLanguage("Hindi");
        dna.setPrefersSimpleLanguage(true);
        dna.increaseExplanationPreference();
        dna.setIndentStyle(2);
        ReflectionTestUtils.setField(dna, "usesCamelCase", false);

        String injection = dna.generatePromptInjection();

        assertTrue(injection.contains("Hindi"));
        assertTrue(injection.contains("simple"));
        assertTrue(injection.contains("2 spaces"));
    }

    @Test
    void testGetSummary_format() {
        DeveloperDNA dna = new DeveloperDNA();
        dna.setPreferredLanguage("Bengali");
        dna.setPrefersSimpleLanguage(true);
        dna.increaseExplanationPreference();

        String summary = dna.getSummary();

        assertTrue(summary.contains("Bengali"));
        assertTrue(summary.contains("true"));
        assertTrue(summary.contains("60"));
    }

    @Test
    void testGeneratePromptInjection_camelCaseFormatting() {
        DeveloperDNA dna = new DeveloperDNA();
        dna.setUsesCamelCase(true);

        String injection = dna.generatePromptInjection();
        assertTrue(injection.contains("camelCase"));
    }

    @Test
    void testGeneratePromptInjection_nonEnglishWithSimpleLanguage() {
        DeveloperDNA dna = new DeveloperDNA();
        dna.setPreferredLanguage("Spanish");
        dna.setPrefersSimpleLanguage(true);

        String injection = dna.generatePromptInjection();

        assertTrue(injection.contains("Spanish"));
        assertTrue(injection.contains("simple"));
    }
}
