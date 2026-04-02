# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records for SupremeAI.

An ADR documents a significant architectural decision: what was decided, why, what alternatives were considered, and what the consequences are.

## Index

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [0000](0000-template.md) | Template | — | — |
| [0001](0001-multi-ai-consensus.md) | Multi-AI Consensus Architecture | Accepted | 2026-04-02 |
| [0002](0002-admin-3-mode-control.md) | Admin 3-Mode Control System | Accepted | 2026-04-02 |

## How to Write a New ADR

1. Copy `0000-template.md`
2. Name it `XXXX-short-title.md` (increment the number)
3. Fill in all sections
4. Add it to the table above
5. Reference it from the relevant implementation guide

## When to Write an ADR

Write an ADR when making a decision that:

- Affects system architecture or core data flow
- Is hard to reverse later
- Involves a real trade-off between options
- Future contributors would ask "why was this done this way?"

Do NOT write an ADR for:

- Bug fixes
- Routine dependency updates
- Minor implementation details
