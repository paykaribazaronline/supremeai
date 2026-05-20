# 🧠 Browser Intelligence Improvement Plan for Solo Mode

**Status:** Planning — May 19, 2026
**Priority:** CRITICAL — Browser is the backbone of Solo Mode intelligence
**Author:** Kilo (SupremeAI Code Agent)

---

## 1. Architecture Analysis — What We Have Now

### The Two-Layer Browser Stack

| Layer | Technology | Port | File Orchestrating |
|-------|------------|------|--------------------|
| TypeScript / Node.js | Playwright + Express | 3001 | `browser-automation-tool/src/browserController.ts` |
| Java / Spring Boot | Playwright via WebClient proxy | 8080 | `src/main/java/com/supremeai/service/browser/BrowserService.java` |

### Solo Mode Context

`KnowledgeService.soloModeAnswerAndLearn()` — (`KnowledgeService.java:296`)
- Solo Mode operates **without external AI** by performing DuckDuckGo web searches via `ActiveInternetScraper`
- Scraped answers are auto-saved as `SystemLearning` entries for future offline use
- **But**: it never uses the browser controller's automation — only HTTP text scraping
- Playwright-based browser is technically available but **not wired into Solo Mode**

### Java `BrowserService` Capacities (25 REST endpoints at `/api/browser/`)

| Capability | Status |
|------------|--------|
| URL trust scoring (`calculateTrustScore`) | Working — basic heuristic only (`.gov`, `.edu`, `github.com`) |
| Credential vault (encrypted) | Working — AES encrypted storage |
| URL permission system (allow/deny/requests) | Working — DB-backed |
| Autonomous step execution (`executeAutonomousStep`) | Partially working — blocks on `VisionService.analyzeImage()`, no timeout/max-iterations |
| Strategic learning (`recordStrategicLearning`) | Partially — calls `VisionService` with `null` image (API contract issue) |
| Auto-learn on URL visit | Working — but low confidence (0.75), no semantic enrichment |
| `/simulate-activity` | Working stub |

### TypeScript `BrowserController` Issues (100 lines)

| Problem | Impact |
|---------|--------|
| `getConsoleLogs()` always returns `[]` — not wired to CDP | No debugging output |
| `navigateTo(url)` has no `waitUntil`, no timeout | Race conditions on slow pages |
| `runPerformanceTrace()` casts to `PerformanceNavigationTiming` but returns `any` insight strings | Useless as structured data |
| `/screenshot` endpoint returns raw base64 in JSON body without any size limit or error handling | Risk of OOM on large pages |
| No session pickle/resume, no cookie persistence | Lost state on server restart |
| No retry on action failures | One failed click = entire Solo Mode task dies |
| No action queue / executor loop — caller must invoke each step manually | Cannot run goal-driven autonomous sequences |
| Same code duplicated in `legacy/` directory | Tech debt, one copy must drift |

### Diagram of the Solo Mode Flow (Current)

```
User Question
     │
     ▼
KnowledgeService.soloModeAnswerAndLearn()
     │
     ▼
ActiveInternetScraper.generalWebSearch(DuckDuckGo)
  ← HTTP-only text grab → no Playwright
     │
     ▼
Results StringBuilder → SystemLearning → Saved
     │
     ▼
[BrowserController NOT involved — missed automation opportunity]
```

---

## 2. Root-Cause Issues (Why the Browser Is "A Crucial Part" That Doesn't Deliver)

### 2.1 The Java and TS controllers are completely disconnected
The Java `BrowserService` proxies to `http://localhost:3001` via `WebClient`. But `browser-automation-tool/src/` has no `/screenshot` review loop, no page context, no autonomous executor. Java holds the intellectual property (task queue, trust scoring, credential management); TS holds the browser automation engine. **They need to be fused into one pipeline.**

### 2.2 Solo Mode doesn't use the browser at all
`KnowledgeService.soloModeAnswerAndLearn()` only uses HTTP scraping. Playwright is available but never called. In Solo Mode (no external AI), Playwright is the only engine capable of extracting rich, structured, JS-rendered page content — this gap must be closed.

### 2.3 No intelligent executor on either layer
Neither TS controller nor Java `executeAutonomousStep()` has:
- **Retry/compensation** (if a click fails, try scrolling first)
- **State persistence** (save task state so you resume from step N after restart)
- **Max loop guard** (infinite loop prevention)
- **Step validation** (did the action actually change the page?)
- **Cost-aware task budgeting** (limit time/actions per Solo query)

### 2.4 No SoloSpecificBrowserTicket mode
There is no concept of a "Solo Browser Session Ticket" — a bounded, auditable, time-limited browser run that:
- Starts fresh context (isolated from any previous Solo Mode session)
- Has a step limit, timeout, and content policy
- Auto-records all extracted learning to `SystemLearning`
- Self-terminates on goal completion or step/timeout exhaustion

### 2.5 Vision service is a mandatory dependency of `executeAutonomousStep()`
**One missing `VisionService.analyzeImage()` call = entire autonomous browsing chain stalls.** Solo Mode must degrade gracefully when vision is unavailable — e.g. fall back to `Page.getAccessibilityTree()` + DOM content heuristics.

### 2.6 API mismatch risk between Java `executeAutonomousStep()` and TS server
Java `BrowserService.navigateTo()` calls `webClient.post(automationUrl + "/navigate")` but the TS server returns `{ success: true }` on JSON and **absorbs exceptions into 500 responses.** Java discards the response body and uses `then()` to flatten — meaning Java **cannot differentiate** "page successfully loaded" from "navigation timed out / DNS error." The same is true for `/click`, `/fill`, `/type-key`.

---

## 3. What "Smart and Intelligent" Means for the Browser in Solo Mode

```
┌─────────────────────────────────────────────────────────────────┐
│                  SMART BROWSER IN SOLO MODE                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  INPUT: "How do I set up a GitHub Actions CI pipeline in 2025?" │
│                                                                  │
│  STEP 1 ── Goal Parser                                            │
│    Parse goal → identify search terms + intent                    │
│    → {"search": "GitHub Actions CI pipeline 2025", "type": "how"}│
│                                                                  │
│  STEP 2 ── Intelligent Search                                      │
│    Navigate Playwright to DuckDuckGo (trust-automatically)         │
│    Extract top 5 result links from DOM + accessibility tree        │
│    Score links by authority (github.com, docs.github.com > blog)   │
│    Pick best result URL                                           │
│                                                                  │
│  STEP 3 ── Smart Page Extract                                     │
│    Navigate to chosen URL                                         │
│    Wait for network idle + DOMContentLoaded                        │
│    Detect main content area (smart selector, not fragile CSS)     │
│    Extract + deduplicate HTML/Markdown                             │
│                                                                  │
│  STEP 4 ── Strategic Learning Synthesise                          │
│    Run content through auto-learn → SystemLearning entry saved    │
│    Extract: prerequisites, step list, code blocks, warnings        │
│                                                                  │
│  STEP 5 ── Answer & Verify                                         │
│    Build final answer from extracted structured content            │
│    Verify page content changed meaningfully after navigation       │
│    Return answer + mark as "fully sourced"                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

Every step above must be:
- **Fault-tolerant** — retries + fallbacks on failure
- **Auditable** — each decision logged to activity stream
- **Learning-aware** — each extracted resource saved to `SystemLearning`
- **Boundary-respecting** — respects URL permissions, step limits, timeout bounds

---

## 4. Phased Improvement Plan

---

### Phase 1 — Fix Foundation (Blocking Issues, no new features)

#### P1.1Unify duplicate `BrowserController.ts` (decouple `browser-automation-tool/` and `legacy/`)

Delete `legacy/browser-automation-tool/src/browserController.ts` and `legacy/browser-automation-tool/src/server.ts`. Point the `legacy/` package.json to the current copy if needed. **One canonical source of truth for the TS controller.**

#### P1.2 — Fix Java→TS API mismatch — action responses must include verification

Every call from `BrowserService` to `browser-automation-tool/` currently uses `then()` (discards body). Add a `result` field to all responses:

```typescript
// TS server returnsverified confirmation
res.json({ success: true, result: { action: "navigate", url, pageLoadTime: 1450 } });
```

Then in Java, inspect the returned `result` for:
- `pageLoadTime` > threshold → slow page flag
- `action` mismatch → log discrepancy
- No success → throw and trigger retry logic

#### P1.3 — Make `navigateTo()` resilient

```typescript
async navigateTo(url: string, options?: { waitUntil?: 'load'|'domcontentloaded'|'networkidle', timeout?: number }): Promise<PageContext> {
    if (!this.page) throw new Error('Browser not launched');
    const safeTimeout = options?.timeout ?? 30000;
    const response = await this.page.goto(url, {
        waitUntil: options?.waitUntil ?? 'domcontentloaded',
        timeout: safeTimeout
    });
    return {
        url: this.page.url(),
        status: response?.status(),
        loadTime: response?.timing()?.responseEnd ?? -1,
        title: await this.page.title()
    };
}
```

#### P1.4 — Wire `getConsoleLogs()` via CDP (no "doesn't exist" excuse)

```typescript
import { chromium, CDPSession } from 'playwright';
// ...
async getConsoleLogs(): Promise<any[]> {
    if (!this.page) throw new Error('Browser not launched');
    const client = (this.page as any)._client;  // CDP session
    if (!client) return [];
    return await client.send('Log.getLog', { levels: ['warning', 'error', 'log', 'info'] })
        .then(r => r?.entries ?? []);
}
```

#### P1.5 — Hardening `runPerformanceTrace()` to use CDP Performance domain

```typescript
async runPerformanceTrace(): Promise<any> {
    if (!this.page) throw new Error('Browser not launched');
    const client = (this.page as any)._client;
    if (!client) return { insights: ['CDP not available'] };
    await client.send('Performance.enable');
    await client.send('Performance.disable');
    const metrics = await client.send('Performance.getMetrics');
    // Map CDP performance metrics to structured insights
    // ...
}
```

#### P1.6 — Add `/extract-text` new TS endpoint for content extraction

```typescript
app.get('/extract-text', async (req, res) => {
    try {
        const { selector } = req.query;  // e.g. "main", "article", ".content"
        const text = selector
            ? await this.page.$(selector as string).then(el => el?.textContent())
            : await this.page.evaluate(() => document.body.innerText);
        res.json({ text: text?.trim() ?? '', length: text?.length ?? 0 });
    } catch (e) {
        const message = e instanceof Error ? e.message : 'Unknown error';
        res.status(500).json({ error: message });
    }
});
```

#### P1.7 — Make `screenshot()` safer — bounded size + error handling

```typescript
async takeScreenshot(options?: { maxWidth?: number; fullPage?: boolean }): Promise<string> {
    if (!this.page) throw new Error('Browser not launched');
    const maxWidth = Math.min(options?.maxWidth ?? 1920, 3840);
    const buffer = await this.page.screenshot({
        type: 'png',
        fullPage: options?.fullPage ?? false,
        clip: maxWidth < 1920 ? { x: 0, y: 0, width: maxWidth, height: 9999 } : undefined
    });
    return buffer.toString('base64');
}
```

---

### Phase 2 — Build Solo Browser Session Ticket System (New Architecture)

Create a new class: `SoloBrowserTicket.java` (Java) + `SoloSessionManager.ts` (TS).

A `SoloBrowserTicket` encapsulates a bounded Solo Mode browser run:

```java
public record SoloBrowserTicket(
    String ticketId,
    String userQuestion,
    String status,               // ACTIVE | PROCESSING | COMPLETED | TIMEOUT | FAILED
    int stepsExecuted,
    int maxSteps,
    Instant startedAt,
    Instant completedAt,
    List<BrowserActivity> activityLog,
    List<BrowserFinding> findings,
    String finalAnswer,
    @Nullable String failureReason
) {}
```

#### P2.1 — TS: `SoloSessionManager` wraps TSA controller

```typescript
export class SoloSessionManager {
    private sessions: Map<string, SoloSession> = new Map();
    private maxAgeMs = 5 * 60 * 1000;  // 5 minutes auto-expiry

    startSession(ticketId: string, options?: SoloSessionOptions): SoloSession {
        const session = new SoloSession(ticketId, this.tsController, options);
        this.sessions.set(ticketId, session);
        return session;
    }

    getSession(ticketId: string): SoloSession | undefined {
        return this.sessions.get(ticketId);
    }

    closeSession(ticketId: string): void {
        this.sessions.delete(ticketId);
    }

    pruneExpired(): void {
        const now = Date.now();
        for (const [id, s] of this.sessions) {
            if (now - s.createdAt.getTime() > this.maxAgeMs) {
                s.close().catch(() => {});
                this.sessions.delete(id);
            }
        }
    }
}
```

#### P2.2 — Add new TS routes for ticket lifecycle

```
POST /solo/start      → { ticketId, maxSteps, timeoutMs }
POST /solo/step/{id}  → { action: "navigate"|"click"|"extract"|"scroll", payload }
GET  /solo/status/{id} → { stepsDone, currentUrl, findings[], activityLog[] }
GET  /solo/result/{id} → { finalAnswer, entities[], sources[] }
POST /solo/stop/{id}  → cleanly terminate and save to DB
GET  /solo/log/{id}   → human-readable session log
```

#### P2.3 — Java: `SoloBrowserService.newBrowserTicket(...)`

```java
@Service
public class SoloBrowserService {
    private final WebClient webClient;
    private final SoloBrowserTicketRepository ticketRepo;
    private final SystemLearningRepository learningRepo;

    public Mono<SoloBrowserTicket> newBrowserTicket(
            String ticketId, String question, int maxSteps, int timeoutMs) {

        SoloBrowserTicket ticket = new SoloBrowserTicket(
            ticketId, question, "ACTIVE",
            0, maxSteps,
            Instant.now(), null,
            List.of(), List.of(), null, null);

        return ticketRepo.save(ticket)
            .then(webClient.post()
                .uri(automationUrl + "/solo/start")
                .bodyValue(Map.of("ticketId", ticketId, "maxSteps", maxSteps, "timeoutMs", timeoutMs))
                .retrieve().bodyToMono(Object.class))
            .then(ticketRepo.findById(ticketId));  // return hydrated ticket
    }
}
```

#### P2.4 — Add `@Scheduled` session pruning for TS

Add to `server.ts`:
```typescript
setInterval(() => sessionManager.pruneExpired(), 60_000);
```

---

### Phase 3 — Intelligent Solo Mode Flow (Wire into `KnowledgeService`)

Replace `soloModeAnswerAndLearn()` with a 5-step Solo Flow:

#### Step A — Intelligent Search (Replace DuckDuckGo stub)

Instead of only HTTP text-grabbing from DuckDuckGo:
1. Launch `SoloBrowserTicket`
2. Use **Playwright to visually navigate DuckDuckGo**
3. Use `runPerformanceTrace()` to wait for DOM ready
4. Use `/extract-text` to GET result links from the SERP with a smart content selector
5. Score each link using trust tier → GitHub docs > MDN > StackOverflow > blogs
6. Pick the highest-ranking link and navigate to it

#### Step B — Rich Content Extraction

```typescript
async extractPageContent(url: string): Promise<PageExtract> {
    const ctx = await navigateTo(url, { waitUntil: 'networkidle' });
    const screenshot = await takeScreenshot({ fullPage: false });

    // Parallel richness
    const [text, accessibility, perf, network] = await Promise.all([
        page.evaluate(() => document.body.innerText),   // text
        getAccessibilityTree(),                          // accessibility tree
        runPerformanceTrace(),                          // perf
        getNetworkRequests()                             // network
    ]);

    return { context: ctx, text, accessibility, perf, network, screenshot };
}
```

#### Step C — Smart Learning Synthesise

After extracting a page:
1. Classify the page type (docs/github/blog/wiki)
2. Parse content structure (headings, code blocks, step lists)
3. Generate structured `SystemLearning` entry with:
   - `topic`: extracted from `<title>` + `h1`
   - `content`: structured blocks (code, warnings, steps)
   - `tags`: domain tags from page URL + extracted keywords
   - `confidenceScore`: 0.7 (docs) / 0.6 (blog) / 0.5 (generic web)

#### Step D — Answer Compilation

```java
private Mono<String> compileAnswer(SoloBrowserTicket ticket) {
    return Flux.fromIterable(ticket.getFindings())
        .collectMultimap(BrowserFinding::getType)
        .map(typeMap -> {
            StringBuilder sb = new StringBuilder();
            typeMap.forEach((type, findings) -> {
                sb.append("\n## ").append(type).append("\n\n");
                findings.forEach(f ->
                    sb.append("- ").append(f.getTitle()).append(": ").append(f.getContent()).append("\n"));
            });
            return sb.toString();
        });
}
```

#### Step E — Integration into Solo Mode Answer

```java
// In soloModeAnswerAndLearn, replace HTTP-only path:
return new SoloBrowserTicket(ticketId, question, maxSteps)
    .flatMap(this::executeIntelligentFlow)  // Step A → B → C → D
    .flatMap(ticket ->
        ticketRepository.save(ticket)
            .then(learningRepository.save(ticket.toSystemLearning(question)))
    )
    .map(ticket -> Map.of(
        "answer", ticket.getFinalAnswer(),
        "source", "Solo Mode Playwright Browser",
        "savedAsLearning", true,
        "learningId", ticket.getSystemLearningId()
    ));
```

---

### Phase 4 — Java Service Enhancements / Bug Fixes

#### P4.1 — Strategic Learning: fix API contract violation

`BrowserService.recordStrategicLearning()` at line 654 calls `visionService.analyzeImage(null, ...)`. Fix this by providing a non-null prompt only mode:

```java
return visionService.analyzeScreenshotsAsync(
    List.of(recordedScreenshot),
    "Given the browsing history for goal '" + task.getGoal() + "', build a strategic blueprint...",
    null  // no image, text-only mode
);
```

If `VisionService` doesn't support text-only mode, add a `VisionService.analyzeText(String prompt)` method.

#### P4.2 — `getCredentialContext()` leaks decrypted passwords as log strings

```diff
- context.append("- Password: ").append(decryptedPass).append("\n");
+ context.append("- Password: [REDACTED]").append("\n");
```

Also: wrap the prompt builder in a `try/catch` that swallows decryption failures and logs `[ENCRYPTION_ERROR]`.

#### P4.3 — `executeAutonomousStep()` lacks max loop/min-retry safety

Add at top of `executeAutonomousStep`:
```java
return ruleService.getRuleByKey("BROWSER_MAX_STEPS")
    .flatMap(maxStepsRule -> {
        int maxSteps = Integer.parseInt(maxStepsRule.getValue());
        return ruleService.getRuleByKey("BROWSER_STEP_TIMEOUT_MS")
            .flatMap(timeoutRule -> {
                int timeoutMs = Integer.parseInt(timeoutRule.getValue());
                return taskRepository.findById(taskId)
                    .flatMap(task -> runStepWithTimeout(task, maxSteps, timeoutMs, stepNumber++));  // stepNumber in task
            });
    });
```

Add a `stepNumber` field to `BrowserTask`:
```java
private int stepNumber = 0; // increment before each call to executeAutonomousStep
```

#### P4.4 — `calculateTrustScore()` → Externalise to `SystemWorkRule`

```java
private double calculateTrustScore(String url) {
    return ruleService.evaluateTrustScore(url); // moves scoring logic to configurable rules
}
```

This makes trust scoring extensible without code changes.

#### P4.5 — Auto-learn confidence too low

```java
learning.setConfidenceScore(0.82);  // current
// → raise to a tiered model
Map<String, Double> confidenceByDomain = Map.of(
    ".gov", 0.95, ".edu", 0.93, "github.com", 0.88, "wikipedia.org", 0.89,
    "google.com", 0.85, "medium.com", 0.72, "dev.to", 0.70
);
double score = confidenceByDomain.entrySet().stream()
    .filter(e -> url.contains(e.getKey()))
    .mapToDouble(Map.Entry::getValue)
    .findFirst().orElse(0.65);
learning.setConfidenceScore(score);
```

---

### Phase 5 — CSS Selector Smartness

#### P5.1 — Add `SoloSmartSelector` to TS controller

```typescript
export class SoloSmartSelector {
    private readonly preferredSelectors: Record<string, string[]> = {
        searchBox:     ['input[name="q"]', 'input[type="search"]', 'input[role="searchbox"]', '#search', 'input[placeholder*="search"]'],
        submitButton: ['button[type="submit"]', 'input[type="submit"]', '[aria-label*="search"]', 'button[aria-label*="search"]'],
        searchResultLink: ['h3 a', 'h1 a', '.result a', 'a[href^="http"]'],
        mainContent:  ['main', 'article', '#main', '#content', '.content', '[role="main"]'],
        codeBlock:    ['pre code', 'pre', '.code', 'code'],
        headingList:  ['h1, h2, h3, h4, h5, h6']
    };

    async findBest(queryType: keyof SoloSmartSelector['preferredSelectors']): Promise<string> {
        const selectors = this.preferredSelectors[queryType];
        for (const sel of selectors) {
            if (await this.page.$(sel)) return sel;
        }
        return selectors[selectors.length - 1];  // fallback to last
    }
}
```

#### P5.2 — `SoloSmartSelector` can rank elements by visual affordance

```typescript
async findVisibleLinkOnPage(): Promise<string> {
    // Rank links by text length, visual center position
    const links = await this.page.evaluate(() => {
        return Array.from(document.querySelectorAll('main a[href]'))
            .map((a: any) => ({
                selector: `//a[normalize-space()="${a.textContent.trim().substring(0, 100)}"]`,
                text: a.textContent?.trim(),
                href: a.href,
                visible: a.offsetParent !== null
            }))
            .filter((l: any) => l.visible && l.href.startsWith('http'));
    });
    // Return best selector (prefer github.com / docs domains)
    // ...
}
```

---

### Phase 6 — Solo Mode Health + Monitoring Dashboard

#### P6.1 — Add solo mode health endpoints to Java

```java
@GetMapping("/solo/health")
public Mono<Map<String, Object>> soloHealth() {
    return soloBrowserService.getHealthSnapshot();
}

@GetMapping("/solo/active-tickets")
public Mono<Map<String, Object>> activeTickets() {
    return soloBrowserService.getActiveTickets().collectList()
        .map(tickets -> Map.of("activeTickets", tickets.size(), "tickets", tickets));
}
```

#### P6.2 — Admin dashboard Solo section (new in admin/dashboard.js)

```javascript
async loadSoloMetrics() {
    const [tickets, health, busiest] = await Promise.all([
        this.fetch('/browser/solo/active-tickets'),
        this.fetch('/browser/solo/health'),
        this.fetch('/browser/solo/recent-completions')
    ]);
    this.renderSoloTicketCount(tickets);
    this.renderSoloHealth(health);
    this.renderRecentSoloCompletions(busiest);
}
```

#### P6.3 — Solo Mode health check in `KnowledgeService`

Expose health endpoint `/api/knowledge/solo-health` that returns:
```json
{
  "isSoloModeActive": true/false,
  "activeBrowserTickets": <count>,
  "averageStepsPerTicket": <float>,
  "lastSoloCompletion": "<ISO datetime>",
  "totalSoloLearnings": <count>,
  "browserAutomationAvailable": true/false,
  "playwrightConnected": true/false,
  "soloExceptionsStats": { "timeouts": <int>, "visionFailures": <int>, "navigationFailures": <int> }
}
```

#### P6.4 — Add `SoloBrowserMetrics` database table

Store per-ticket metrics for auditing and performance analysis:
```sql
CREATE TABLE solo_browser_metrics (
    ticket_id VARCHAR(36) PRIMARY KEY,
    question TEXT,
    steps_executed INT,
    total_time_ms BIGINT,
    status VARCHAR(20),
    sources_extracted INT,
    confidence_score DOUBLE,
    completed_at TIMESTAMP,
    failure_reason VARCHAR(500)
);
```

---

### Phase 7 — Solo Mode Action Queue (for true autonomous execution)

#### P7.1 — `SoloActionQueue` — TS-side action execution loop

```typescript
export class SoloActionQueue {
    private queue: SoloAction[] = [];
    private busy = false;

    enqueue(actions: SoloAction[]): void {
        this.queue.push(...actions.filter(a => !a.isTerminal()));
    }

    async executeNext(): Promise<SoloActionResult | null> {
        if (this.busy || this.queue.length === 0) return null;
        this.busy = true;
        const action = this.queue.shift()!;
        try {
            const result = await this.executeSingle(action);
            if (action.onComplete) action.onComplete(result);
            return result;
        } finally {
            this.busy = false;
        }
    }

    private async executeSingle(action: SoloAction): Promise<SoloActionResult> {
        switch (action.type) {
            case 'navigate':
                const ctx = await controller.navigateTo(action.url!, { waitUntil: action.waitUntil || 'domcontentloaded' });
                return { type: 'navigate', url: ctx.url, loadTime: ctx.loadTime };
            case 'click':
                await controller.clickElement(action.selector!);
                return { type: 'click', selector: action.selector };
            case 'extract':
                const text = await controller.getExtractedText(action.selector);
                return { type: 'extract', text, selector: action.selector };
            case 'scroll':
                await controller.page.evaluate((d) => window.scrollBy(0, d === 'down' ? 500 : -500), action.direction);
                return { type: 'scroll', direction: action.direction };
            case 'finish':
                return { type: 'finish', summary: action.summary };
        }
    }
}
```

#### P7.2 — AI Planning: Convert goal into action queue

Use Vision Service (or in Solo Mode fallback, a simple keyword matcher) to convert an English goal into a `SoloAction[]` sequence:

```java
// Solo Mode: fallback AI-less planning
private SoloAction[] planActionsForGoal(String goal) {
    List<SoloAction> actions = new ArrayList<>();
    actions.add(new SoloAction(SoloActionType.NAVIGATE,
        "https://duckduckgo.com/?q=" + URLEncoder.encode(goal, StandardCharsets.UTF_8)));

    // Wait for SERP
    actions.add(new SoloAction(SoloActionType.WAIT_MS, 2000));

    // Extract the top result
    actions.add(new SoloAction(SoloActionType.EXTRACT, ".results"),
        // Click the first link
        new SoloAction(SoloActionType.CLICK, ".result a"),
        // Wait for page load
        new SoloAction(SoloActionType.WAIT_MS, 3000));

    return actions.toArray(SoloAction[]::new);
}
```

---

### Phase 8 — TypeScript Browser Controller Upgrade

Add to `browserController.ts` — new “Smart Mode” methods:

```typescript
// Get clean text from any page
async getPageText(options?: { selector?: string; wordLimit?: number }): Promise<string> {
    if (!this.page) throw new Error('Browser not launched');
    const text = options?.selector
        ? await this.page.textContent(options.selector) ?? ''
        : await this.page.evaluate(() => document.body.innerText) ?? '';
    const words = text.trim().split(/\s+/);
    return options?.wordLimit ? words.slice(0, options.wordLimit).join(' ') : text.trim();
}

// Get structured data from all links on page
async getPageLinks(): Promise<Array<{ text: string; href: string; domain: string }>> {
    if (!this.page) throw new Error('Browser not launched');
    return this.page.evaluate(() => {
        return Array.from(document.querySelectorAll('main a[href]'),
            (a: HTMLAnchorElement) => ({
                text: a.textContent?.trim() || '',
                href: a.href,
                domain: new URL(a.href).hostname
            }));
    });
}

// Auto-wait for an element (smart wait with timeout)
async waitForElement(selector: string, timeoutMs?: number): Promise<boolean> {
    if (!this.page) throw new Error('Browser not launched');
    try {
        await this.page.waitForSelector(selector, { timeout: timeoutMs ?? 10000, state: 'visible' });
        return true;
    } catch { return false; }
}

// Get page structured metadata
async getPageMetadata(): Promise<PageMetadata> {
    if (!this.page) throw new Error('Browser not launched');
    return this.page.evaluate(() => {
        const m = document.querySelector('meta[name="description"]') as HTMLMetaElement | null;
        return {
            title: document.title,
            description: m?.content || '',
            canonical: document.querySelector('link[rel="canonical"]')?.getAttribute('href') || '',
            ogType: document.querySelector('meta[property="og:type"]')?.getAttribute('content') || ''
        };
    });
}
```

---

### Phase 9 — Cross-Layer Request Management

#### P9.1 — Central `BrowserRequestManager` in Java

```java
@Service
public class BrowserRequestManager {
    private final WebClient webClient;
    private final String automationUrl;

    // Retry with exponential backoff on top of TS task-level retry
    public <T> Mono<T> callWithRetry(Function<WebClient.RequestBodySpec, Mono<T>> operation, int maxRetries) {
        return Mono.defer(() -> operation.apply(webClient))
            .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(500))
                .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable
                                  || throwable instanceof IOException));
    }
}
```

#### P9.2 — Add circuit-breaker between Java BrowserService and TS server

```java
@Bean
public Resilience4JCircuitBreakerFactory browserCircuitBreakerFactory() {
    return Resilience4JCircuitBreakerFactory.create("browserAutomation");
}

// In BrowserService methods:
public Mono<String> getScreenshot() {
    return circuitBreakerFactory.create("screenshot")
        .run(() -> webClient.get().uri(automationUrl + "/screenshot")...);
}
```

---

### Phase 10 — Novelty / Polish

#### P10.1 — VisibilityToken: Solo Mode trace and replay

Persist all Solo Mode actions to a `SoloSessionPlayback` table. Admin can click "Replay" in the dashboard and see frame-by-frame replay of a Solo Mode query.

#### P10.2 — Solo Mode page screenshot progress bar

In `solo/status/{id}` endpoint, return `{ stepsDone, totalSteps, currentURL, progressBar }`. The admin dashboard renders a live progress bar.

#### P10.3 — Automatic browser health preflight

Before executing any Solo Mode flow, run:
```
GET /test  → 200 OK required
GET /solo/health-check  → must confirm browser is not crashed
```
If either fails, auto-restart the TS browser before proceeding.

#### P10.4 — Migrate `browser-automation-tool/` to Playwright Test fixtures

The Playwright `browser-automation-tool/` is essentially a mini-test-server. Migrate it to use `@playwright/test` fixtures for proper browser lifecycle (automatic retry, teardown, isolated contexts per request) — this replaces the singleton `BrowserController` pattern.

---

## 5. 🔍 Search Intelligence — Where, How, What (NEW)

### Problem Today

`KnowledgeService.soloModeAnswerAndLearn()` → `ActiveInternetScraper.generalWebSearch()` sends **one hardcoded DuckDuckGo JSON API call** with the raw user question as-is. There is no reasoning about which source is best, no query transformation, no result quality scoring, and no admin override mechanism.

```
User: "How do I fix a NullPointerException in Java Spring Boot?"
   │
   ▼
DuckDuckGo JSON API  →  "how do i fix a nullpointerexception in java spring boot"
   │
   ▼
Top 5 snippets returned (no scoring, no source ranking, no deep extraction)
   │
   ▼
Saved directly as SystemLearning  →  Done
```

This is the **single biggest intelligence gap** in Solo Mode.

---

### The Search Intelligence Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                  SEARCH INTELLIGENCE LAYER                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  User Question: "How do I fix a NullPointerException in Spring Boot?"    │
│                                                                  │
│  STEP 1 ── Query Classification (Java)                           │
│    detects intent + domain:                                      │
│    intent=ERROR_FIX, domain=java, framework=spring,              │
│    category=programming, difficulty=intermediate                  │
│                                                                  │
│  STEP 2 ── Search Engine Planner (Java)                          │
│    Given: intent=ERROR_FIX, domain=java → picks engines:         │
│    [1] StackOverflow (query: "java spring boot nullpointerexception fix") │
│    [2] GitHub Issues (query: "NullPointerException spring-boot")  │
│    [3] DuckDuckGo  (fallback general)                            │
│    Note: Admin can override → force DuckDuckGo only, or add custom│
│                                                                  │
│  STEP 3 ── Query Transform per Engine                            │
│    StackOverflow:  "java spring boot nullpointerexception"        │
│    GitHub:         "NullPointerException repo:spring-projects/spring-boot"  │
│    DuckDuckGo:     "how to fix NullPointerException Spring Boot 2024"         │
│    Wikipedia:      "NullPointerException Java runtime exception"   │
│    MDN:            (skipped — not relevant)                      │
│                                                                  │
│  STEP 4 ── Execute in Parallel (reactive Flux)                   │
│    Playwright navigates to each chosen search URL               │
│    Extracts top N results per engine using smart selectors        │
│    Runs content confidence scoring                               │
│                                                                  │
│  STEP 5 ── Result Ranker & Merger                                │
│    Results tagged with: source authority, content quality,       │
│    recency, code snippet coverage                                 │
│    Top results deduplicated and ranked                           │
│    Best answer compiled from top 3-5 sources                     │
│                                                                  │
│  STEP 6 ── Auto-learn each high-quality source                   │
│    Each top result saved as SystemLearning entry                 │
│    Confidence score assigned by source tier                       │
│                                                                  │
│  OUTPUT: Answer + 3-5 learned SystemLearning entries             │
└─────────────────────────────────────────────────────────────────┘
```

---

### 5.1 Core Data Model — `SearchEngineConfig`

Admin-controlled configuration stored in Firestore `search_engine_configs` collection:

```java
@Document(collectionName = "search_engine_configs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SearchEngineConfig {
    @DocumentId
    private String id;
    private String engineName;          // "stackoverflow", "github", "duckduckgo", "wikipedia", "mdn", "custom"
    private String displayName;         // "StackOverflow", "GitHub Issues"
    private String baseUrl;             // "https://stackoverflow.com/search?q={query}"
    private Boolean enabled;            // true/false
    private Integer priority;           // 1=highest, 5=lowest — order engine is tried
    private String queryTemplate;       // "{query} {language} {framework}"
    private String selectorStrategy;    // "css" or "smart" — how to extract results
    private String resultSelector;      // CSS selector for search results
    private String titleSelector;       // CSS selector for result title/link
    private String snippetSelector;     // CSS selector for description snippet
    private String nextPageSelector;    // Optional pagination
    private Map<String, String> headers; // Optional HTTP headers
    private Integer maxResults;         // Max results to extract per query
    private Map<String, Object> contextHints; // When to use this engine (see 5.2)
    private Boolean adminOverrideOnly;  // Only use if admin explicitly adds it
    private Double sourceAuthorityWeight; // 0.0-1.0 confidence multiplier
}
```

---

### 5.2 `SearchContextHints` — WHAT to Route Each Query To

Each engine gets a `contextHints` map that identifies which query types it handles:

```java
// StackOverflow config hint
contextHints = Map.of(
    "intents", List.of("ERROR_FIX", "DEBUG", "TROUBLESHOOT"),
    "domains", List.of("programming", "devops", "cloud"),
    "codeRequired", true,
    "minConfidence", 0.7,
    "answerFormat", "code_snippet",
    "recencyPreference", "any"
);

// GitHub Issues config hint
contextHints = Map.of(
    "intents", List.of("ERROR_FIX", "BUG", "FRAMEWORK_SPECIFIC"),
    "domains", List.of("java", "javascript", "python", "go"),
    "codeRequired", true,
    "minConfidence", 0.75,
    "answerFormat", "discussion_thread",
    "searchSyntax", "github:label:bug"
);

// DuckDuckGo config hint
contextHints = Map.of(
    "intents", List.of("GENERAL_QUESTION", "HOW_TO", "COMPARISON"),
    "domains", List.of("all"),
    "minConfidence", 0.5,
    "answerFormat", "instant_answer"
);
```

#### Intent Taxonomy for Query Classification

| Intent | Triggers | Preferred Engines |
|--------|----------|-------------------|
| `ERROR_FIX` | "error", "exception", "fix", "broken", "not working" | StackOverflow → GitHub Issues → DuckDuckGo |
| `HOW_TO` | "how to", "how do i", "tutorial", "guide", "steps" | DuckDuckGo → Medium → YouTube API |
| `CODE_REVIEW` | "review", "refactor", "improve", "optimize" | GitHub Code Search → DuckDuckGo |
| `DEBUG` | "debug", "inspect", "trace", "why" | StackOverflow → GitHub Issues |
| `COMPARISON` | "vs", "versus", "difference", "compare" | DuckDuckGo → StackOverflow |
| `DOCS` | "documentation", "api", "reference" | MDN → official docs → Wikipedia |
| `DEPLOY` | "deploy", "docker", "kubernetes", "ci/cd" | GitHub Actions → official docs |
| `SECURITY` | "vulnerability", "cve", "exploit", "injection" | NIST NVD → OWASP → security blogs |
| `LEARNING` | "learn", "tutorial", "course", "beginner" | MDN → freeCodeCamp → YouTube |
| `GENERAL_QUESTION` | Default fallback | DuckDuckGo → Wikipedia |

---

### 5.3 `SearchQueryTransformer` — HOW to Transform the Query

```java
@Service
public class SearchQueryTransformer {

    /**
     * Transform raw user question into engine-optimized query strings.
     * Each engine gets a context-aware transformation.
     */
    public Map<String, String> transform(String rawQuestion, QueryClassification classification, SearchEngineConfig engine) {
        String template = engine.getQueryTemplate();
        Map<String, String> vars = new HashMap<>();

        // Base query — clean and trim
        String baseQuery = cleanQuery(rawQuestion);

        // Extract key terms from classification
        if (classification.getDomain() != null)  vars.put("domain", classification.getDomain());
        if (classification.getLanguage() != null) vars.put("language", classification.getLanguage());
        if (classification.getFramework() != null) vars.put("framework", classification.getFramework());
        if (classification.getErrorType() != null) vars.put("error", classification.getErrorType());
        vars.put("query", baseQuery);

        // Add engine-specific syntax if configured
        if (engine.getContextHints().containsKey("searchSyntax")) {
            String syntax = (String) engine.getContextHints().get("searchSyntax");
            baseQuery = applyEng___(syntax, baseQuery, classification);
        }

        // Format using template
        String formatted = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return Map.of("query", formatted, "rawQuery", baseQuery, "classification", toJson(classification));
    }

    private String applyGitHubSyntax(String syntax, String query, QueryClassification c) {
        // e.g. "NullPointerException repo:spring-projects/spring-boot is:issue"
        StringBuilder sb = new StringBuilder(query);
        if (c.getDomain() != null) sb.append(" repo:").append(c.getDomain());
        if (syntax.contains("is:issue")) sb.append(" is:issue");
        if (syntax.contains("is:closed")) sb.append(" is:closed");
        return sb.toString();
    }

    private String cleanQuery(String raw) {
        return raw
            .replaceAll("[\\p{Punct}&&[^:?!.-]]", " ") // Keep useful punctuation
            .replaceAll("\\s+", " ")
            .trim();
    }
}
```

---

### 5.4 `QueryClassifier` — WHAT the User is Asking

```java
@Service
public class QueryClassifier {

    /**
     * Classify user question into intent + domain + technology tags.
     * Rules-based first, LLM-enhanced when available.
     */
    public QueryClassification classify(String question, List<String> knownDomains) {
        String lower = question.toLowerCase();

        // ── Intent detection (rules-based, zero-AI safe) ──
        QueryIntent intent = classifyIntent(lower);
        
        // ── Technology / domain detection ──
        TechContext tech = classifyTechnology(lower, knownDomains);
        
        // ── Error type extraction ──
        ErrorType errorType = extractErrorType(lower);
        
        // ── Confidence in our classification ──
        double confidence = calculateConfidence(intent, tech, errorType);

        return new QueryClassification(intent, tech, errorType, confidence);
    }

    private QueryIntent classifyIntent(String lower) {
        if (lower.contains("how to") || lower.contains("how do i") || lower.contains("tutorial"))
            return QueryIntent.HOW_TO;
        if (lower.contains("error") || lower.contains("exception") || lower.contains("fix") || lower.contains("broken"))
            return QueryIntent.ERROR_FIX;
        if (lower.contains("review") || lower.contains("refactor") || lower.contains("improve"))
            return QueryIntent.CODE_REVIEW;
        if (lower.contains("debug") || lower.contains("why is") || lower.contains("inspect"))
            return QueryIntent.DEBUG;
        if (lower.contains("vs") || lower.contains("versus") || lower.contains("compare"))
            return QueryIntent.COMPARISON;
        if (lower.contains("deploy") || lower.contains("docker") || lower.contains("kubernetes"))
            return QueryIntent.DEPLOY;
        if (lower.contains("vulnerability") || lower.contains("cve") || lower.contains("injection"))
            return QueryIntent.SECURITY;
        if (lower.contains("learn") || lower.contains("tutorial") || lower.contains("beginner"))
            return QueryIntent.LEARNING;
        return QueryIntent.GENERAL_QUESTION;
    }

    private TechContext classifyTechnology(String lower, List<String> knownDomains) {
        // Check known domains first, then technology keyword map
        Map<String, String> techMap = Map.ofEntries(
            Map.entry("java", "java"), Map.entry("spring", "java"), Map.entry("maven", "java"),
            Map.entry("python", "python"), Map.entry("django", "python"), Map.entry("flask", "python"),
            Map.entry("javascript", "javascript"), Map.entry("typescript", "javascript"),
            Map.entry("react", "javascript"), Map.entry("vue", "javascript"), Map.entry("angular", "javascript"),
            Map.entry("node", "javascript"), Map.entry("express", "javascript"),
            Map.entry("go", "go"), Map.entry("golang", "go"), Map.entry("rust", "rust"),
            Map.entry("docker", "devops"), Map.entry("kubernetes", "devops"), Map.entry("k8s", "devops"),
            Map.entry("aws", "cloud"), Map.entry("gcp", "cloud"), Map.entry("firebase", "cloud")
        );
        for (Map.Entry<String, String> entry : techMap.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return new TechContext(entry.getValue(), entry.getKey());
            }
        }
        return TechContext.UNKNOWN;
    }

    private ErrorType extractErrorType(String lower) {
        String[] errorSignatures = {
            "nullpointerexception", "NullPointerException",
            "classnotfound", "ClassNotFoundException",
            "outofmemory", "OutOfMemoryError",
            "stackoverflow", "StackOverflowError",
            "illegalargument", "IllegalArgumentException",
            "sql injection", "SQLInjection",
            "nullreference", "NullReferenceException",
            "typeerror", "TypeError",
            "referenceerror", "ReferenceError",
            "modulenotfound", "ModuleNotFoundError",
            "syntaxerror", "SyntaxError"
        };
        for (String sig : errorSignatures) {
            if (lower.contains(sig.toLowerCase())) return ErrorType.parse(sig);
        }
        // Generic error patterns
        if (lower.matches(".*\\b(500|404|403|401)\\b.*")) return ErrorType.HTTP_ERROR;
        return ErrorType.NONE;
    }
}
```

---

### 5.5 `SearchEngineRegistry` — Admin-Configurable Engine List

```java
@Service
public class SearchEngineRegistry {

    /**
     * Load search engine configs from Firestore.
     * Falls back to built-in defaults if Firestore is unavailable.
     * Admin changes are live — no restart needed.
     */
    private final ReactiveSearchEngineConfigRepository configRepo;
    private final SystemWorkRuleService ruleService;

    // Built-in defaults (when Firestore empty or unavailable)
    private static final List<SearchEngineConfig> DEFAULTS = List.of(
        SearchEngineConfig.builder()
            .id("builtin-stackoverflow")
            .engineName("stackoverflow")
            .displayName("StackOverflow")
            .baseUrl("https://stackoverflow.com/search?q={query}")
            .enabled(true)
            .priority(1)
            .queryTemplate("{query} {language} {framework}")
            .resultSelector(".s-post-summary")
            .titleSelector(".s-post-summary--content-title a")
            .snippetSelector(".s-post-summary--content-excerpt")
            .maxResults(3)
            .sourceAuthorityWeight(0.85)
            .contextHints(Map.of(
                "intents", List.of("ERROR_FIX","DEBUG","TROUBLESHOOT","CODE_REVIEW"),
                "domains", List.of("programming")
            ))
            .build(),
        SearchEngineConfig.builder()
            .id("builtin-github")
            .engineName("github")
            .displayName("GitHub Issues")
            .baseUrl("https://github.com/search?q={query}&type=issues")
            .enabled(true)
            .priority(2)
            .queryTemplate("{query} repo:{domain}")
            .resultSelector(".issue-list-item")
            .titleSelector("a[data-hovercard-type='issue']")
            .snippetSelector(".markdown-title")
            .maxResults(3)
            .sourceAuthorityWeight(0.88)
            .contextHints(Map.of(
                "intents", List.of("ERROR_FIX","BUG","FRAMEWORK_SPECIFIC"),
                "domains", List.of("java","javascript","python","go","rust")
            ))
            .build(),
        SearchEngineConfig.builder()
            .id("builtin-duckduckgo")
            .engineName("duckduckgo")
            .displayName("DuckDuckGo")
            .baseUrl("https://duckduckgo.com/?q={query}&format=json")
            .enabled(true)
            .priority(5)
            .queryTemplate("{query} 2024")
            .maxResults(5)
            .sourceAuthorityWeight(0.60)
            .contextHints(Map.of(
                "intents", List.of("GENERAL_QUESTION","HOW_TO","COMPARISON"),
                "domains", List.of("all")
            ))
            .build(),
        SearchEngineConfig.builder()
            .id("builtin-wikipedia")
            .engineName("wikipedia")
            .displayName("Wikipedia")
            .baseUrl("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch={query}&srlimit=3&format=json")
            .enabled(true)
            .priority(4)
            .queryTemplate("{query}")
            .maxResults(2)
            .sourceAuthorityWeight(0.80)
            .contextHints(Map.of(
                "intents", List.of("LEARNING","GENERAL_QUESTION"),
                "domains", List.of("all")
            ))
            .build(),
        SearchEngineConfig.builder()
            .id("builtin-mdndocs")
            .engineName("mdn")
            .displayName("MDN Web Docs")
            .baseUrl("https://developer.mozilla.org/en-US/search?q={query}")
            .enabled(true)
            .priority(2)
            .queryTemplate("{query}")
            .resultSelector(".result-item")
            .titleSelector(".result-item-title a")
            .snippetSelector(".result-item-description")
            .maxResults(3)
            .sourceAuthorityWeight(0.90)
            .contextHints(Map.of(
                "intents", List.of("DOCS","HOW_TO","LEARNING"),
                "domains", List.of("javascript","html","css","web")
            ))
            .build()
    );

    /**
     * Returns the ranked list of engines applicable to this query.
     * Admin-added engines appear first (forced priority override).
     */
    public Flux<SearchEngineConfig> resolveEngines(QueryClassification classification) {
        return configRepo.findAll()
            .filter(SearchEngineConfig::getEnabled)
            .collectList()
            .flatMapMany(saved -> {
                List<SearchEngineConfig> result = new ArrayList<>(saved);
                // Merge with defaults for any missing built-in engines
                for (SearchEngineConfig def : DEFAULTS) {
                    boolean exists = result.stream().anyMatch(c -> c.getEngineName().equals(def.getEngineName()));
                    if (!exists) result.add(def);
                }
                // Sort by admin-set priority, then fall back to built-in priority
                result.sort(Comparator
                    .comparingInt(SearchEngineConfig::getPriority)
                    .thenComparing(c -> !(c.getAdminOverrideOnly() != null && c.getAdminOverrideOnly()))
                );
                // Filter by classification match
                return Flux.fromIterable(result)
                    .filter(engine -> engineMatchesClassification(engine, classification));
            });
    }

    private boolean engineMatchesClassification(SearchEngineConfig engine, QueryClassification c) {
        Map<String, Object> hints = engine.getContextHints();
        if (hints == null || hints.isEmpty()) return true; // Default engine, matches everything

        @SuppressWarnings("unchecked")
        List<String> intents = (List<String>) hints.getOrDefault("intents", List.of("GENERAL_QUESTION"));
        if (!intents.contains(c.getIntent().name())) return false;

        @SuppressWarnings("unchecked")
        List<String> domains = (List<String>) hints.getOrDefault("domains", List.of("all"));
        if (!domains.contains("all") && !domains.contains(c.getTechContext().getDomain())) return false;

        return true;
    }
}
```

---

### 5.6 Admin Manual Override — What Admin Controls

The Admin interface at **`/browser/search-config`** (new page) exposes:

| Control | Description | Stored In |
|---------|-------------|-----------|
| **Engine list** | Enable/disable/reorder each search engine | `search_engine_configs` Firestore |
| **Priority slider** | Drag to reorder execution priority for each engine | `search_engine_configs.priority` |
| **Query template** | Edit `{query} {language}` template per engine | `search_engine_configs.queryTemplate` |
| **CSS selectors** | Override result/title/snippet selectors per engine | `search_engine_configs.resultSelector` |
| **Custom engine** | Add entirely new engine with base URL + query template | New `search_engine_configs` record |
| **Intent routing** | Force/intent→engine mapping table | `search_engine_configs.contextHints.intents` |
| **Max results** | Per-engine result limit | `search_engine_configs.maxResults` |
| **Authority weight** | Per-engine confidence multiplier | `search_engine_configs.sourceAuthorityWeight` |
| **Domain allowlist** | Per-engine domain restriction (e.g. GitHub only for java, python) | `search_engine_configs.contextHints.domains` |
| **Override lock** | Lock an engine so it always appears (ignore auto-disable) | `search_engine_configs.enabled` |
| **Test query** | Send a test query through a specific engine and preview raw results | Live API call + result preview |

#### Admin UI Page: `AdminSearchConfig.tsx` (new)

```typescript
// Concept of new admin page at /browser/search-config
<AdminSearchConfig /> 
  ├── Engine Cards (reorderable via drag + drop):
  │    ┌──────────────────────────────────────────┐
  │    │ StackOverflow   [priority 1]    [✓ on]   │
  │    │ Query Template: {query} {language}        │
  │    │ Max Results:    3    Authority: 0.85      │
  │    │ Intents: [ERROR_FIX ✓] [DEBUG ✓] [HOW_TO ✗]│
  │    │ Action: [Test Query ▼] [Edit ▼] [Remove] │
  │    └──────────────────────────────────────────┘
  ├── "Add New Engine" button:
  │    ┌──────────────────────────────────────────┐
  │    │ Name: [Custom Site    ]                   │
  │    │ Base URL: [https://.../{query}   ]        │
  │    │ Query Template: [{query} site:{domain}]   │
  │    │ Selectors: [Title=.title] [Snippet=.desc] │
  │    │ Authority: [0.70]   Max Results: [5]     │
  │    │ [Add]                                     │
  │    └──────────────────────────────────────────┘
  └── SoloSettings Card:
       ├── Solo Mode Enabled: [Switch]
       ├── Default Search Mode: 
       │    [Multi-engine ▼] [Single: DuckDuckGo ▼]
       ├── Solo Max Steps: [15]
       ├── Solo Timeout Ms: [30000]
       ├── Auto-learn Results: [✓ enabled]
       └── Smart Selector Mode: [✓ enabled]
```

---

### 5.7 `IntelligentSearchService` — The Glue (New Java file)

```java
@Service
public class IntelligentSearchService {

    private final QueryClassifier classifier;
    private final SearchQueryTransformer transformer;
    private final SearchEngineRegistry engineRegistry;
    private final BrowserService browserService;
    private final SystemLearningRepository learningRepo;
    private final MultiAIVotingService votingService; // For LLM fallback analysis

    /**
     * Main API for Solo Mode search.
     * Returns ranked, merged, learning-backed answer.
     */
    public Mono<SearchResult> intelligentSearch(String userQuestion, SoloSearchOptions options) {
        return classifyAndPlan(userQuestion, options)
            .flatMap(plan -> Flux.fromIterable(plan.getEngines())
                .flatMap(engine -> searchWithEngine(engine, plan.getTransformedQueries().get(engine.getEngineName())))
                .collectList()
                .flatMap(results -> {
                    // Rank, deduplicate, merge
                    List<SearchResult> ranked = rankResults(results);
                    SearchResult best = ranked.isEmpty() ? emptyResult(userQuestion) : ranked.get(0);

                    // Auto-learn all top results
                    return autoLearnAll(ranked, userQuestion)
                        .thenReturn(best);
                })
            );
    }

    private Mono<QueryPlan> classifyAndPlan(String question, SoloSearchOptions options) {
        QueryClassification classification = classifier.classify(question, options.getKnownDomains());

        // Admin may force single engine (override)
        if (options.getForceEngine() != null) {
            SearchEngineConfig forced = engineRegistry.get(options.getForceEngine());
            return transformer.transform(question, classification, forced)
                .map(t -> {
                    Map<String, String> qs = Map.of(forced.getEngineName(), t.get("query"));
                    return new QueryPlan(classification, List.of(forced), qs);
                });
        }

        return engineRegistry.resolveEngines(classification)
            .flatMap(engine -> transformer.transform(question, classification, engine)
                .map(t -> Map.entry(engine, t)))
            .collectList()
            .map(entries -> {
                Map<String, String> qs = new HashMap<>();
                List<SearchEngineConfig> engines = new ArrayList<>();
                for (Map.Entry<SearchEngineConfig, Map<String, String>> e : entries) {
                    qs.put(e.getKey().getEngineName(), e.getValue().get("query"));
                    engines.add(e.getKey());
                }
                return new QueryPlan(classification, engines, qs);
            });
    }
}
```

---

### 5.8 Playwright Smart Selectors for Search Extraction

Add new TS methods to `browserController.ts` — **these are engine-agnostic**:

```typescript
// Extract search result links and text from any SERP (Search Engine Results Page)
async extractSearchResults(config: SearchExtractConfig): Promise<SearchResult[]> {
    if (!this.page) throw new Error('Browser not launched');

    // Wait for results to appear
    await this.page.waitForSelector(config.resultSelector, { timeout: 8000 }).catch(() => null);
    await this.page.waitForTimeout(1500); // Let dynamic content load

    return this.page.evaluate((cfg) => {
        const results: any[] = [];
        const items = document.querySelectorAll(cfg.resultSelector);
        for (const item of items) {
            try {
                const titleEl = item.querySelector(cfg.titleSelector);
                const snippetEl = item.querySelector(cfg.snippetSelector);
                const linkEl = item.querySelector(cfg.titleSelector);
                results.push({
                    title: titleEl?.textContent?.trim() || '',
                    snippet: snippetEl?.textContent?.trim() || '',
                    url: linkEl?.getAttribute('href') || '',
                    visible: titleEl ? titleEl.offsetParent !== null : false
                });
            } catch (e) { /* skip broken items */ }
        }
        return results.filter(r => r.visible && r.title.length > 0);
    }, config);
}

// Smart link extraction: uses accessibility tree + visual scoring
async extractSmartLinks(maxResults?: number): Promise<SmartLink[]> {
    if (!this.page) throw new Error('Browser not launched');

    return this.page.evaluate((limit) => {
        const links: SmartLink[] = [];
        const seen = new Set<string>();
        document.querySelectorAll('main a[href]').forEach((a: any) => {
            const href = a.href;
            if (seen.has(href) || !href.startsWith('http')) return;
            seen.add(href);
            const text = a.textContent?.trim() || '';
            const rect = a.getBoundingClientRect();
            links.push({
                text: text.substring(0, 200),
                href,
                domain: new URL(href).hostname,
                visible: rect.width > 0 && rect.height > 0,
                x: rect.x,
                y: rect.y,
                area: rect.width * rect.height,
                trustScore: computeTrustScore(href)
            });
        });
        // Sort by trust score + visibility area (prefer bigger, more visible links)
        return links
            .filter(l => l.visible)
            .sort((a, b) => (b.trustScore * 1000 + b.area) - (a.trustScore * 1000 + a.area))
            .slice(0, limit || 10);
    }, maxResults);
}
```

---

### 5.9 Trust Score Authority Tiering (Tier 1–5 Per Source)

Used by `extractSmartLinks` and result ranker:

| Tier | Domains | Weight | When Used |
|------|---------|--------|-----------|
| **Tier 1** (0.90–0.95) | `.gov`, `.edu`, `github.com`, `docs.*` | 0.95 | Official docs, specs |
| **Tier 2** (0.80–0.89) | `stackoverflow.com`, `wikipedia.org`, `npmjs.com`, `pypi.org` | 0.85 | Community knowledge |
| **Tier 3** (0.70–0.79) | `medium.com`, `dev.to`, `reddit.com` | 0.72 | Blogs, discussions |
| **Tier 4** (0.50–0.69) | General web | 0.60 | News, personal blogs |
| **Tier 5** (0.00–0.49) | Unknown / suspicious | 0.30 | Ads, low-authority sites |

Each engine has a `sourceAuthorityWeight` (0.0–1.0) that **multiplies** the tier base score. Result `finalScore = tierBase * engineWeight * recencyBonus`.

---

### 5.10 `QueryClassification` Model

```java
@Document(collectionName = "query_classifications")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class QueryClassification {
    @DocumentId
    private String id;
    private QueryIntent intent;                    // HOW_TO, ERROR_FIX, etc.
    private TechContext techContext;               // domain, language, framework
    private ErrorType errorType;                   // NullPointerException, HTTP_404, etc.
    private Double classificationConfidence;       // 0.0-1.0

    public record TechContext(String domain, String language, String framework) {
        public static final TechContext UNKNOWN = new TechContext("unknown", "unknown", "unknown");
    }
}

public enum QueryIntent {
    ERROR_FIX, HOW_TO, CODE_REVIEW, DEBUG, COMPARISON,
    DOCS, DEPLOY, SECURITY, LEARNING, GENERAL_QUESTION
}

public enum ErrorType {
    NULL_POINTER, CLASS_NOT_FOUND, OUT_OF_MEMORY,
    STACK_OVERFLOW, HTTP_404, HTTP_500,
    SQL_INJECTION, AUTH_FAILURE, MODULE_NOT_FOUND,
    SYNTAX_ERROR, NONE
}
```

---

### 5.11 Full Search Flow — Replaces `soloModeAnswerAndLearn()`

```
soloModeAnswerAndLearn("How do I fix NullPointerException in Spring Boot?")
  │
  ├─► QueryClassifier → intent=ERROR_FIX, domain=java, framework=spring, error=NullPointerException
  │
  ├─► SearchEngineRegistry.resolveEngines(classification)
  │     → [StackOverflow(p1), GitHub Issues(p2), DuckDuckGo(p5)]
  │
  ├─► SearchQueryTransformer.transform() per engine
  │     → StackOverflow: "java spring boot nullpointerexception"
  │     → GitHub:        "NullPointerException repo:spring-projects/spring-boot is:issue"
  │     → DuckDuckGo:    "how to fix NullPointerException Spring Boot 2024"
  │
  ├─► Playwright navigates to each URL (isolated pages)
  │     ├─► extractSearchResults(StackOverflow) → 3 results
  │     ├─► extractSmartLinks(GitHub Issues)    → 2 results
  │     └─► generalWebSearch(DuckDuckGo)        → 2 results
  │
  ├─► ResultRanker.rank(allResults)
  │     → [SO answer: score 0.82, GitHub: 0.79, SO answer: 0.73, DuckDuckGo: 0.55, DDG: 0.50]
  │
  ├─► Final answer compiled from top 3
  │     → Answer + 5 citations + source links
  │
  ├─► Auto-learn ALL top results as SystemLearning
  │     → 5 new entries saved (SO × 3, GitHub × 1, DDG × 1)
  │
  └─► Return:
        { answer, sources: [...], learningIds: [...], confidence: 0.82, enginesUsed: [...] }
```

---

### 5.12 Solo Search Config API Endpoints (new)

```
GET    /api/browser/search/engines          → List all configured engines
POST   /api/browser/search/engines          → Add new engine (admin)
PUT    /api/browser/search/engines/{id}     → Update engine config (admin)
DELETE /api/browser/search/engines/{id}     → Delete engine config (admin)
PUT    /api/browser/search/engines/{id}/priority  → Reorder engine priority (admin)
POST   /api/browser/search/test            → Send test query, return raw results for preview
GET    /api/browser/search/planner          → Show which engines fire for a given classification
GET    /api/browser/search/logs             → Recent solo search logs (admin)
```

---

## 6. Files to Modify

### Tier 1 (Foundation)

| File | Changes |
|------|---------|
| `legacy/browser-automation-tool/` | DELETE — consolidate to one copy |
| `browser-automation-tool/src/browserController.ts` | Expand to 300+ lines (smart methods, bounded screenshot, CDP console, CDP perf, session manager, smart extractors) |
| `browser-automation-tool/src/server.ts` | Add `/test`, `/solo/health-check`, `/extract-text`, `/solo/*` routes, session manager, health preflight |
| `browser-automation-tool/package.json` | Upgrade Playwright, add `@playwright/test` |
| `src/main/java/.../service/browser/BrowserService.java` | Fix trust scoring, credential redaction, strategic learning null-image, step guard, auto-learn confidence |
| `src/main/java/.../service/browser/VisionService.java` | Add text-only `analyzeText(String prompt)` method |
| `src/main/java/.../model/browser/BrowserTask.java` | Add `stepNumber` field |
| `src/main/java/.../service/KnowledgeService.java` | Already partially implements soloModeAnswerAndLearn — prepare for Handover to SoloBrowserService |
| `src/main/java/.../service/SystemWorkRuleService.java` | Add `BROWSER_MAX_STEPS`, `BROWSER_STEP_TIMEOUT_MS` rule keys |

### Tier 2 (Browser IQ)

| File | Changes |
|------|---------|
| `browser-automation-tool/src/browserController.ts` | `SoloSmartSelector`, `extractSmartLinks()`, `extractSearchResults()`, `getPageText()`, `getPageMetadata()` |
| `src/main/java/.../service/browser/BrowserRequestManager.java` | **NEW** — retry/circuit-breaker wrapper between Java and TS |
| `dashboard/src/lib/authUtils.ts` | Add `/browser/search/*` emulator stubs |

### Tier 3 (Solo Session Tickets)

| File | Changes |
|------|---------|
| `src/main/java/.../model/browser/SoloBrowserTicket.java` | **NEW** — ticket record with steps, status, findings |
| `src/main/java/.../repository/SoloBrowserTicketRepository.java` | **NEW** — Firestore repo for tickets |
| `src/main/java/.../service/browser/SoloBrowserService.java` | **NEW** — ticket lifecycle, step planning, answer compilation |
| `browser-automation-tool/src/SoloSessionManager.ts` | **NEW** — TS-side session manager with pruning |
| `browser-automation-tool/src/server.ts` | Add `/solo/*` route handlers (start, step, status, result, stop, log) |
| `src/main/java/.../controller/browser/BrowserController.java` | Add `/solo/*` REST endpoints mapping |

### Tier 4 (Search Intelligence — NEW)

| File | Changes |
|------|---------|
| `src/main/java/.../model/search/QueryClassification.java` | **NEW** — intent, tech context, error type model |
| `src/main/java/.../model/search/SearchEngineConfig.java` | **NEW** — admin-configurable engine config |
| `src/main/java/.../model/search/SearchResult.java` | **NEW** — ranked result with score, source, snippets |
| `src/main/java/.../model/search/QueryPlan.java` | **NEW** — plan: which engines + transformed queries |
| `src/main/java/.../service/search/QueryClassifier.java` | **NEW** — rules-based intent/tech/error classification |
| `src/main/java/.../service/search/SearchQueryTransformer.java` | **NEW** — per-engine query transformation |
| `src/main/java/.../service/search/SearchEngineRegistry.java` | **NEW** — loads engine configs from Firestore + defaults |
| `src/main/java/.../service/search/IntelligentSearchService.java` | **NEW** — orchestration: classify → plan → search → rank → learn |
| `src/main/java/.../service/search/SearchResultRanker.java` | **NEW** — ranks and deduplicates results |
| `src/main/java/.../service/KnowledgeService.java` | Refactor `soloModeAnswerAndLearn()` → delegate to `IntelligentSearchService` |
| `src/main/java/.../repository/search/SearchEngineConfigRepository.java` | **NEW** — Firestore CRUD for engine configs |
| `src/main/java/.../controller/browser/SearchConfigController.java` | **NEW** — REST endpoints for admin search config |

### Tier 5 (Admin UI — Search Config Page)

| File | Changes |
|------|---------|
| `dashboard/src/pages/AdminSearchConfig.tsx` | **NEW** — admin page for engine list, add/edit/remove, test query |
| `dashboard/src/components/browser/SearchTestModal.tsx` | **NEW** — modal: send test query, preview results from any engine |
| `dashboard/src/components/browser/EnginePriorityList.tsx` | **NEW** — drag-reorder engine priority list |
| `dashboard/src/lib/authUtils.ts` | Add `/browser/search/*` emulator stub responses |
| `dashboard/src/components/AdminSystemWorkRules.tsx` | Add search-intelligence related rules |
| `dashboard/src/pages/AutoBrowser.tsx` | Expose "Smart Search" as a mode option alongside current URL+task input |

### Tier 6 (Solo Mode Dashboard & Monitoring)

| File | Changes |
|------|---------|
| `dashboard/src/pages/AdminBrowser.tsx` | Add Solo Mode section (active tickets, progress bar, recent completions, search plan preview) |
| `dashboard/src/components/browser/MissionProtocol.tsx` | Display search plan: which engines fired + which results scored best |
| `dashboard/src/components/browser/IntelligenceFeed.tsx` | Show search events: "Classified as ERROR_FIX → StackOverflow(0.82) + GitHub(0.79)" |
| `admin/dashboard.js` | Add browser/search metrics (engines enabled, top queries, success rate) |

---

## 7. Implementation Sequence (Tiers)

### Tier 1 — Foundation (Blocking Issues)

| # | Item | Files |
|---|------|-------|
| 1.1 | Delete `legacy/` duplicate copy | `legacy/` removal |
| 1.2 | Fix `navigateTo()` — waitUntil + timeout + page context return | `browserController.ts` |
| 1.3 | Fix `getConsoleLogs()` via CDP | `browserController.ts` |
| 1.4 | Fix `runPerformanceTrace()` → CDP Performance domain | `browserController.ts` |
| 1.5 | Add `/extract-text` and `/extract-links` TS endpoints | `server.ts` |
| 1.6 | Fix `recordStrategicLearning()` null-image API contract | `BrowserService.java`, `VisionService` |
| 1.7 | Fix Java→TS API mismatch — POST responses include `result` object | `server.ts` |
| 1.8 | Add `/test`, `/solo/health-check` TS endpoints | `server.ts` |
| 1.9 | Fix step guard + timeout in `executeAutonomousStep()` | `BrowserService.java`, `BrowserTask.java` |
| 1.10 | Fix `getCredentialContext()` decrypt error swallowing + redaction | `BrowserService.java` |
| 1.11 | `BROWSER_MAX_STEPS`, `BROWSER_STEP_TIMEOUT_MS` system work rules | `SystemWorkRuleService.java` |
| 1.12 | Tiered auto-learn confidence scoring | `BrowserService.java` |

### Tier 2 — Browser IQ & Reliability

| # | Item | Files |
|---|------|-------|
| 2.1 | TS: `SoloSmartSelector` + smart link finder | `browserController.ts` |
| 2.2 | TS: bounded screenshot (max-width, clamping) | `browserController.ts` |
| 2.3 | TS: CDP console log capture | `browserController.ts` |
| 2.4 | TS: CDP performance trace | `browserController.ts` |
| 2.5 | Java: `BrowserRequestManager` circuit-breaker | `BrowserRequestManager.java` |
| 2.6 | Java: externalise `calculateTrustScore()` to `SystemWorkRule` | `BrowserService.java`, `SystemWorkRuleService` |
| 2.7 | Java: Playwright-backed `scroll()` endpoint | `server.ts` |

### Tier 3 — Solo Session Tickets

| # | Item | Files |
|---|------|-------|
| 3.1 | Java model: `SoloBrowserTicket` + `SoloBrowserTicketRepository` | NEW files |
| 3.2 | TS: `SoloSessionManager` + `/solo/start`, `/solo/step`, `/solo/status`, `/solo/result`, `/solo/stop` | `server.ts` |
| 3.3 | Java: `SoloBrowserService` ticket lifecycle | NEW |
| 3.4 | Java controller REST endpoints for ticket lifecycle | `BrowserController.java` |
| 3.5 | Java tests for ticket lifecycle | NEW test |
| 3.6 | TS: `SoloActionQueue` — queue-based action executor with guards | `server.ts` |
| 3.7 | Java: simple goal→action planner for Solo Mode (keyword-driven) | `SoloBrowserService` |

### Tier 4 — Search Intelligence (NEW — core of "where/how/what to search")

| # | Item | Files |
|---|------|-------|
| 4.1 | Java model: `QueryClassification`, `QueryIntent`, `TechContext`, `ErrorType` | NEW `search/` model package |
| 4.2 | Java model: `SearchEngineConfig`, `SearchResult`, `QueryPlan`, `SoloSearchOptions` | NEW `search/` model package |
| 4.3 | Java: `QueryClassifier` — zero-AI rules-based query classification | NEW |
| 4.4 | Java: `SearchQueryTransformer` — per-engine query transformation | NEW |
| 4.5 | Java: `SearchEngineRegistry` — loads engine configs from Firestore + built-in defaults | NEW |
| 4.6 | Java: `SearchResultRanker` — ranks/deduplicates by tier × engine × recency | NEW |
| 4.7 | Java: `IntelligentSearchService` — orchestrates classify→plan→search→rank→learn | NEW |
| 4.8 | Java: `SearchEngineConfigRepository` — Firestore CRUD | NEW |
| 4.9 | Java: `SearchConfigController` — REST API for admin search config | NEW |
| 4.10 | TS: `extractSearchResults()` + `extractSmartLinks()` + `extractPageContent()` | `browserController.ts` |
| 4.11 | Refactor `KnowledgeService.soloModeAnswerAndLearn()` → delegates to `IntelligentSearchService` | `KnowledgeService.java` |

### Tier 5 — Admin UI — Search Configuration Page

| # | Item | Files |
|---|------|-------|
| 5.1 | `AdminSearchConfig.tsx` — engine list, add/edit/delete, reorder, test query | NEW |
| 5.2 | `SearchTestModal.tsx` — live result preview for any engine | NEW |
| 5.3 | `EnginePriorityList.tsx` — drag-reorder engine card list | NEW |
| 5.4 | Solo Mode settings panel in `AutoBrowser.tsx` or `AdminBrowser.tsx` | `AutoBrowser.tsx`, `AdminBrowser.tsx` |
| 5.5 | Emulator stubs for `/browser/search/*` endpoints | `dashboard/src/lib/authUtils.ts` |
| 5.6 | SoloSettings card in `AdminSettings.tsx` (or new page) | `AdminSettings.tsx` |

### Tier 6 — Solo Dashboard & Monitoring

| # | Item | Files |
|---|------|-------|
| 6.1 | Java: `/browser/solo/health` + `/browser/solo/active-tickets` + `/browser/solo/recent-completions` | `BrowserController.java`, `SoloBrowserService` |
| 6.2 | Admin browser — Solo Mode section (active ticket count, step progress, replay) | `AdminBrowser.tsx` |
| 6.3 | `MissionProtocol.tsx` — show which engines fired + their result scores | `MissionProtocol.tsx` |
| 6.4 | `IntelligenceFeed.tsx` — show search events: intent, engine used, confidence | `IntelligenceFeed.tsx` |
| 6.5 | `SoloBrowserMetrics` DB table for auditing | NEW migration |
| 6.6 | Search log streaming via WebSocket to admin dashboard | `WebSocketController.java` |

---

## 7. Success Criteria

| Criterion | Metric |
|-----------|--------|
| Solo Mode consistently uses Playwright for research | Code path from `soloModeAnswerAndLearn` → `IntelligentSearchService` → Playwright (not just `HttpClient`) |
| 0 knowledge-base JS bugs in tests | `BrowserControllerTest` / `BrowserServiceTest` all green |
| Average Solo answer accuracy | Domain-specific benchmarking > 85% |
| Solo Mode crash resilience | Restarts within 2s and recovers previous ticket state |
| Solo answer uses 2+ sources | ≥ 2 `SearchEngineConfig` engines fire on ≥ 70% of queries |
| Search accuracy improvement | Same query yields higher answer quality with 3 sources vs. single DuckDuckGo |
| **WHERE**: query routed to right engine | `QueryClassifier` routes ≥ 90% of knowledge queries to the correct engine(s) |
| **Admin override**: engine list is live | Admin adds/disables/reorders engine → active on next Solo Mode query with no restart |
| **WHERE**: custom engine works end-to-end | Admin adds `docs.python.org` → it fires in query plan and returns ranked results |
| **HOW**: query engine optimised | `SearchQueryTransformer` converts "nullpointer spring boot" to `"java spring boot nullpointerexception"` for StackOverflow |
| **WHAT**: smart link extraction | Browser picks best result link from 5+ results using `extractSmartLinks()` trust + visibility scoring |
| **WHAT**: structured extraction | Answer compiles from ranked, deduplicated multi-source content |
| Admin Solo dashboard | Live active ticket count, step progress, realtime `< 2s` lag |
| Auto-learn entries from solo sessions | ≥ 3 entries per active Solo ticket per session |
| Code change single truth | No `legacy/` copy of same code in two places |

---

## 8. Quick-Reference: Admin Controls Summary

```
Admin Search Config Page (/browser/search-config)
├── STACK OVERFLOW          [priority 1] [✓ on]
│    Query:  {query} {language} {framework}
│    Select: .s-post-summary → .title a → .excerpt
│    Max:    3   Authority: 0.85
├── GITHUB ISSUES           [priority 2] [✓ on]
│    Query:  {query} repo:{domain} is:issue
│    Select: .issue-list-item → title a → .markdown-title
│    Max:    3   Authority: 0.88
├── DUCKDUCKGO              [priority 5] [✓ on]
│    Query:  {query} 2024
│    JSON API backfill
│    Max:    5   Authority: 0.60
├── WIKIPEDIA               [priority 4] [✓ on]
│    Query:  {query}
│    API:    en.wikipedia.org/w/api.php
│    Max:    2   Authority: 0.80
├── MDN WEB DOCS            [priority 2] [✓ on]
│    Query:  {query}
│    Select: .result-item → a → .desc
│    Max:    3   Authority: 0.90
│
├─[+ ADD CUSTOM ENGINE]
│    Name: [Custom Site    ]  Base URL: [https://.../{query}]
│    Query Template: [{{query}} site:{{domain}}]
│    Selectors: [Result=.r] [Title=.title] [Snippet=.desc]
│    Authority: [0.75]  Max Results: [5]
│    [Add to Registry] [Test Query ▶]
│
├─ Solo Mode Settings ────────────────────────────────────────
│    Smart Search:   [✓ Enabled]   Smart Selector: [✓ Enabled]
│    Default Mode:   [Multi-engine ▼]  Max Steps: [15]  Timeout: [30000ms]
│    Auto-learn:     [✓ Enabled]
│
└─ Intent → Engine Routing Table ─────────────────────────────
   ERROR_FIX    → StackOverflow(1) → GitHub(2) → DuckDuckGo(5)
   HOW_TO       → DuckDuckGo(5) → Medium(6) → YouTube()
   CODE_REVIEW  → GitHub(2) → DuckDuckGo(5)
   SECURITY     → NIST(3) → OWASP(4) → DuckDuckGo(5)
    DOCS         → MDN(2) → Official Docs(3) → Wikipedia(4)
   [Edit routing →]
```
