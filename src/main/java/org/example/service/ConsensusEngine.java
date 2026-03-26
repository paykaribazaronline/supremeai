package org.example.service;

import org.example.model.Vote;
import java.util.List;
import java.util.stream.Collectors;

public class ConsensusEngine {
    private final double threshold;

    public ConsensusEngine(double threshold) {
        this.threshold = threshold;
    }

    public boolean hasReachedConsensus(List<Vote> votes) {
        if (votes == null || votes.isEmpty()) return false;
        long approvals = votes.stream().filter(Vote::isApproved).count();
        double approvalRate = (double) approvals / votes.size();
        return approvalRate >= threshold;
    }

    public double getApprovalRate(List<Vote> votes) {
        if (votes == null || votes.isEmpty()) return 0;
        long approvals = votes.stream().filter(Vote::isApproved).count();
        return (double) approvals / votes.size();
    }

    public String collectImprovements(List<Vote> votes) {
        return votes.stream()
                .map(v -> extractSuggestion(v.getReason()))
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining("\n- "));
    }

    private String extractSuggestion(String feedback) {
        if (feedback.contains("Suggestion:")) {
            return feedback.split("Suggestion:")[1].trim();
        }
        return "";
    }

    public boolean noMoreImprovementsNeeded(List<Vote> votes) {
        return collectImprovements(votes).isEmpty();
    }
}
