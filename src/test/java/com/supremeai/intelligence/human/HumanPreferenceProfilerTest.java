package com.supremeai.intelligence.human;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HumanPreferenceProfiler.
 * Tests developer profile learning, language detection, and coding style inference.
 */
class HumanPreferenceProfilerTest {

    private final HumanPreferenceProfiler profiler = new HumanPreferenceProfiler();

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
    void testGetProfile_newUserCreatesProfile() {
        DeveloperDNA profile = profiler.getProfile("user123");

        assertNotNull(profile);
        assertInstanceOf(DeveloperDNA.class, profile);
    }

    @Test
    void testGetProfile_sameUserReturnsSameProfile() {
        DeveloperDNA profile1 = profiler.getProfile("user123");
        DeveloperDNA profile2 = profiler.getProfile("user123");

        assertSame(profile1, profile2, "Same user should get same profile instance");
    }

    @Test
    void testGetProfile_differentUsersGetDifferentProfiles() {
        DeveloperDNA profile1 = profiler.getProfile("user1");
        DeveloperDNA profile2 = profiler.getProfile("user2");

        assertNotSame(profile1, profile2);
    }

    @Test
    void testLearnFromInteraction_detectsExplainKeyword() {
        String userId = "user1";
        String prompt = "Can you explain how this works?";
        String code = "public void test() {}";

        profiler.learnFromInteraction(userId, prompt, code, false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals(60, getExplanationScore(dna));
    }

    @Test
    void testLearnFromInteraction_shortPromptIncreasesDirectPreference() {
        String userId = "user2";
        String prompt = "fix"; // Short prompt
        String code = null;

        profiler.learnFromInteraction(userId, prompt, code, false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals(40, getExplanationScore(dna));
    }

    @Test
    void testLearnFromInteraction_askedForExplanationIncreasesPreference() {
        String userId = "user3";
        String prompt = "do it";
        String code = "code here";

        profiler.learnFromInteraction(userId, prompt, code, true);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals(60, getExplanationScore(dna));
    }

    @Test
    void testLearnFromInteraction_detectsBengaliLanguage() {
        String userId = "user_bengali";
        String prompt = "একটি লগিন পৃষ্ঠা তৈরি করুন (ekta login porsha toiri koro)";
        String code = "some code";

        profiler.learnFromInteraction(userId, prompt, code, false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals("Bengali", dna.getPreferredLanguage());
        assertTrue(dna.isPrefersSimpleLanguage());
    }

    @Test
    void testLearnFromInteraction_detectsHindiLanguage() {
        String userId = "user_hindi";
        String prompt = "ek login page banao";
        String code = "some code";

        profiler.learnFromInteraction(userId, prompt, code, false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals("Hindi", dna.getPreferredLanguage());
        assertTrue(dna.isPrefersSimpleLanguage());
    }

    @Test
    void testLearnFromInteraction_detectsSpanishLanguage() {
        String userId = "user_spanish";
        String prompt = "hola, hacer un login por favor";
        String code = "some code";

        profiler.learnFromInteraction(userId, prompt, code, false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals("Spanish", dna.getPreferredLanguage());
    }

    @Test
    void testLearnFromInteraction_detectsSimpleLanguagePreference() {
        String userId = "user_simple";
        String prompt = "Can you explain in simple terms? I'm new to programming";
        String code = "code";

        profiler.learnFromInteraction(userId, prompt, code, false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertTrue(dna.isPrefersSimpleLanguage());
    }

    @Test
    void testLearnFromInteraction_detectsIndentStyle4Spaces() {
        String userId = "user_indent4";
        String prompt = "test";
        String code = "public void method() {\n    return;\n}";

        profiler.learnFromInteraction(userId, prompt, code, false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals(4, getIndentStyle(dna));
    }

    @Test
    void testLearnFromInteraction_detectsIndentStyle2Spaces() {
        String userId = "user_indent2";
        String prompt = "test";
        String code = "public void method() {\n  return;\n}";

        profiler.learnFromInteraction(userId, prompt, code, false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals(2, getIndentStyle(dna));
    }

    @Test
    void testLearnFromInteraction_detectsCamelCase() {
        String userId = "user_camel";
        String prompt = "test";
        String code = "private String userName;\nprivate int userId;";

        profiler.learnFromInteraction(userId, prompt, code, false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertTrue(getUsesCamelCase(dna));
    }

    @Test
    void testLearnFromInteraction_nullCodeDoesNotBreak() {
        String userId = "user_nullcode";
        String prompt = "test prompt";
        String code = null;

        assertDoesNotThrow(() -> {
            profiler.learnFromInteraction(userId, prompt, code, false);
        });
    }

    @Test
    void testLearnFromInteraction_combinedLearning() {
        String userId = "user_combined";
        String prompt = "Explain how to create login in simple words";
        String code = "public class User {\n    private String userName;\n}";

        profiler.learnFromInteraction(userId, prompt, code, false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertTrue(getExplanationScore(dna) > 50);
        assertTrue(dna.isPrefersSimpleLanguage());
        assertEquals("English", dna.getPreferredLanguage());
        assertTrue(getUsesCamelCase(dna));
    }

    @Test
    void testLearnFromInteraction_multipleInteractionsAccumulate() {
        String userId = "user_multi";

        for (int i = 0; i < 5; i++) {
            profiler.learnFromInteraction(userId, "explain this", "code", false);
        }

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals(100, getExplanationScore(dna));
    }

    @Test
    void testLearnFromInteraction_bengaliUnicodeDetection() {
        String userId = "user_bengali2";
        String prompt = "বাংলা ভাষায় লগিন তৈরি korun";

        profiler.learnFromInteraction(userId, prompt, "code", false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals("Bengali", dna.getPreferredLanguage());
    }

    @Test
    void testLearnFromInteraction_hindiUnicodeDetection() {
        String userId = "user_hindi2";
        String prompt = "हिंदी में लॉगिन बनाओ";

        profiler.learnFromInteraction(userId, prompt, "code", false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals("Hindi", dna.getPreferredLanguage());
    }

    @Test
    void testPromptInjectionGeneration_includesAllPreferences() {
        DeveloperDNA dna = new DeveloperDNA();
        dna.setPreferredLanguage("Spanish");
        dna.setPrefersSimpleLanguage(true);
        dna.increaseExplanationPreference();
        dna.setIndentStyle(2);
        ReflectionTestUtils.setField(dna, "usesCamelCase", false);

        String injection = dna.generatePromptInjection();

        assertTrue(injection.contains("Spanish"));
        assertTrue(injection.contains("simple"));
        assertTrue(injection.contains("2 spaces"));
        assertTrue(injection.contains("snake_case"));
        assertTrue(injection.contains("detailed explanations"));
    }

    @Test
    void testProfilePersistence_acrossMultipleGetProfileCalls() {
        String userId = "persistent_user";

        DeveloperDNA first = profiler.getProfile(userId);
        first.increaseExplanationPreference();
        first.setPreferredLanguage("French");

        DeveloperDNA second = profiler.getProfile(userId);

        assertEquals(60, getExplanationScore(second));
        assertEquals("French", second.getPreferredLanguage());
    }

    @Test
    void testLearnFromInteraction_emptyPrompt() {
        String userId = "user_empty";
        profiler.learnFromInteraction(userId, "", "code", false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals(50, getExplanationScore(dna));
    }

    @Test
    void testLearnFromInteraction_longPromptDoesNotIncreaseDirectPreference() {
        String userId = "user_long";
        String prompt = "This is a very detailed and long prompt that describes exactly what I need, so it shouldn't trigger the short prompt logic";
        String code = "code";

        profiler.learnFromInteraction(userId, prompt, code, false);

        DeveloperDNA dna = profiler.getProfile(userId);
        assertEquals(50, getExplanationScore(dna));
    }

    @Test
    void testLearnFromInteraction_nullPrompt() {
        String userId = "user_null_prompt";

        assertDoesNotThrow(() -> {
            profiler.learnFromInteraction(userId, null, "code", false);
        });
    }

    @Test
    void testLearnFromInteraction_indentStyleNotChangedIfNoPattern() {
        String userId = "user_no_indent_pattern";
        String prompt = "test";
        String code = "public class Test {}";  // single line, no indentation

        profiler.learnFromInteraction(userId, prompt, code, false);

        DeveloperDNA dna = profiler.getProfile(userId);
        // Default should remain 4
        assertEquals(4, getIndentStyle(dna));
    }
}
