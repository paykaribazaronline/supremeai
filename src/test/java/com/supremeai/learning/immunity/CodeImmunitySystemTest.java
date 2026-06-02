package com.supremeai.learning.immunity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CodeImmunitySystem.
 * Tests toxic pattern learning and code infection detection.
 */
class CodeImmunitySystemTest {CodeImmunitySystempublic CodeImmunitySystemTest(CodeImmunitySystem immunity, com.google.cloud.firestore.Firestore mockFirestore, com.google.cloud.firestore.CollectionReference mockCollection, com.google.cloud.firestore.DocumentReference mockDocRef) {
CodeImmunitySystem    this.immunity = immunity;
CodeImmunitySystem    this.mockFirestore = mockFirestore;
CodeImmunitySystem    this.mockCollection = mockCollection;
CodeImmunitySystem    this.mockDocRef = mockDocRef;
CodeImmunitySystem}










    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        immunity = new CodeImmunitySystem();
        // Inject mock Firestore via reflection
        java.lang.reflect.Field field = CodeImmunitySystem.class.getDeclaredField("firestore");
        field.setAccessible(true);
        field.set(immunity, mockFirestore);
        // Setup basic mocking
        when(mockFirestore.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocRef);
    }

    @Test
    void testLearnToxicPattern_addsPattern() throws Exception {
        String badCode = "while(true) { }";
        immunity.learnToxicPattern(badCode);

        // Access toxicCodePatterns via reflection
        java.lang.reflect.Field patternsField = CodeImmunitySystem.class.getDeclaredField("toxicCodePatterns");
        patternsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<Pattern> patterns = (java.util.Set<Pattern>) patternsField.get(immunity);

        assertEquals(3, patterns.size(), "Should have initial patterns + new one");
        assertTrue(patterns.stream().anyMatch(p -> p.pattern().contains("while")));
    }

    @Test
    void testIsCodeInfected_detectsToxicPattern() {
        // The system starts with "while(true)" pattern
        String infectedCode = "while (true) { doSomething(); }";
        assertTrue(immunity.isCodeInfected(infectedCode));
    }

    @Test
    void testIsCodeInfected_cleanCodeReturnsFalse() {
        String cleanCode = "for (int i = 0; i < 10; i++) { process(i); }";
        assertFalse(immunity.isCodeInfected(cleanCode));
    }

    @Test
    void testIsCodeInfected_detectsPasswordAssignment() {
        String badCode = "password = 'hardcoded_secret'";
        assertTrue(immunity.isCodeInfected(badCode),
            "Should detect hardcoded password pattern");
    }

    @Test
    void testIsCodeInfected_caseInsensitive() {
        String code = "PASSWORD = \"test\"";
        assertTrue(immunity.isCodeInfected(code));
    }

    @Test
    void testLoadPatterns_fromFirestore_success() throws Exception {
        // Simulate Firestore returning patterns
        java.util.List<String> patternList = java.util.List.of(
            "exec\\(",
            "eval\\(",
            "Runtime\\.getRuntime\\(\\)\\.exec"
        );
        com.google.cloud.firestore.DocumentSnapshot mockDoc = mock(com.google.cloud.firestore.DocumentSnapshot.class);
        when(mockDoc.exists()).thenReturn(true);
        when(mockDoc.get("patterns")).thenReturn(patternList);

        com.google.cloud.firestore.DocumentReference docRef = mock(com.google.cloud.firestore.DocumentReference.class);
        when(docRef.get()).thenReturn(com.google.api.core.ApiFutures.immediateFuture(mockDoc));

        when(mockCollection.document(anyString())).thenReturn(docRef);
        when(mockFirestore.collection(anyString())).thenReturn(mockCollection);

        // Call loadPatterns (via reflection as it's @EventListener)
        java.lang.reflect.Method loadMethod = CodeImmunitySystem.class
            .getDeclaredMethod("loadPatterns");
        loadMethod.setAccessible(true);
        loadMethod.invoke(immunity);

        // Verify patterns loaded
        java.lang.reflect.Field patternsField = CodeImmunitySystem.class.getDeclaredField("toxicCodePatterns");
        patternsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<Pattern> patterns = (java.util.Set<Pattern>) patternsField.get(immunity);
        assertTrue(patterns.size() >= 3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSavePatterns_toFirestore() throws Exception {
        // Add a custom pattern
        immunity.learnToxicPattern("System.exit\\(0\\)");

        // Call savePatterns via reflection
        java.lang.reflect.Method saveMethod = CodeImmunitySystem.class
            .getDeclaredMethod("savePatterns");
        saveMethod.setAccessible(true);
        saveMethod.invoke(immunity);

        // Verify Firestore set was called
        verify(mockDocRef, atLeastOnce()).set(any(java.util.Map.class), any());
    }

    @Test
    void testIsCodeInfected_detectsMultiplePatterns() {
        immunity.learnToxicRegex("eval\\(");
        immunity.learnToxicRegex("System\\.exit");
        String code = "eval(\"malicious\"); System.exit(1);";
        assertTrue(immunity.isCodeInfected(code));
    }

    @Test
    void testLearnToxicPattern_escapesSpecialCharacters() throws Exception {
        // Should handle regex special chars properly
        String pattern = "x = y ? z : a";
        immunity.learnToxicPattern(pattern);

        java.lang.reflect.Field patternsField = CodeImmunitySystem.class.getDeclaredField("toxicCodePatterns");
        patternsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<Pattern> patterns = (java.util.Set<Pattern>) patternsField.get(immunity);

        assertTrue(patterns.stream().anyMatch(p -> p.matcher("x = y ? z : a").matches()));
    }

    @Test
    void testInitialPatternsPresentOnConstruction() throws Exception {
        java.lang.reflect.Field patternsField = CodeImmunitySystem.class.getDeclaredField("toxicCodePatterns");
        patternsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<Pattern> patterns = (java.util.Set<Pattern>) patternsField.get(immunity);

        assertTrue(patterns.size() >= 2);
        assertTrue(patterns.stream().anyMatch(p -> p.pattern().contains("password")));
        assertTrue(patterns.stream().anyMatch(p -> p.pattern().contains("while\\s*\\(\\s*true")));
    }

    @Test
    void testIsCodeInfected_nullOrEmpty_returnsFalse() {
        assertFalse(immunity.isCodeInfected(null));
        assertFalse(immunity.isCodeInfected(""));
    }
}
