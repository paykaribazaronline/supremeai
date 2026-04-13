package org.example.service;

import org.example.model.Vote;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ConsensusEngine {
    private final double threshold;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public ConsensusEngine() {
        this.threshold = 0.70; // 70% threshold as requested
    }
    
    public ConsensusEngine(double threshold) {
        this.threshold = threshold;
    }

    public boolean hasReachedConsensus(List<Vote> votes) {
        if (votes == null || votes.isEmpty()) return false;
        long approvals = votes.stream().filter(Vote::isApproved).count();
        double approvalRate = (double) approvals / votes.size();
        
        boolean reached = approvalRate >= threshold;
        
        // Auto-execution via event if consensus reached
        if (reached && eventPublisher != null) {
            String appId = extractAppId(votes);
            if (appId != null) {
                eventPublisher.publishEvent(new ConsensusReachedEvent(this, appId, approvalRate));
            }
        }
        
        return reached;
    }

    private String extractAppId(List<Vote> votes) {
        // Find appId from context, assuming first vote has app ID
        // This is a placeholder since we don't have the exact Vote model structure
        return votes.isEmpty() ? null : "app-id-placeholder"; 
    }

    public double getApprovalRate(List<Vote> votes) {
        if (votes == null || votes.isEmpty()) return 0;
        long approvals = votes.stream().filter(Vote::isApproved).count();
        return (double) approvals / votes.size();
    }

    public String collectImprovements(List<Vote> votes) {
        return votes.stream()
                .map(v -> extractSuggestion(v.getComments()))
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining("\n- "));
    }

    private String extractSuggestion(String feedback) {
        if (feedback == null) return "";
        if (feedback.contains("Suggestion:")) {
            return feedback.split("Suggestion:")[1].trim();
        }
        return "";
    }

    public boolean noMoreImprovementsNeeded(List<Vote> votes) {
        return collectImprovements(votes).isEmpty();
    }

    public static class ConsensusReachedEvent extends org.springframework.context.ApplicationEvent {
        private final String appId;
        private final double approvalRate;

        public ConsensusReachedEvent(Object source, String appId, double approvalRate) {
            super(source);
            this.appId = appId;
            this.approvalRate = approvalRate;
        }

        public String getAppId() { return appId; }
        public double getApprovalRate() { return approvalRate; }
    }
}
