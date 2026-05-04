package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Plan 15: Hybrid Voice System.
 *
 * Provides voice-to-text processing with support for:
 * - English (primary, high accuracy)
 * - Bengali/Bangla (supported via Web Speech API on frontend; normalized here)
 * - Text normalization and cleanup post-transcription
 *
 * Architecture: Voice capture happens on the frontend (Web Speech API / MediaRecorder).
 * The transcribed text is sent to this service for normalization, language detection,
 * and storage. Actual STT conversion is browser-side to avoid latency and API costs.
 *
 * Note: Bengali accuracy via Web Speech API is ~70-80%. This service applies
 * post-processing corrections for common Bengali transcription errors.
 */
@Service
public class HybridVoiceService {

    private static final Logger logger = LoggerFactory.getLogger(HybridVoiceService.class);

    @Value("${voice.max.text.length:5000}")
    private int maxTextLength;

    @Value("${voice.bengali.correction.enabled:true}")
    private boolean bengaliCorrectionEnabled;

    // Common Bengali transcription corrections (romanized misreadings → correct forms)
    private static final Map<String, String> BENGALI_CORRECTIONS = Map.of(
        "ami", "আমি",
        "tumi", "তুমি",
        "apni", "আপনি",
        "ki", "কি",
        "koro", "করো",
        "koro na", "করো না",
        "hobe", "হবে",
        "dao", "দাও",
        "bol", "বলো"
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Process transcribed text from the frontend voice capture.
     * Normalizes, detects language, and applies corrections.
     *
     * @param rawText    Raw transcription from Web Speech API
     * @param language   Language hint from frontend (e.g., "bn-BD", "en-US")
     * @return VoiceProcessingResult with normalized text and metadata
     */
    public VoiceProcessingResult processTranscription(String rawText, String language) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return VoiceProcessingResult.empty();
        }

        String text = rawText.trim();

        // Truncate if too long
        if (text.length() > maxTextLength) {
            text = text.substring(0, maxTextLength);
            logger.warn("[VOICE] Text truncated to {} chars", maxTextLength);
        }

        // Detect language if not specified
        DetectedLanguage detectedLang = detectLanguage(text, language);

        // Apply corrections based on language
        String normalizedText = normalize(text, detectedLang);

        // Extract intent hints (command-like phrases)
        List<String> intentHints = extractIntentHints(normalizedText);

        logger.debug("[VOICE] Processed {} chars lang={} hints={}", normalizedText.length(),
            detectedLang.getCode(), intentHints.size());

        return new VoiceProcessingResult(normalizedText, detectedLang, intentHints, true);
    }

    /**
     * Validate that the voice input is usable (not empty, not too short, not noise).
     */
    public ValidationResult validate(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return ValidationResult.invalid("Empty transcription");
        }
        if (rawText.trim().length() < 3) {
            return ValidationResult.invalid("Transcription too short — likely background noise");
        }
        // Check for noise-like single repeated characters
        if (rawText.matches("[aeiouAEIOU\\s]+")) {
            return ValidationResult.invalid("Likely background noise detected");
        }
        return ValidationResult.valid();
    }

    /**
     * Get supported languages.
     */
    public List<SupportedLanguage> getSupportedLanguages() {
        return List.of(
            new SupportedLanguage("en-US", "English (US)", "high"),
            new SupportedLanguage("en-GB", "English (UK)", "high"),
            new SupportedLanguage("bn-BD", "Bengali (Bangladesh)", "medium"),
            new SupportedLanguage("bn-IN", "Bengali (India)", "medium"),
            new SupportedLanguage("hi-IN", "Hindi", "medium")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private DetectedLanguage detectLanguage(String text, String hint) {
        if (hint != null && !hint.isEmpty()) {
            return new DetectedLanguage(hint, "hint");
        }
        // Simple Bengali Unicode detection
        long bengaliChars = text.chars()
            .filter(c -> c >= 0x0980 && c <= 0x09FF)
            .count();
        if (bengaliChars > text.length() * 0.3) {
            return new DetectedLanguage("bn-BD", "auto");
        }
        return new DetectedLanguage("en-US", "auto");
    }

    private String normalize(String text, DetectedLanguage lang) {
        String result = text;

        // Basic cleanup
        result = result.replaceAll("\\s+", " ").trim();
        result = result.replaceAll("[.]{2,}", ".");

        // Bengali romanized corrections
        if (bengaliCorrectionEnabled && lang.getCode().startsWith("bn")) {
            for (Map.Entry<String, String> entry : BENGALI_CORRECTIONS.entrySet()) {
                result = result.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
            }
        }

        // Capitalize first letter of sentences
        if (!result.isEmpty()) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        }

        return result;
    }

    private List<String> extractIntentHints(String text) {
        java.util.List<String> hints = new java.util.ArrayList<>();
        String lower = text.toLowerCase();

        if (lower.startsWith("create") || lower.startsWith("make") || lower.startsWith("build")) {
            hints.add("COMMAND_CREATE");
        }
        if (lower.startsWith("delete") || lower.startsWith("remove")) {
            hints.add("COMMAND_DELETE");
        }
        if (lower.startsWith("show") || lower.startsWith("list") || lower.startsWith("get")) {
            hints.add("COMMAND_QUERY");
        }
        if (lower.contains("?")) {
            hints.add("QUESTION");
        }
        if (lower.startsWith("set") || lower.startsWith("update") || lower.startsWith("change")) {
            hints.add("COMMAND_UPDATE");
        }
        return hints;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner Models
    // ─────────────────────────────────────────────────────────────────────────

    public static class DetectedLanguage {
        private final String code;
        private final String source; // "hint" or "auto"

        public DetectedLanguage(String code, String source) {
            this.code = code;
            this.source = source;
        }

        public String getCode() { return code; }
        public String getSource() { return source; }
    }

    public static class VoiceProcessingResult {
        private final String normalizedText;
        private final DetectedLanguage language;
        private final List<String> intentHints;
        private final boolean success;

        public VoiceProcessingResult(String normalizedText, DetectedLanguage language,
                                     List<String> intentHints, boolean success) {
            this.normalizedText = normalizedText;
            this.language = language;
            this.intentHints = intentHints;
            this.success = success;
        }

        public static VoiceProcessingResult empty() {
            return new VoiceProcessingResult("", new DetectedLanguage("unknown", "auto"), List.of(), false);
        }

        public String getNormalizedText() { return normalizedText; }
        public DetectedLanguage getLanguage() { return language; }
        public List<String> getIntentHints() { return intentHints; }
        public boolean isSuccess() { return success; }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String reason;

        private ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static ValidationResult valid() { return new ValidationResult(true, null); }
        public static ValidationResult invalid(String reason) { return new ValidationResult(false, reason); }

        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
    }

    public static class SupportedLanguage {
        private final String code;
        private final String displayName;
        private final String accuracy; // high, medium, low

        public SupportedLanguage(String code, String displayName, String accuracy) {
            this.code = code;
            this.displayName = displayName;
            this.accuracy = accuracy;
        }

        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
        public String getAccuracy() { return accuracy; }
    }
}
