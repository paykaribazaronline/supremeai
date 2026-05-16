package com.supremeai.service.analysis;

import com.supremeai.model.analysis.AnalysisFix;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FixSuggestionServiceTest {

    @Test
    void testValidateFixSyntaxValid() {
        AnalysisFix fix = AnalysisFix.builder()
            .fixedCode("public void test() { if (x > 0) { return; } }")
            .build();

        FixSuggestionService service = new FixSuggestionService(null, null, null);
        assertTrue(service.validateFixSyntax(fix));
    }

    @Test
    void testValidateFixSyntaxUnbalancedBraces() {
        AnalysisFix fix = AnalysisFix.builder()
            .fixedCode("public void test() { if (x > 0) { return; }")
            .build();

        FixSuggestionService service = new FixSuggestionService(null, null, null);
        assertFalse(service.validateFixSyntax(fix));
    }

    @Test
    void testValidateFixSyntaxUnbalancedParens() {
        AnalysisFix fix = AnalysisFix.builder()
            .fixedCode("public void test() { if (x > 0 { return; } }")
            .build();

        FixSuggestionService service = new FixSuggestionService(null, null, null);
        assertFalse(service.validateFixSyntax(fix));
    }

    @Test
    void testValidateFixSyntaxEmpty() {
        AnalysisFix fix = AnalysisFix.builder()
            .fixedCode("")
            .build();

        FixSuggestionService service = new FixSuggestionService(null, null, null);
        assertFalse(service.validateFixSyntax(fix));
    }

    @Test
    void testValidateFixSyntaxNull() {
        AnalysisFix fix = AnalysisFix.builder()
            .fixedCode(null)
            .build();

        FixSuggestionService service = new FixSuggestionService(null, null, null);
        assertFalse(service.validateFixSyntax(fix));
    }

    @Test
    void testValidateFixSyntaxWithStrings() {
        AnalysisFix fix = AnalysisFix.builder()
            .fixedCode("String s = \"hello { world\";")
            .build();

        FixSuggestionService service = new FixSuggestionService(null, null, null);
        assertTrue(service.validateFixSyntax(fix));
    }
}
