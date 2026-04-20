package com.supremeai.intelligence.voting;

import com.supremeai.fallback.AIProvider;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The actual Multi-AI Voting Council.
 */
@Service
public class CouncilVotingSystem {

    private final VotingTopicGenerator topicGenerator;

    public CouncilVotingSystem(VotingTopicGenerator topicGenerator) {
        this.topicGenerator = topicGenerator;
    }

    /**
     * Conducts a vote among multiple AI models to approve a risky action.
     */
    public boolean conductVote(String changeType, String codeSnippet, List<AIProvider> councilMembers) {
        
        System.out.println("\n[Council Voting] Initiating vote for major change: " + changeType);
        
        // 1. System intelligently figures out WHAT to ask the council
        VotingTopic topic = topicGenerator.generateTopicForMajorChange(changeType, codeSnippet);
        System.out.println("[Council Voting] Formulated Question: " + topic.getQuestionToAsk());

        int approveCount = 0;
        int rejectCount = 0;
        
        // 2. Ask each AI model on the council
        for (AIProvider member : councilMembers) {
            System.out.print(" -> Asking " + member.name() + "... ");
            
            // Simulate calling the AI API with the context and targeted question
            boolean voteApprove = simulateAIVote(member, topic);
            
            if (voteApprove) {
                System.out.println("Voted: APPROVE");
                approveCount++;
            } else {
                System.out.println("Voted: REJECT (Raised concerns)");
                rejectCount++;
            }
        }

        // 3. Tally the votes (Requires majority)
        boolean finalDecision = approveCount > rejectCount;
        
        System.out.printf("[Council Voting] Final Result: %d Approve, %d Reject -> Decision: %s\n\n", 
                          approveCount, rejectCount, finalDecision ? "PROCEED" : "ABORT");

        return finalDecision;
    }

    private boolean simulateAIVote(AIProvider member, VotingTopic topic) {
        // In reality, this would send `topic.getContext() + "\n" + topic.getQuestionToAsk()` to the AI via API
        // If the AI response contains "looks safe" or "approve", return true. Else false.
        
        // Simulating a scenario where Claude is strict on Security
        if (topic.getCategory().equals("SECURITY") && member.name().contains("CLAUDE")) {
             return Math.random() > 0.4; // 60% chance to approve
        }
        
        return Math.random() > 0.2; // 80% chance to approve for others
    }
}