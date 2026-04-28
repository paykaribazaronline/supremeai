package com.supremeai.controller;

import com.supremeai.learning.UserCodeLearningService;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.model.SystemLearning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Learning Ingest Controller - Public endpoints for VS Code Extension
 * Receives real-time learning data from VS Code without requiring admin auth
 */
@RestController
@RequestMapping("/api/v1/learning")
@CrossOrigin(origins = "*", allowedHeaders = "*") // Allow VS Code extension origin
public class LearningIngestController {

    private static final Logger log = LoggerFactory.getLogger(LearningIngestController.class);

    @Autowired
    private UserCodeLearningService userCodeLearningService;

    @Autowired
    private GlobalKnowledgeBase globalKnowledgeBase;

    /**
     * Receive code edit event from VS Code extension
     * POST /api/v1/learning/code-edit
     */
    @PostMapping("/code-edit")
    public ResponseEntity<Map<String, Object>> receiveCodeEdit(@RequestBody LearningEvent event) {
        log.info("Received code edit event: {} for file: {}", event.getTaskId(), event.getFilePath());
        
        try {
            userCodeLearningService.learnFromUserEdit(
                event.getTaskId(),
                event.getOriginalCode(),
                event.getEditedCode(),
                event.getContext()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Code edit learned successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to process code edit: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Receive error report from VS Code extension
     * POST /api/v1/learning/error
     */
    @PostMapping("/error")
    public ResponseEntity<Map<String, Object>> reportError(@RequestBody ErrorEvent event) {
        log.info("Received error report: {} at {}", event.getErrorType(), event.getFilePath());
        
        try {
            // Convert to SystemLearning pattern and record as learning
            String errorSignature = generateErrorSignature(event);
            
            // Record failure for this code pattern
            userCodeLearningService.recordFailure(errorSignature, event.getCodeSnippet() != null ? event.getCodeSnippet() : "");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Error reported and learned"
            ));
        } catch (Exception e) {
            log.error("Failed to process error report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Receive suggestion feedback (accept/reject) from VS Code
     * POST /api/v1/learning/feedback
     */
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> receiveFeedback(@RequestBody FeedbackEvent event) {
        log.info("Received feedback: {} for suggestion: {}", event.isAccepted(), event.getSuggestionId());
        
        try {
            String category = event.isAccepted() ? "SUGGESTION_ACCEPTED" : "SUGGESTION_REJECTED";
            
            // Create learning pattern from feedback
            SystemLearning pattern = new SystemLearning();
            pattern.setId("feedback_" + event.getSuggestionId() + "_" + System.currentTimeMillis());
            pattern.setLearningType(category);
            pattern.setCategory("USER_FEEDBACK");
            pattern.setContent(String.format("User %s suggestion: %s", 
                event.isAccepted() ? "accepted" : "rejected", 
                event.getContext()));
            pattern.setConfidenceScore(event.isAccepted() ? 0.9 : 0.3); // Higher confidence for accepts
            pattern.setLearnedAt(java.time.LocalDateTime.now());
            pattern.setSource("vscode-extension");
            
            // Save via GlobalKnowledgeBase
            globalKnowledgeBase.saveSolutionMemory(pattern).subscribe();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Feedback recorded"
            ));
        } catch (Exception e) {
            log.error("Failed to process feedback: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get learning statistics for VS Code status bar
     * GET /api/v1/learning/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            long patternCount = globalKnowledgeBase.countSolutions();
            
            Map<String, Object> stats = Map.of(
                "learningCount", patternCount,
                "status", "active",
                "lastUpdated", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "learningCount", 0,
                "status", "error"
            ));
        }
    }

    /**
     * Health check endpoint
     * GET /api/v1/learning/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "learning-ingest",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    /**
     * Generate error signature string from error event
     */
    private String generateErrorSignature(ErrorEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append(event.getErrorType()).append(": ");
        sb.append(event.getErrorMessage());
        if (event.getErrorCode() != null) {
            sb.append(" (").append(event.getErrorCode()).append(")");
        }
        sb.append(" @ ").append(event.getFilePath()).append(":").append(event.getLineNumber());
        return sb.toString();
    }

    // DTOs for incoming events

    public static class LearningEvent {
        private String type; // "CODE_EDIT"
        private Map<String, Object> data;
        private String sessionId;

        public String getTaskId() {
            return (String) data.get("taskId");
        }

        public String getOriginalCode() {
            return (String) data.get("originalCode");
        }

        public String getEditedCode() {
            return (String) data.get("editedCode");
        }

        public String getContext() {
            return (String) data.get("context");
        }

        public String getFilePath() {
            return (String) data.get("filePath");
        }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class ErrorEvent {
        private String errorType;
        private String errorMessage;
        private String errorCode;
        private String filePath;
        private int lineNumber;
        private Integer columnNumber;
        private String codeSnippet;
        private String severity;

        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
        public Integer getColumnNumber() { return columnNumber; }
        public void setColumnNumber(Integer columnNumber) { this.columnNumber = columnNumber; }
        public String getCodeSnippet() { return codeSnippet; }
        public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }

    public static class FeedbackEvent {
        private String suggestionId;
        private boolean accepted;
        private String modifiedCode;
        private String context;
        private String taskId;

        public String getSuggestionId() { return suggestionId; }
        public void setSuggestionId(String suggestionId) { this.suggestionId = suggestionId; }
        public boolean isAccepted() { return accepted; }
        public void setAccepted(boolean accepted) { this.accepted = accepted; }
        public String getModifiedCode() { return modifiedCode; }
        public void setModifiedCode(String modifiedCode) { this.modifiedCode = modifiedCode; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
    }
}
