# ADR-0002: Admin 3-Mode Control System

**Status:** Accepted
**Date:** 2026-04-02
**Deciders:** SupremeAI Core Team

---

## Context

SupremeAI is an autonomous system that commits code, generates applications, and interacts with external APIs without manual intervention. Without a control mechanism, the admin has no way to pause, review, or stop operations during incidents. We needed a control layer that works at runtime — without stopping the server — and is respected by every service in the system.

---

## Decision

We will implement a **3-mode admin control system** with the following modes:

- `AUTO`: All operations execute immediately without admin approval
- `WAIT`: All state-modifying operations are queued and require explicit admin approval before executing
- `FORCE_STOP`: All running and queued operations are halted immediately; no new operations start

Every Service method that modifies state must check the current mode before executing. The mode is stored in-memory for instant reading and persisted to Firebase for durability across restarts.

---

## Options Considered

### Option A: On/Off Toggle (enabled/disabled)

- Pros: Simple
- Cons: Binary — no way to pause for review without fully disabling the system

### Option B: Rate Limiting Only

- Pros: Prevents overload
- Cons: Still no way for admin to pause and review queued work; no emergency stop

### Option C: 3-Mode System (AUTO / WAIT / FORCE_STOP) ✅ (CHOSEN)

- Pros: Covers all operational scenarios (normal operation, human review, emergency), clear semantics, easily extensible
- Cons: All service methods must include a mode check (adds boilerplate)

---

## Rationale

The three modes map directly to three real operational states:

1. **Normal operation:** AUTO — the system runs itself
2. **Review required:** WAIT — a human needs to approve before changes go out
3. **Something is wrong:** FORCE_STOP — halt everything immediately

These are the three states any autonomous system needs in production. Adding only two (on/off) loses the ability to inspect and approve queued work without fully shutting down.

---

## Consequences

### Positive

- Admin retains full control over an autonomous system at any time
- Audit trail captures every mode change with timestamp and actioning user
- FORCE_STOP provides an emergency kill switch without requiring a server restart
- WAIT mode enables safe deployment reviews

### Negative / Trade-offs

- Every Service method must check admin mode (enforced by code review checklist)
- WAIT mode may create queue buildup if admin is unavailable
- In-memory mode state is lost on crash; Firebase persistence is the recovery mechanism

### Risks

- If admin control service itself fails, default behavior must be defined (default: WAIT, not AUTO)
- Mitigation: Health check monitors admin service; alerts trigger if unavailable > 60 seconds

---

## Implementation Notes

Key files:

- `src/main/java/org/example/admin/AdminControlService.java` — Mode storage, queue management
- Admin mode check pattern (required in all state-modifying service methods):

```java
AdminMode mode = adminControlService.getCurrentMode();
if (mode == AdminMode.FORCE_STOP) throw new OperationBlockedException(...);
if (mode == AdminMode.WAIT) return adminControlService.queueForApproval(...);
```

Default admin credentials (auto-created once at first setup):

- Username: `supremeai`
- Password: value from `SUPREMEAI_ADMIN_PASSWORD`

Key endpoints:

- `GET /api/admin/mode` — Current mode
- `POST /api/admin/mode` — Change mode
- `GET /api/admin/queue` — Queued operations awaiting approval
- `POST /api/admin/approve/{id}` — Approve a queued operation

Follow-up ADRs:

- ADR-0003: Self-Extension Engine

---

*See [ADMIN_CONTROL_COMPLETE_GUIDE.md](../../ADMIN_CONTROL_COMPLETE_GUIDE.md) for full implementation guide.*
