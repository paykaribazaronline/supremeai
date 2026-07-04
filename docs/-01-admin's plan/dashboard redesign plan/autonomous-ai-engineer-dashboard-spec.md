# Autonomous AI Engineer Dashboard — Production Architecture Specification
### Codename: "Sujon Core" | Cross-Platform Web Automation Cockpit
**Document Class:** Engineering Blueprint v1.0
**Audience:** Full-Stack Engineering Team, DevOps, Security Reviewers

---

## Guiding Architectural Principles

| Principle | Implementation Mandate |
|---|---|
| Zero Operating Cost | All heavy compute (browser sessions, DOM crawling) runs on ephemeral, pay-per-execution serverless containers (e.g., Fly.io Machines, AWS Lambda + Firecracker, or Cloudflare Durable Objects + Browser Rendering API). No persistent VM billing. |
| Infinite Automation | Every UI element is a *reflection* of database state, not a static component. Selectors, retry policies, and workflows are hot-reloadable without redeploying frontend/backend code. |
| Database-Driven UI/Logic | A single `dashboard_config` + `site_actions_registry` schema pair drives every rendered metric, button, and threshold. No magic numbers or hardcoded copy in the frontend layer. |

---

## 1. Global Workspace & Live Action Center ("The Cockpit")

### 1.1 Live Execution Shell

**Purpose:** A terminal-grade, append-only event stream giving the user forensic visibility into everything the agent does.

**UI Composition:**
- Three-pane resizable layout: `File Tree (left) | Execution Shell (center) | Agent Reasoning Log (right, collapsible)`.
- Shell renders a virtualized log list (only ~150 DOM nodes ever mounted regardless of log volume) backed by a `xterm.js`-style renderer for ANSI color codes emitted by sandboxed shell commands.
- Each log line is a row from `execution_logs` table, streamed via WebSocket/Server-Sent-Events channel `ws://.../session/{session_id}/stream`.

**Database Schema — `execution_logs`:**
```
execution_logs (
  id UUID PK,
  session_id UUID FK -> agent_sessions.id,
  ts TIMESTAMPTZ,
  log_type ENUM('shell_cmd','shell_stdout','shell_stderr','file_write','file_delete','dom_action','reasoning_token'),
  payload JSONB,          -- e.g. {"cmd": "npm install", "cwd": "/workspace/app"}
  exit_code INT NULL,
  duration_ms INT NULL
)
```
- **File-tree manipulation logs** are diffed events: every `file_write`/`file_delete` row triggers an optimistic patch to a client-side virtual file tree (a Merkle-diffed JSON structure), so the user watches files appear/change in real time without re-fetching the whole tree.
- A "Replay" scrubber at the bottom of the shell lets the user drag a timeline slider to reconstruct the exact terminal + file-tree state at any historical timestamp — reconstructed purely by replaying `execution_logs` rows client-side (no server replay compute cost, honoring the Zero-Cost principle).

### 1.2 Interactive Sandbox Viewport (Live Browser Streaming)

**Mechanism:**
- The ephemeral serverless sandbox runs a headless Chromium instance (Playwright) with `--remote-debugging-port` exposed internally.
- Frame delivery to the browser uses the Chrome DevTools Protocol's `Page.startScreencast`, piping JPEG frames over the same WebSocket channel as logs (multiplexed by a `channel: "screencast"` envelope), rendered onto an HTML5 `<canvas>` at adaptive frame rate (throttled to 4–8 fps when idle, up to 15 fps during active interaction, to minimize egress cost).

**Human-in-the-Loop Takeover Protocol:**
1. User clicks **"Take Control"** — this sends a `takeover_request` event.
2. Backend flips the session's `control_mode` column (`agent` → `pending_handoff` → `human`) in `agent_sessions`.
3. Agent process receives a `SIGPAUSE`-equivalent signal (an internal cooperative-yield checkpoint — agents only yield at safe checkpoints, never mid-DOM-mutation, to avoid corrupting page state).
4. Canvas becomes directly interactive: mouse/keyboard events captured client-side are translated into CDP `Input.dispatchMouseEvent` / `Input.dispatchKeyEvent` calls and forwarded over the WebSocket.
5. A persistent amber banner reads: **"You are now driving. Agent is paused."** with a single **"Return Control to Agent"** button.
6. On handoff-back, the backend snapshots the live DOM + cookies so the agent's next reasoning step includes the human's manual changes as fresh context (preventing the agent from "undoing" the user's fix).
7. All takeover windows are logged in `handoff_events` for audit: `{session_id, user_id, start_ts, end_ts, actions_taken_count}`.

**Security constraint:** Takeover streaming uses a short-lived (60s TTL) signed viewer token per session — never a static embed URL — to prevent session-stream hijacking via a leaked link.

### 1.3 Agent State Machine UI

Every session displays a state pill driven by `agent_sessions.current_state`, a Postgres ENUM, with a strict allowed-transition graph enforced at the database layer via a trigger (not just app logic — defense in depth):

| State | Visual Treatment | Description |
|---|---|---|
| `Idle` | Grey, static dot | No active task; awaiting instruction |
| `Scanning_Target_DOM` | Blue, slow pulse | Crawling/indexing target site structure |
| `Executing_Workflows` | Green, fast pulse | Actively performing mapped actions |
| `Circuit_Breaker_Open` | Deep red, static + lock icon | Automation halted after threshold failures |
| `Self_Healing_Retries` | Amber, erratic flicker | Attempting selector re-mapping |
| `Awaiting_Human_Input` | Violet, breathing glow | Blocked on takeover or approval gate |
| `Success` | Emerald, single flash then solid | Task completed, awaiting acknowledgment |
| `Failed` | Crimson, X icon | Terminal failure, requires manual review |

Transitions are broadcast via the same WebSocket channel and drive both the state pill **and** the ambient background (see Section 5).

---

## 2. Boundless Target Platform Vault & Security Panel

### 2.1 Cross-Domain Session Vault

**UI:** A card-grid "Connected Platforms" view. Each card represents one row in `target_platform_credentials`, showing platform favicon, connection health (`Active`/`Expired`/`Needs Re-Auth`), and last-used timestamp — all database-driven, no hardcoded platform list.

**Schema:**
```
target_platform_credentials (
  id UUID PK,
  user_id UUID FK,
  platform_label TEXT,             -- "AWS Console", "Shopify Store #2", custom portals
  auth_type ENUM('oauth2','cookie_session','api_key','basic_auth'),
  encrypted_blob BYTEA,             -- AES-256-GCM ciphertext, never plaintext at rest
  kms_key_ref TEXT,                 -- reference to KMS envelope key, not the key itself
  status ENUM('active','expired','revoked','needs_reauth'),
  last_used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ
)
```

**Import mechanisms offered:**
1. **OAuth2 Standard Flow** — redirect + callback, tokens stored encrypted.
2. **Browser Extension Cookie Sync** — a companion extension exports the session cookie jar for a domain (user-initiated, explicit per-domain consent screen shown before any transfer).
3. **Manual Cookie/Token Paste** — a masked textarea for advanced users/custom internal portals with no OAuth support.

### 2.2 Zero-Knowledge Token Masking

- Frontend **never** receives decrypted secrets. The `/vault` API returns only metadata (`platform_label`, `status`, `last_used_at`) — the `encrypted_blob` column is excluded from every serialization path by an ORM-level field guard, not just a UI hide.
- Visual masking: `••••••••••4f2a` (last 4 chars of a non-reversible display hash, purely cosmetic — not derived from the real secret) so users can distinguish which credential is which without any exposure risk.
- **Decryption only happens inside the ephemeral sandbox's isolated memory space** at task-execution time, using a short-lived KMS-issued data key that is wiped when the sandbox container terminates. No decrypted credential is ever written to disk, logs, or `execution_logs.payload` (a redaction middleware scrubs any payload matching known secret patterns before persistence).

---

## 3. Database-Driven Workflow Builder & Action Registry UI

### 3.1 Site Action Mapper

**Purpose:** Lets power users/super-admins visually inspect and edit *how* the agent interacts with any given target site — entirely through data, never a code deploy.

**Schema:**
```
site_actions_registry (
  id UUID PK,
  platform_label TEXT,
  action_name TEXT,                       -- "add_product", "deploy_to_prod"
  target_url_pattern TEXT,                -- supports wildcard/regex
  primary_selector TEXT,                  -- "button[data-action='add-product']"
  fallback_selectors TEXT[],              -- ordered list, tried in sequence
  selector_strategy ENUM('css','xpath','text_match','aria_label','visual_anchor'),
  expected_dom_signature JSONB,           -- structural fingerprint for validation
  last_verified_at TIMESTAMPTZ,
  health_score NUMERIC(3,2)               -- 0.00–1.00, decays on failures
)
```

**UI:** A searchable/filterable table (one row per registered action) with an inline "Test Selector" button that runs a live, sandboxed dry-run against the real target page and highlights the matched element with a red bounding-box overlay screenshot returned to the UI — giving admins instant visual confirmation without touching code.

### 3.2 Adaptive UI Handler Logs (Self-Healing Trail)

When a target site's layout changes and `primary_selector` fails:
1. Agent falls back through `fallback_selectors` in order.
2. If all fail, agent triggers a **visual + semantic re-mapping pass**: it screenshots the region, sends it plus the DOM subtree to a vision-capable reasoning pass, and proposes a new candidate selector.
3. A `selector_healing_events` row is written:
```
selector_healing_events (
  id UUID PK,
  action_id UUID FK -> site_actions_registry.id,
  old_selector TEXT,
  new_selector TEXT,
  confidence_score NUMERIC(3,2),
  auto_applied BOOLEAN,       -- true if confidence > configurable threshold
  screenshot_before_url TEXT,
  screenshot_after_url TEXT,
  reviewed_by_user_id UUID NULL
)
```
4. **UI Display:** A timeline card in the "Healing Log" panel shows a before/after side-by-side screenshot diff, the confidence score as a progress ring, and — if `auto_applied = false` — an **Approve / Reject** button pair so a human can gate low-confidence self-healing before it's promoted into `site_actions_registry`.

---

## 4. Operational Guardrails & Cost-Billing Matrix

### 4.1 Resource Hard-Caps Panel

**UI:** A settings form, one row per constraint, all backed by `execution_policies` — changing a slider here takes effect on the *next* task run with zero deploy.

```
execution_policies (
  id UUID PK,
  user_id UUID FK,
  scope ENUM('global','per_platform','per_action'),
  scope_ref_id UUID NULL,
  max_timeout_seconds INT DEFAULT 45,
  max_retries INT DEFAULT 3,
  max_serverless_compute_budget_usd NUMERIC(6,4) DEFAULT 0.05,
  max_concurrent_sandboxes INT DEFAULT 1,
  circuit_breaker_failure_threshold INT DEFAULT 5,
  circuit_breaker_cooldown_seconds INT DEFAULT 300
)
```

Fields exposed as labeled sliders/number inputs: **Max Timeout**, **Max Compute Budget ($)**, **Max Retries**, **Circuit Breaker Threshold**, **Cooldown Window**.

### 4.2 Execution Failover & Error Logs

When `circuit_breaker_failure_threshold` is met within a rolling window:
- Session state flips to `Circuit_Breaker_Open`.
- A structured diagnostic card renders (not a raw stack trace) with:
  - **Root cause classification** (`selector_not_found`, `timeout_exceeded`, `auth_expired`, `unexpected_dom_structure`)
  - **Failure count** and **time-to-trip**
  - **Last 3 failed attempts**, each linking back to its `execution_logs` timestamp for one-click shell scrub-to
  - **Suggested remediations** (e.g., "Re-authenticate Shopify session", "Review selector for `add_product`")
- A **"Reset Breaker"** button is disabled for `cooldown_seconds`, visually shown as a countdown ring, reinforcing the guardrail rather than letting users bypass it impulsively.

---

## 5. The "Sujon" Real-Time Ambient Visual Core

### `LiveSujonBackground.tsx` — Specification

**Purpose:** A GPU-accelerated ambient canvas occupying the dashboard's backdrop, giving peripheral, non-intrusive awareness of system state without reading text.

**Technical Approach:**
- WebGL2 canvas (fallback to Canvas2D on unsupported devices) rendering a particle-flow field, driven by a shader uniform `u_stateVector` updated on every `agent_sessions.current_state` change.
- Rendered via `requestAnimationFrame`, throttled to pause entirely (zero GPU draw calls) when the tab is backgrounded (`document.visibilityState`), honoring the Zero-Cost/battery-conscious principle.

**State → Visual Mapping Table:**

| Backend State | Color Palette (HSL base) | Particle Behavior |
|---|---|---|
| `Idle` | `220°, 15%, 20%` (muted slate) | Slow ambient drift, low density |
| `Scanning_Target_DOM` | `210°, 80%, 55%` (electric blue) | Grid-aligned scan-line sweep pattern |
| `Executing_Workflows` | `150°, 70%, 45%` (vector green) | High-speed directional data-vector streams |
| `Self_Healing_Retries` | `40°, 90%, 55%` (amber) | Erratic, stutter-step particle jitter |
| `Circuit_Breaker_Open` | `355°, 65%, 30%` (deep static crimson) | Motion nearly frozen; slow "protective glow" pulse, vignette darkens edges |
| `Awaiting_Human_Input` | `265°, 55%, 55%` (violet) | Gentle breathing radial pulse, inviting attention |
| `Success` | `160°, 75%, 50%` (emerald) | Single outward shockwave ring, then settles to `Idle` palette |
| `Failed` | `0°, 70%, 40%` (crimson) | Sharp inward implosion animation, then holds static |

**Props/Interface:**
```ts
interface LiveSujonBackgroundProps {
  currentState: AgentState;       // enum, single source of truth
  intensity?: number;             // 0-1, derived from active session count
  reducedMotion?: boolean;        // respects prefers-reduced-motion
}
```

**Accessibility:** Automatically degrades to a static gradient (no motion) when `prefers-reduced-motion` is detected, with state still communicated via the text pill from Section 1.3 — the ambient canvas is decorative-only and never the sole channel of state information.

---

## Cross-Cutting Data Flow Summary

```
[Ephemeral Sandbox] --CDP frames + logs--> [WebSocket Gateway] --> [execution_logs / agent_sessions tables]
                                                     |
                                                     v
                                     [Realtime subscription layer]
                                                     |
                        --------------------------------------------------------
                        |                    |                    |            |
                 Execution Shell    Sandbox Viewport      State Pill   LiveSujonBackground
```

All four UI surfaces subscribe to the **same** realtime channel — no surface polls independently, keeping serverless read costs minimal (single fan-out, not N queries).

---

🛑 ম্যানুয়াল অ্যাকশন আইটেম (Manual Action Items)

নিচের বিষয়গুলো কোনো অটোমেশন বা এআই এজেন্ট দিয়ে সমাধান করা যাবে না। প্রধান আর্কিটেক্টকে ব্যক্তিগতভাবে এগুলো পর্যালোচনা ও অনুমোদন করতে হবে, ডেভেলপমেন্ট শুরুর আগেই।

১. **KMS কী রোটেশন পলিসি:** `target_platform_credentials.kms_key_ref`-এর জন্য এনভেলপ এনক্রিপশন কী রোটেশনের সময়সীমা (৩০/৬০/৯০ দিন) এবং পুরনো ডেটা রি-এনক্রিপশনের দায়িত্ব ম্যানুয়ালি নির্ধারণ করতে হবে। এটি স্বয়ংক্রিয় স্ক্রিপ্টের ওপর সম্পূর্ণভাবে ছেড়ে দেওয়া উচিত নয়, কারণ কী কম্প্রোমাইজ হলে পুরো ভল্ট ঝুঁকিতে পড়বে।

২. **মাল্টি-প্ল্যাটফর্ম সেশন-শেয়ারিং সীমাবদ্ধতা:** একই ইউজারের একাধিক টার্গেট প্ল্যাটফর্ম (যেমন AWS + Shopify) একসাথে একই সময়ে অ্যাক্টিভ সেশনে থাকলে কুকি/টোকেন ক্রস-কন্টামিনেশনের ঝুঁকি রয়েছে। প্রতিটি সেশনের জন্য আলাদা ব্রাউজার কনটেক্সট/প্রোফাইল আইসোলেশন নিশ্চিত করার বিষয়টি কোড রিভিউয়ের সময় হাতে-কলমে যাচাই করতে হবে।

৩. **সার্কিট ব্রেকার থ্রেশহোল্ড ডিফল্ট মান:** ডিফল্ট `circuit_breaker_failure_threshold = 5` এবং `cooldown_seconds = 300` মান শুধুমাত্র প্রাথমিক প্রস্তাব — প্রকৃত প্রোডাকশন ট্র্যাফিক প্যাটার্ন বিশ্লেষণ করে এই সংখ্যাগুলো ম্যানুয়ালি টিউন করতে হবে, নইলে ফলস-পজিটিভ ব্রেকার ট্রিপ বা বিপরীতভাবে অতিরিক্ত রিট্রাই খরচ হতে পারে।

৪. **হিউম্যান-ইন-দ্য-লুপ টেকওভার টোকেন এক্সপায়ারি:** ৬০ সেকেন্ডের ভিউয়ার টোকেন TTL একটি প্রস্তাবিত মান — এন্টারপ্রাইজ কমপ্লায়েন্স রিকোয়ারমেন্ট (যেমন SOC 2, HIPAA) অনুযায়ী এই সময়সীমা এবং সেশন-রেকর্ডিং রিটেনশন পলিসি লিগ্যাল/কমপ্লায়েন্স টিমের সাথে বসে চূড়ান্ত করতে হবে।

৫. **সেলফ-হিলিং অটো-অ্যাপ্লাই কনফিডেন্স থ্রেশহোল্ড:** `selector_healing_events.auto_applied` কখন `true` হবে তার কনফিডেন্স-স্কোর কাটঅফ (যেমন ০.৮৫+) নির্ধারণ করা একটি ব্যবসায়িক ঝুঁকি সংক্রান্ত সিদ্ধান্ত — ভুল সেলফ-হিলিং প্রোডাকশন সাইটে ভুল অ্যাকশন ট্রিগার করতে পারে, তাই এই থ্রেশহোল্ড প্রতিটি ক্লায়েন্ট/প্ল্যাটফর্মের রিস্ক প্রোফাইল অনুযায়ী ম্যানুয়ালি সেট করতে হবে।
