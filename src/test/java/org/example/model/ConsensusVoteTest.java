package org.example.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsensusVoteTest {

    @Test
    void calculatesConsensusFromNormalizedWinningResponse() {
        ConsensusVote vote = new ConsensusVote();
        vote.addResponse("openai-gpt4", "This is a much longer answer that should still count toward the same normalized bucket");
        vote.addResponse("anthropic-claude", "This is a much longer answer that should still count toward the same normalized bucket with more detail");
        vote.setVotes(Map.of(
            "This is a much longer answer that should still count", 2
        ));
        vote.setWinningResponse("This is a much longer answer that should still count toward the same normalized bucket");

        assertEquals(100, vote.getConsensusPercentage());
    }
}