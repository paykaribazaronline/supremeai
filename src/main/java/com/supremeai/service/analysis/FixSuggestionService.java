package com.supremeai.service.analysis;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.model.analysis.AnalysisFix;
import com.supremeai.model.analysis.AnalysisFinding;
import com.supremeai.repository.analysis.AnalysisFixRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FixSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(FixSuggestionService.class);

    private final AIProviderFactory providerFactory;
    private final FixPromptTemplates fixPromptTemplates;
    private final AnalysisFixRepository fixRepository;

    private static final Pattern EXPLANATION_PATTERN = Pattern.compile("EXPLANATION:\\s*(.+?)(?=FIXED_CODE:|$)", Pattern.DOTALL);
    private static final Pattern FIXED_CODE_PATTERN = Pattern.compile("FIXED_CODE:\\s*(.+?)(?=CONFIDENCE:|$)", Pattern.DOTALL);
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("CONFIDENCE:\\s*([0-9.]+)");

    @Autowired
    public FixSuggestionService(AIProviderFactory providerFactory, FixPromptTemplates fixPromptTemplates,
                                 AnalysisFixRepository fixRepository) {
        this.providerFactory = providerFactory;
        this.fixPromptTemplates = fixPromptTemplates;
        this.fixRepository = fixRepository;
    }

    public Mono<List<AnalysisFix>> generateFixes(String jobId, List<AnalysisFinding> findings) {
        List<AnalysisFinding> fixableFindings = findings.stream()
            .filter(f -> "HIGH".equals(f.getSeverity()) || "CRITICAL".equals(f.getSeverity()))
            .filter(f -> f.getFile() != null && !f.getFile().isEmpty())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        if (fixableFindings.isEmpty()) {
            return Mono.just(List.of());
        }

        log.info("Generating fix suggestions for {} HIGH/CRITICAL findings in job {}", fixableFindings.size(), jobId);

        return Flux.fromIterable(fixableFindings)
            .flatMap(finding -> generateFix(jobId, finding))
            .collectList()
             .flatMap(fixes -> {
                 fixes.removeIf(Objects::isNull);
                 if (fixes.isEmpty()) {
                     return Mono.just(List.<AnalysisFix>of());
                 }
                 return Flux.fromIterable(fixes)
                     .flatMap(fix -> fixRepository.save(fix))
                     .collectList();
             })
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<AnalysisFix> generateFix(String jobId, AnalysisFinding finding) {
        return Mono.fromCallable(() -> {
            try {
                FixPromptTemplate template = fixPromptTemplates.getTemplate(finding.getCategory());
                FixPromptTemplate.FixContext context = FixPromptTemplate.FixContext.builder()
                    .filePath(finding.getFile())
                    .lineNumber(finding.getLine())
                    .findingMessage(finding.getMessage())
                    .suggestion(finding.getSuggestion())
                    .codeSnippet(finding.getCodeSnippet() != null ? finding.getCodeSnippet() : "")
                    .severity(finding.getSeverity())
                    .category(finding.getCategory())
                    .language(detectLanguage(finding.getFile()))
                    .build();

                String prompt = template.render(context);
                log.debug("Sending fix prompt for finding {} in file {}", finding.getId(), finding.getFile());

                String response;
                try {
                    AIProvider provider = providerFactory.getDefaultProvider();
                    response = provider.generate(prompt)
                        .subscribeOn(Schedulers.boundedElastic())
                        .block(java.time.Duration.ofSeconds(30));
                } catch (Exception e) {
                    log.warn("LLM call failed for finding {}: {}", finding.getId(), e.getMessage());
                    response = "Error: " + e.getMessage();
                }

                if (response == null || response.isEmpty() || response.startsWith("Failed") || response.startsWith("Error")) {
                    log.warn("LLM returned error for finding {}: {}", finding.getId(), response);
                    return null;
                }

                ParsedFix parsed = parseFixResponse(response);

                return AnalysisFix.builder()
                    .id(UUID.randomUUID().toString())
                    .jobId(jobId)
                    .findingId(finding.getId())
                    .file(finding.getFile())
                    .line(finding.getLine())
                    .originalCode(finding.getCodeSnippet())
                    .fixedCode(parsed.fixedCode)
                    .explanation(parsed.explanation)
                    .confidence(parsed.confidence)
                    .applied(false)
                    .createdAt(Instant.now().toString())
                    .build();

                    } catch (Exception e) {
                log.error("Error generating fix for finding {}: {}", finding.getId(), e.getMessage());
                return null;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private ParsedFix parseFixResponse(String response) {
        String explanation = "";
        String fixedCode = "";
        double confidence = 0.5;

        Matcher explanationMatcher = EXPLANATION_PATTERN.matcher(response);
        if (explanationMatcher.find()) {
            explanation = explanationMatcher.group(1).trim();
        }

        Matcher codeMatcher = FIXED_CODE_PATTERN.matcher(response);
        if (codeMatcher.find()) {
            fixedCode = codeMatcher.group(1).trim();
            if (fixedCode.startsWith("```")) {
                int firstNewline = fixedCode.indexOf('\n');
                int lastBacktick = fixedCode.lastIndexOf("```");
                if (firstNewline > 0 && lastBacktick > firstNewline) {
                    fixedCode = fixedCode.substring(firstNewline + 1, lastBacktick).trim();
                }
            }
        }

        Matcher confidenceMatcher = CONFIDENCE_PATTERN.matcher(response);
        if (confidenceMatcher.find()) {
            try {
                confidence = Double.parseDouble(confidenceMatcher.group(1).trim());
                confidence = Math.max(0.0, Math.min(1.0, confidence));
            } catch (NumberFormatException e) {
                confidence = 0.5;
            }
        }

        return new ParsedFix(explanation, fixedCode, confidence);
    }

    public boolean validateFixSyntax(AnalysisFix fix) {
        if (fix.getFixedCode() == null || fix.getFixedCode().isEmpty()) {
            return false;
        }

        String code = fix.getFixedCode();
        int openBraces = 0;
        int closeBraces = 0;
        int openParens = 0;
        int closeParens = 0;

        boolean inString = false;
        char stringChar = '"';

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if ((c == '"' || c == '\'') && !inString) {
                inString = true;
                stringChar = c;
            } else if (c == stringChar && inString) {
                if (i > 0 && code.charAt(i - 1) == '\\') continue;
                inString = false;
            } else if (!inString) {
                switch (c) {
                    case '{': openBraces++; break;
                    case '}': closeBraces++; break;
                    case '(': openParens++; break;
                    case ')': closeParens++; break;
                }
            }
        }

        return openBraces == closeBraces && openParens == closeParens;
    }

    public Mono<AnalysisFix> applyFix(String jobId, String fixId) {
        return fixRepository.findById(fixId)
            .flatMap(fix -> {
                fix.setApplied(true);
                return fixRepository.save(fix);
            })
            .doOnSuccess(f -> log.info("Applied fix {} for job {}", fixId, jobId))
            .doOnError(e -> log.error("Failed to apply fix {}: {}", fixId, e.getMessage()));
    }

    public Flux<AnalysisFix> getFixesForJob(String jobId) {
        return fixRepository.findByJobId(jobId);
    }

    private String detectLanguage(String filename) {
        if (filename == null) return "unknown";
        if (filename.endsWith(".java")) return "java";
        if (filename.endsWith(".js")) return "javascript";
        if (filename.endsWith(".ts") || filename.endsWith(".tsx")) return "typescript";
        if (filename.endsWith(".py")) return "python";
        if (filename.endsWith(".go")) return "go";
        return "unknown";
    }

    private static class ParsedFix {
        private String explanation;
        private String fixedCode;
        private double confidence;

        public ParsedFix(String explanation, String fixedCode, double confidence) {
            this.explanation = explanation;
            this.fixedCode = fixedCode;
            this.confidence = confidence;
        }
    }
}
