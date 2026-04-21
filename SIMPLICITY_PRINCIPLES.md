# Development Philosophy

## Mission Statement

**SIMPLICITY FIRST:** No duplication. No unnecessary complexity. Maximum clarity and performance.

## Core Principles

1. **DRY (Don't Repeat Yourself)**
   - Never copy-paste code
   - Extract shared logic into reusable functions/services
   - Single source of truth for every piece of logic

2. **KISS (Keep It Simple, Stupid)**
   - Prefer the simplest solution that works
   - Avoid clever code; favor readable code
   - One responsibility per function/class
   - No over-engineering

3. **YAGNI (You Ain't Gonna Need It)**
   - Don't add features until they're needed
   - Don't build abstractions "just in case"
   - Implement today's requirement, not tomorrow's guess

4. **Performance Matters**
   - Choose efficient algorithms and data structures
   - Minimize overhead and unnecessary operations
   - Measure before optimizing
   - Fast is better than slow, but correct and simple comes first

## Implementation Rules

### Code Structure

- Small, focused functions (under 30 lines preferred)
- Clear, descriptive names over short abbreviations
- Flat hierarchies when possible
- Minimal nested conditionals and loops

### Dependencies

- Favor standard library over third-party
- Every dependency must earn its place
- No new dependencies without clear justification

### Documentation

- Self-documenting code through naming
- Comments only for "why", never for "what"
- Keep README and docs minimal and current

### Testing

- Write tests for business logic
- Mock external dependencies only
- Fast, reliable, deterministic tests

## When in Doubt

**Ask:**

- Can this be simpler?
- Does this duplicate something existing?
- Is there a straightforward way to do this?
- Will this be fast enough?

**Default to:**

```java
// Good: Simple, clear, direct
if (condition) {
    doThing();
}

// Bad: Clever, complex, indirect
if (condition ? true : false) { doThing(); }
```

## Enforcement

Any AI assistant working on this codebase MUST:

1. Search for existing patterns before creating new ones
2. Refactor duplication immediately when found
3. Reject complexity that doesn't serve a clear need
4. Document deviations from this philosophy with justification

---

**Last Updated:** 2026-04-21
**Branch:** main
