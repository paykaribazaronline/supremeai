package com.supremeai.intelligence.voting;

import com.supremeai.provider.AIProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The actual Multi-AI Voting Council.
 */
@Service
public class CouncilVotingSystem {

    private static final Logger log = LoggerFactory.getLogger(CouncilVotingSystem.class);
    private final VotingTopicGenerator topicGenerator;

    public CouncilVotingSystem(VotingTopicGenerator topicGenerator) {
        this.topicGenerator = topicGenerator;
    }

    /**
     * Conducts a vote among multiple AI models to approve a risky action.
     */
    public boolean conductVote(String changeType, String codeSnippet, List<AIProviderType> councilMembers) {

        log.info("\n[Council Voting] Initiating vote for major change: {}", changeType);

        // 1. System intelligently figures out WHAT to ask the council
        VotingTopic topic = topicGenerator.generateTopicForMajorChange(changeType, codeSnippet);
        log.info("[Council Voting] Formulated Question: {}", topic.getQuestionToAsk());

        int approveCount = 0;
        int rejectCount = 0;
        
        // 2. Ask each AI model on the council
        for (AIProviderType member : councilMembers) {
            log.debug(" -> Asking {}...", member.name());

            // Simulate calling the AI API with the context and targeted question
            boolean voteApprove = simulateAIVote(member, topic);

            if (voteApprove) {
                log.debug("Voted: APPROVE");
                approveCount++;
            } else {
                log.debug("Voted: REJECT (Raised concerns)");
                rejectCount++;
            }
        }

        // 3. Tally the votes (Requires majority)
        boolean finalDecision = approveCount > rejectCount;

        log.info("[Council Voting] Final Result: {} Approve, {} Reject -> Decision: {}",
                approveCount, rejectCount, finalDecision ? "PROCEED" : "ABORT");

        return finalDecision;
    }

    private boolean simulateAIVote(AIProviderType member, VotingTopic topic) {
        // In reality, this would send `topic.getContext() + "\n" + topic.getQuestionToAsk()` to the AI via API
        // If the AI response contains "looks safe" or "approve", return true. Else false.
        
        // Simulating a scenario where Claude is strict on Security
        if (topic.getCategory().equals("SECURITY") && member.name().contains("CLAUDE")) {
             return Math.random() > 0.4; // 60% chance to approve
        }
        
        return Math.random() > 0.2; // 80% chance to approve for others
    }
}