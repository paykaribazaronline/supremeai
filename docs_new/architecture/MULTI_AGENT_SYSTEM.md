# Multi-Agent System Architecture

## Overview

SupremeAI uses a multi-agent orchestration system to analyze requirements and generate applications automatically.

## Agent Components

### Core Orchestrator

**File:** `src/main/java/com/supremeai/agentorchestration/AdaptiveAgentOrchestrator.java`

The main orchestrator coordinates the agent workflow:

1. **Requirement Analysis** - AI-driven question generation
2. **Consensus Building** - Multi-provider voting on decisions
3. **Context Building** - Generation context assembly
4. **Code Generation** - Code synthesis from decisions

### Agent Orchestration Controller

**File:** `src/main/java/com/supremeai/agentorchestration/AgentOrchestrationController.java`

REST endpoints:

- `POST /api/orchestrate/requirement` - Orchestrate a requirement
- `POST /api/orchestrate/generate` - Orchestrate + generate code
- `POST /api/orchestrate/generate-with-context` - Generate with existing context
- `GET /api/orchestrate/health` - Health check

### Agent Types

| Agent | File | Status | Description |
|-------|------|--------|-------------|
| DiOSAgent | `agent/DiOSAgent.java` | Stub (0 bytes) | Desktop agent - placeholder |
| EWebAgent | `agent/EWebAgent.java` | Stub (0 bytes) | Web agent - placeholder |
| FDesktopAgent | `agent/FDesktopAgent.java` | Stub (0 bytes) | Desktop agent - placeholder |
| GPublishAgent | `agent/GPublishAgent.java` | Stub (0 bytes) | Publish agent - placeholder |

### Supporting Components

| Component | File | Status |
|-----------|------|--------|
| ExpertAgentRouter | `agentorchestration/ExpertAgentRouter.java` | Implemented |
| AgentOrchestrationHub | `service/AgentOrchestrationHub.java` | Implemented |
| RequirementAnalyzerAI | `agentorchestration/RequirementAnalyzerAI.java` | Implemented |
| VotingDecision | `agentorchestration/VotingDecision.java` | Implemented |
| OrchesResultContext | `agentorchestration/OrchesResultContext.java` | Implemented |

## X-Builder and Z-Architect

These are conceptual agent roles within the system:

- **X-Builder**: Responsible for code generation and build orchestration
- **Z-Architect**: Responsible for architecture decisions and system design

**Status:** These roles are implemented through the `AdaptiveAgentOrchestrator` and `CodeGenerationService`, not as separate classes.

## Provider System

The multi-agent system uses multiple AI providers for consensus voting:

**Configuration:** `src/main/resources/application.properties` or environment variables

| Provider | Environment Variable | Status |
|----------|---------------------|--------|
| OpenAI | `OPENAI_API_KEY` | Supported |
| Anthropic | `ANTHROPIC_API_KEY` | Supported |
| Google | `GOOGLE_API_KEY` | Supported |
| Ollama | `OLLAMA_BASE_URL` | Supported (local) |
| Others | See `provider/` package | Supported |

**Note:** Configure API keys before using the multi-agent consensus features.

## Current Limitations

1. **Agent Stub Files**: The 4 agent files (`DiOSAgent`, `EWebAgent`, `FDesktopAgent`, `GPublishAgent`) are empty placeholders
2. **Auto-Answer Mode**: Currently auto-answers questions for demo; needs admin UI for production
3. **Provider Coverage**: 11 providers supported but only tested with subset
4. **End-to-End Testing**: Full pipeline from requirement to generated app needs verification

## Related Documentation

- [API Endpoints](../guides/API_ENDPOINTS.md)
- [Feature Status](../../README.md#-feature-status)
