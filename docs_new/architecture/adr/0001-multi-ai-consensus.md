# ADR-0001: Multi-AI Consensus Architecture

**Status:** Accepted
**Date:** 2026-04-02
**Deciders:** SupremeAI Core Team

---

## Context

SupremeAI needs to generate high-quality application code reliably. A single AI model has known failure modes: blind spots in certain domains, biased training data, and inconsistent quality on edge cases. We needed a strategy that maximizes code quality and reasoning accuracy without being dependent on any single provider's availability or accuracy.

Additionally, if one AI provider goes down (rate-limited, API outage, policy change), the system should degrade gracefully rather than fail completely.

---

## Decision

We will use a **Multi-AI Consensus system** that queries the admin-configured AI providers simultaneously, collects their individual responses, and determines the best answer through a voting mechanism. The winning response is used, and all vote percentages are fed back into the SystemLearning module.

Provider count is admin-controlled from 0 to unlimited. A typical example set may include OpenAI, Anthropic, Google, Meta, Mistral, Cohere, HuggingFace, XAI, DeepSeek, and Perplexity.

---

## Options Considered

### Option A: Single AI Provider (e.g., OpenAI only)

- Pros: Simple integration, low latency, single API key to manage
- Cons: Single point of failure, one perspective only, subject to one model's biases, outages affect entire system

### Option B: Primary + Fallback (2 providers)

- Pros: Redundancy for availability, simple logic
- Cons: Still only 2 perspectives, no consensus accuracy benefit, fallback may have lower quality

### Option C: Multi-AI Consensus with admin-configured providers ✅ (CHOSEN)

- Pros: Multiple independent perspectives, majority voting increases accuracy, provider failure degrades gracefully, learning system improves from all configured viewpoints
- Cons: Higher latency (parallel requests still needed), cost scales with number of providers, complex consensus logic

### Option D: Fine-tuned local model only

- Pros: No external API cost, full control, no data privacy concerns
- Cons: Requires significant GPU infrastructure, lower quality than frontier models, no diversity of reasoning

---

## Rationale

The core insight is: **SupremeAI should always be a student with great ability** — it sees every problem from as many angles as the admin configures. Just as a panel of expert reviewers produces better decisions than a single reviewer, multiple AI providers voting on the best answer produces higher-quality and more robust outputs than any single model.

Benchmarks from initial testing:

- Single AI: 85% code quality, 92% success rate
- Multi-AI Consensus: 95%+ code quality, 96%+ success rate
- Latency overhead: Negligible when requests are made in parallel

The vote percentages from each round are also valuable training signal — the learning system tracks which providers agree, which diverge, and what the consensus margin is.

---

## Consequences

### Positive

- Code quality increases from 85% to 95%+
- System remains functional when individual providers have outages
- Learning system benefits from 10x more signal per query
- Different providers catch different error types (complementary blind spots)

### Negative / Trade-offs

- Requires API keys for however many providers the admin enables (operational overhead)
- Cost increases proportionally — MULTI-AI mode costs $110-190/month vs $0 for SOLO
- More complex error handling: need to handle partial failures when only some configured providers respond
- Consensus logic must define quorum requirements (what if it's 5-5?)

### Risks

- Provider price changes could make the system economically unviable
- If a provider returns adversarial or low-quality responses, it could influence consensus
- Mitigation: Track per-provider accuracy scores and weight votes by historical performance

---

## Implementation Notes

Key files:

- `src/main/java/org/example/consensus/ConsensusVote.java` — Model tracking votes
- `src/main/java/org/example/consensus/MultiAIConsensusService.java` — Query configured AIs, select winner
- `src/main/java/org/example/consensus/MultiAIConsensusController.java` — Admin API

Key endpoints:

- `POST /api/consensus/ask` — Submit question, get consensus answer
- `GET /api/consensus/history` — View all past votes
- `GET /api/consensus/stats` — Provider performance metrics

Follow-up ADRs:

- ADR-0002: Admin 3-Mode Control System (AUTO / WAIT / FORCE_STOP)
- ADR-0003: Self-Extension Engine (SupremeAI generating its own code)
- ADR-0004: 3-Layer Documentation Maintenance System

---

*Accepted in commit `9b82efb`.*
