package com.supremeai.service;

import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ProviderVote;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;

@Service
public class MultiAIVotingService {
    public MultiAIVotingService(VotingOrchestrator votingOrchestrator, SoloModeService soloModeService, BrowserResearchService browserResearchService) {
        this.votingOrchestrator = votingOrchestrator;
        this.soloModeService = soloModeService;
        this.browserResearchService = browserResearchService;
    }





    public MultiAIVotingService() {}

    public Mono<ConsensusResult> askContextualAIs(String prompt, int x, int timeoutMs) {
        return votingOrchestrator.askContextualAIs(prompt, x, timeoutMs);
    }

    public Mono<VotingResult> executeEnsembleVoting(String prompt, List<String> selectedModels, long timeoutMs) {
        return votingOrchestrator.executeEnsembleVoting(prompt, selectedModels, timeoutMs);
    }

    public Mono<VotingResult> executeEnsembleVoting(String prompt, List<String> selectedModels, long timeoutMs, String taskType) {
        return votingOrchestrator.executeEnsembleVoting(prompt, selectedModels, timeoutMs, taskType);
    }

    public Flux<ProviderVote> streamVotes(String prompt, List<String> selectedModels, long timeoutMs) {
        return votingOrchestrator.streamVotes(prompt, selectedModels, timeoutMs);
    }

    public Mono<Boolean> conductApprovalVote(String changeType, String codeSnippet, List<String> councilMembers) {
        return votingOrchestrator.conductApprovalVote(changeType, codeSnippet, councilMembers);
    }

    public Mono<VotingDecision> conductDecisionVote(String question, String context) {
        return votingOrchestrator.conductDecisionVote(question, context);
    }

    public Mono<ConsensusResult> askConsensus(String question, List<String> providerNames, long timeoutMs) {
        return votingOrchestrator.askConsensus(question, providerNames, timeoutMs);
    }

    public Mono<ConsensusResult> askContextualConsensus(String question, int count, long timeoutMs) {
        return votingOrchestrator.askContextualConsensus(question, count, timeoutMs);
    }

    public Flux<ConsensusVote> getConsensusHistory(int limit) {
        return votingOrchestrator.getConsensusHistory(limit);
    }
}
