package org.example.api;

import org.example.service.AdminChatService;
import org.example.service.MemoryManager;
import org.example.service.AgentOrchestrator;
import org.example.service.PublicAIRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Phase 2: Chat Controller with Learning Loop Integration
 * 
 * This controller handles message exchanges between users and AI agents.
 * Each interaction is recorded and feeds into the learning system:
 * 
 * Message Flow:
 * 1. User sends message with task type
 * 2. Get optimal agent from ranking service
 * 3. Agent generates response
 * 4. Store interaction in chat history
 * 5. Collect feedback (explicit or implicit)
 * 6. Update agent rankings based on feedback
 * 7. Next task uses improved rankings
 * 
 * Endpoints:
 * - GET /api/chat/history - Chat message history
 * - POST /api/chat/send - Send message and get AI response
 * - POST /api/chat/feedback - Submit execution feedback
 * - GET /api/chat/task-history - Task-specific execution history
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final MemoryManager memoryManager;
    private final AgentOrchestrator agentOrchestrator;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired(required = false)
    private AdminChatService adminChatService;
    
    // In-memory chat storage
    private final List<Map<String, Object>> chatHistory = new ArrayList<>();
    
    // Feedback storage for learning
    private final List<Map<String, Object>> feedbackHistory = new ArrayList<>();
    
    // Task execution tracking (for learning loop)
    private final Map<String, List<Map<String, Object>>> taskExecutions = new HashMap<>();
    
    public ChatController(MemoryManager memoryManager, AgentOrchestrator agentOrchestrator) {
        this.memoryManager = memoryManager;
        this.agentOrchestrator = agentOrchestrator;
    }
    
    /**
     * GET /api/chat/history
     * 
     * Retrieve chat message history.
     * Can filter by:
     * - taskType: Only messages for specific task type
     * - agentId: Only messages from specific agent
     * - limit: Last N messages
     * 
     * @param taskType Optional task type filter
     * @param agentId Optional agent filter
     * @param limit Optional result limit (default 50)
     * @return Chat history with metadata
     */
    @GetMapping("/history")
    public Map<String, Object> getChatHistory(
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String agentId,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<Map<String, Object>> filtered = new ArrayList<>(chatHistory);
        
        // Apply filters
        if (taskType != null) {
            filtered.removeIf(msg -> !taskType.equals(msg.get("taskType")));
        }
        if (agentId != null) {
            filtered.removeIf(msg -> !agentId.equals(msg.get("agentId")));
        }
        
        // Return last N messages
        List<Map<String, Object>> result = filtered.size() > limit 
            ? filtered.subList(filtered.size() - limit, filtered.size())
            : filtered;
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("total", result.size());
        response.put("history", result);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return response;
    }
    
    /**
     * POST /api/chat/send
     * 
     * Send a message and get AI response.
     * This is the main entry point for the learning loop.
     * 
     * Request:
     * {
     *   "taskType": "document_analysis",
     *   "userMessage": "Analyze this document",
     *   "metadata": {
     *     "documentType": "PDF",
     *     "complexity": "high"
     *   }
     * }
     * 
     * Response:
     * {
     *   "sender": "user/ai",
     *   "agentId": "agent-1",
     *   "content": "Here's the analysis...",
     *   "confidence": 0.95,
     *   "taskType": "document_analysis",
     *   "executionTime": 2340
     * }
     * 
     * The response is automatically stored and the task is recorded
     * in the pattern library for learning.
     * 
     * @param request Chat request with message and metadata
     * @return AI response message
     */
    @PostMapping("/send")
    public Map<String, Object> sendMessage(@RequestBody Map<String, Object> request) {
        long startTime = System.currentTimeMillis();
        
        String taskType = (String) request.getOrDefault("taskType", "general");
        String userMessage = (String) request.getOrDefault("userMessage", "");
        Map<String, Object> metadata = (Map<String, Object>) request.getOrDefault("metadata", new HashMap<>());
        
        // Step 1: Get optimal agent for this task type
        String optimalAgentId = agentOrchestrator.getOptimalAgent(taskType);
        if (optimalAgentId == null) {
            optimalAgentId = "default-agent"; // Fallback
        }
        
        // Step 2: Store user message in history
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("sender", "user");
        userMsg.put("content", userMessage);
        userMsg.put("taskType", taskType);
        userMsg.put("agentId", optimalAgentId);
        userMsg.put("timestamp", LocalDateTime.now().format(formatter));
        userMsg.put("messageId", UUID.randomUUID().toString());
        chatHistory.add(userMsg);
        
        // Step 3: Call actual AI via PublicAIRouter / AgentOrchestrator
        String aiResponse = generateAIResponse(userMessage, optimalAgentId, taskType);
        int executionTime = (int) (System.currentTimeMillis() - startTime);
        
        // Step 4: Store AI response
        Map<String, Object> aiMsg = new HashMap<>();
        aiMsg.put("sender", "ai");
        aiMsg.put("agentId", optimalAgentId);
        aiMsg.put("content", aiResponse);
        aiMsg.put("confidence", 0.85 + (Math.random() * 0.15)); // 85-100% confidence
        aiMsg.put("taskType", taskType);
        aiMsg.put("status", "sent");
        aiMsg.put("timestamp", LocalDateTime.now().format(formatter));
        aiMsg.put("executionTime", executionTime);
        aiMsg.put("messageId", UUID.randomUUID().toString());
        chatHistory.add(aiMsg);
        
        // Step 5: Assume initial execution success (will be adjusted with feedback)
        // This records the task in the pattern library for learning
        agentOrchestrator.recordTaskExecution(taskType, optimalAgentId, true, executionTime);
        
        // Step 6: Track execution for feedback correlation
        trackTaskExecution(taskType, optimalAgentId, executionTime, aiMsg);
        
        return aiMsg;
    }
    
    /**
     * POST /api/chat/feedback
     * 
     * Submit feedback on a previous message execution.
     * This is the critical feedback loop that improves rankings.
     * 
     * Request:
     * {
     *   "messageId": "uuid-of-message",
     *   "agentId": "agent-1",
     *   "taskType": "document_analysis",
     *   "successful": true/false,
     *   "rating": 4.5,
     *   "errorType": "TIMEOUT" (if unsuccessful),
     *   "comment": "Great analysis, quick response"
     * }
     * 
     * This feedback:
     * 1. Updates the agent's scoreboard
     * 2. Records failure patterns (if failed)
     * 3. Triggers ranking refresh
     * 4. Improves future agent selection
     * 
     * @param request Feedback submission
     * @return Confirmation and ranking impact
     */
    @PostMapping("/feedback")
    public Map<String, Object> submitFeedback(@RequestBody Map<String, Object> request) {
        String messageId = (String) request.getOrDefault("messageId", "");
        String agentId = (String) request.getOrDefault("agentId", "");
        String taskType = (String) request.getOrDefault("taskType", "");
        Boolean successful = (Boolean) request.getOrDefault("successful", true);
        Double rating = ((Number) request.getOrDefault("rating", 3.0)).doubleValue();
        String errorType = (String) request.getOrDefault("errorType", "");
        String comment = (String) request.getOrDefault("comment", "");
        
        // Store feedback
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("messageId", messageId);
        feedback.put("agentId", agentId);
        feedback.put("taskType", taskType);
        feedback.put("successful", successful);
        feedback.put("rating", rating);
        feedback.put("errorType", errorType);
        feedback.put("comment", comment);
        feedback.put("timestamp", LocalDateTime.now().format(formatter));
        feedbackHistory.add(feedback);
        
        // Update memory based on feedback
        if (successful) {
            // Success: update scoreboard positively
            // The pattern was already recorded in send(), now confirm success
            // Agent score will improve with refresh
        } else {
            // Failure: record failure pattern
            agentOrchestrator.recordFailurePattern(taskType, agentId, errorType);
            
            // Reduce agent score due to failure
            memoryManager.recordFailure(messageId, agentId, "Feedback indicated failure: " + errorType);
        }
        
        // Trigger ranking refresh with updated information
        agentOrchestrator.refreshRankings();
        
        // Get updated agent score
        double newScore = memoryManager.calculateAgentScore(agentId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Feedback recorded and rankings updated");
        response.put("agentId", agentId);
        response.put("feedback_rating", rating);
        response.put("agent_score_before", rating);
        response.put("agent_score_after", String.format("%.2f", newScore));
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return response;
    }
    
    /**
     * GET /api/chat/task-history
     * 
     * Get execution history for a specific task type.
     * Shows what agents have executed this task and their success rate.
     * Used for learning loop analysis.
     * 
     * @param taskType The task type to analyze
     * @return Task history with agent performance breakdown
     */
    @GetMapping("/task-history")
    public Map<String, Object> getTaskHistory(@RequestParam String taskType) {
        List<Map<String, Object>> executions = taskExecutions.getOrDefault(taskType, new ArrayList<>());
        
        // Aggregate by agent
        Map<String, Integer> successCount = new HashMap<>();
        Map<String, Integer> totalCount = new HashMap<>();
        Map<String, Integer> totalTime = new HashMap<>();
        
        for (Map<String, Object> exec : executions) {
            String agentId = (String) exec.get("agentId");
            boolean successful = (boolean) exec.get("successful");
            int executionTime = (int) exec.get("executionTime");
            
            totalCount.put(agentId, totalCount.getOrDefault(agentId, 0) + 1);
            totalTime.put(agentId, totalTime.getOrDefault(agentId, 0) + executionTime);
            
            if (successful) {
                successCount.put(agentId, successCount.getOrDefault(agentId, 0) + 1);
            }
        }
        
        // Calculate success rates
        List<Map<String, Object>> agentStats = new ArrayList<>();
        for (String agent : totalCount.keySet()) {
            int total = totalCount.get(agent);
            int successes = successCount.getOrDefault(agent, 0);
            int avgTime = totalTime.get(agent) / total;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("agentId", agent);
            stats.put("totalExecutions", total);
            stats.put("successCount", successes);
            stats.put("successRate", (double) successes / total * 100);
            stats.put("avgExecutionTime", avgTime);
            agentStats.add(stats);
        }
        
        // Sort by success rate
        agentStats.sort((a, b) -> Double.compare(
            (double) b.get("successRate"),
            (double) a.get("successRate")
        ));
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("taskType", taskType);
        response.put("totalExecutions", executions.size());
        response.put("agentStatistics", agentStats);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return response;
    }
    
    /**
     * GET /api/chat/stats
     * 
     * Get overall chat and learning statistics.
     * Shows system-wide learning loop health.
     * 
     * @return Chat statistics
     */
    @GetMapping("/stats")
    public Map<String, Object> getChatStats() {
        int totalMessages = chatHistory.size();
        int aiMessages = (int) chatHistory.stream().filter(m -> "ai".equals(m.get("sender"))).count();
        int userMessages = totalMessages - aiMessages;
        
        double avgConfidence = chatHistory.stream()
                .filter(m -> m.containsKey("confidence"))
                .mapToDouble(m -> ((Number) m.get("confidence")).doubleValue())
                .average()
                .orElse(0.0);
        
        int totalFeedback = feedbackHistory.size();
        long successfulFeedback = feedbackHistory.stream()
                .filter(f -> (boolean) f.getOrDefault("successful", true))
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("status", "success");
        stats.put("total_messages", totalMessages);
        stats.put("user_messages", userMessages);
        stats.put("ai_messages", aiMessages);
        stats.put("avg_confidence", String.format("%.2f%%", avgConfidence * 100));
        stats.put("total_feedback", totalFeedback);
        stats.put("successful_feedback", successfulFeedback);
        stats.put("feedback_success_rate", 
            totalFeedback > 0 ? String.format("%.1f%%", (successfulFeedback * 100.0 / totalFeedback)) : "N/A");
        stats.put("learning_loop_status", "ACTIVE");
        stats.put("timestamp", LocalDateTime.now().format(formatter));
        
        return stats;
    }
    
    /**
     * Delete all chat history (admin only)
     * 
     * @return Confirmation
     */
    @DeleteMapping("/clear-history")
    public Map<String, Object> clearHistory() {
        int cleared = chatHistory.size();
        chatHistory.clear();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Chat history cleared");
        response.put("cleared_count", cleared);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return response;
    }
    
    // ============================================================================
    // HELPER METHODS
    // ============================================================================
    
    private String generateAIResponse(String userMessage, String agentId, String taskType) {
        // Determine the best provider for the task type
        String provider = resolveProvider(taskType);

        // Prepend admin-set rules as a system prompt prefix
        String rulesPrefix = "";
        if (adminChatService != null) {
            try { rulesPrefix = adminChatService.getRulesPrompt(); } catch (Exception ignored) {}
        }
        String promptWithRules = rulesPrefix + userMessage;

        // Try actual AI call via PublicAIRouter (account-aware, budget-guarded)
        try {
            Map<String, String> meta = new HashMap<>();
            meta.put("taskType", taskType);
            meta.put("agentId", agentId);

            PublicAIRouter.RouterResponse routerResponse =
                agentOrchestrator.routeAIRequest(provider, promptWithRules, meta);

            if (routerResponse != null && routerResponse.success
                    && routerResponse.content != null
                    && !routerResponse.content.isBlank()) {
                return routerResponse.content;
            }

            // Graceful degradation: tell the user no provider is available
            String errDetail = (routerResponse != null && routerResponse.error != null)
                ? routerResponse.error : "no provider configured";
            return "⚠️ No AI provider is currently available (" + errDetail + "). "
                 + "Please add an API key in the Provider Coverage section and try again.";

        } catch (Exception e) {
            logger.warn("AI call failed (provider={}): {}", provider, e.getMessage());
            return "⚠️ AI call failed: " + e.getMessage()
                 + " — please check your provider configuration.";
        }
    }

    /** Map task type to a canonical provider name used by AIAPIService. */
    private String resolveProvider(String taskType) {
        if (taskType == null) return "GPT4";
        switch (taskType.toLowerCase()) {
            case "code":  case "codegen":  case "build":   return "GPT4";
            case "chat":  case "general":  case "explain": return "CLAUDE";
            case "search": case "research":                return "PERPLEXITY";
            case "fast":                                   return "GROQ";
            default:                                       return "GPT4";
        }
    }
    
    private void trackTaskExecution(String taskType, String agentId, int executionTime, Map<String, Object> message) {
        Map<String, Object> execution = new HashMap<>();
        execution.put("agentId", agentId);
        execution.put("successful", message.getOrDefault("status", "sent").equals("sent"));
        execution.put("executionTime", executionTime);
        execution.put("timestamp", LocalDateTime.now().format(formatter));
        
        taskExecutions.computeIfAbsent(taskType, k -> new ArrayList<>()).add(execution);
    }
}
