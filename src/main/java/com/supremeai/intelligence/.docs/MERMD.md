# MERMD - Intelligence & AI Systems

## Overview
The Intelligence feature provides AI reasoning, voting systems, profiling, and human interaction capabilities.

## How It Works

### Architecture Flow
```
Input → Intelligence Router → Specialized AI Services → Voting/Consensus → Output
```

### Key Components

| Component | File | Purpose |
|-----------|------|---------|
| `CouncilVotingSystem` | `intelligence/voting/CouncilVotingSystem.java` | Multi-AI voting |
| `VotingDecision` | `agentorchestration/VotingDecision.java` | Voting results |
| `AdaptiveAgentOrchestrator` | `agentorchestration/AdaptiveAgentOrchestrator.java` | Agent orchestration |
| `AIProfiler` | `intelligence/profiling/AIProfiler.java` | AI performance profiling |
| `SystemSuggestionService` | `intelligence/SystemSuggestionService.java` | System-wide suggestions |
| `ParallelCodeAnalyzer` | `intelligence/ParallelCodeAnalyzer.java` | Parallel analysis |
| `StressTestService` | `intelligence/StressTestService.java` | Load testing |

### Voting System

#### CouncilVotingSystem
- Distributes questions to multiple AI agents
- Aggregates responses
- Calculates confidence scores

#### VotingTopic
```java
class VotingTopic {
    String topicId;
    String question;
    List<AIAnswer> answers;
    VotingResult result;
}
```

### Human Interaction

#### IntentPredictor
- Predicts user intentions
- Reduces clarification needs

#### RequirementClarification
- Handles ambiguous requirements
- Interactive clarification flow

#### DeveloperDNA
- User preference profiling
- Code style analysis

### Profiling

#### TaskPerformanceProfile
- Tracks AI task performance
- Identifies improvement areas

#### AIProfiler
- Real-time performance metrics
- Resource utilization tracking

### Specialized Services

| Service | Purpose |
|---------|---------|
| `RippleEffectPredictor` | Predicts cascading changes |
| `LightningCache` | Fast response caching |
| `ContextualAIRankingService` | AI model ranking |
| `HumanUnderstandingService` | Human-AI interaction |

### Integration Points
- `AIAgentsController` - AI agent management
- `VotingController` - Voting operations
- `AdminLearning.tsx` - Dashboard UI