# Browser Scraping & Web Search — Full System Plan

## Vision
All web-searching and scraping logic lives inside one **scraping engine** (controlled from Firebase), served through the existing `browser-automation-tool` Playwright server. No hardcoded URLs anywhere. The admin configures scrape/search policies per conversation type via Firestore documents.

---

## 1. Firebase — Configuration Data Model

All configuration lives in **Firestore** (no hardcoding):

| Collection | Document | Purpose |
|---|---|---|
| `scrapePolicies` | `global` | Top-level switch: enable/disable scraping globally, max concurrent sessions |
| `scrapePolicies/{type}` | `greeting` / `question` / `command` / `complex` | Per-conversation-type policy |
| `scrapePresets` | `{id}` | Named presets: search-engines, allowed-domains, max-depth, content-filters |
| `scrapeAllowedDomains` | `{domain}` | Per-domain permissions (who can be scraped) |
| `scrapeHistory` | `{sessionId}` | Session history for audit and analytics |
| `scrapeEvent` | `{randomId}` | Per-request event log for debugging |

### `scrapePolicies/{type}` document fields

```
enabled: boolean               ← scraping ON for this type?
maxDepth: int                  ← max pages to follow (0 = 1 page only)
maxResults: int                ← max results to return
allowedDomains: string[]       ← Firestore ref[] to scrapeAllowedDomains
searchEngines: string[]        ← e.g. ["google", "bing", "duckduckgo"]
contentTypes: string[]         ← ["text","image","link","table"]
extractStrategy: string        ← "selenium" | "youtube-webpage" | "article-extract"
fallbackAndRetry: boolean      ← if primary fails, try alternate
rateLimitMs: number            ← min ms between requests to same host
timeoutMs: number              ← per-request timeout
cacheTTL: number               ← seconds to cache results
```

### `scrapeAllowedDomains` document fields

```
domain: string                 ← e.g. "github.com", "reddit.com"
allowedPaths: string[]         ← URL path allowlist (empty = all)
allowedTypes: string[]         ← ["search","extract","navigate"]
trustLevel: string             ← "trusted" | "standard" | "suspicious"
rateLimitMs: number            ← domain-specific rate limit
enabled: boolean
```

---

## 2. Chat Classifier — Dynamic Intent Detection

Based on the existing `chat_classifier.py` pattern, the classifier maps every incoming message to a **conversation type** before any scraping decision is made:

| Conversational Type | Examples | Scrape Action |
|---|---|---|
| `GREETING` | "hi", "hello", "hlw", "হ্যালো" | No scraping. Return local greeting from `core_knowledge.json`. |
| `SIMILAR` | "how are you", "কেমন আছো" | No scraping. Return local small-talk response. |
| `SIMPLE_QUESTION` | "what is react", "What is 2+2" | Single search-engine query → extract top result snippets. |
| `COMPLEX_QUESTION` | "compare Next.js vs Svelte with benchmarks" | Multi-source search → crawl 2–3 pages → merge + summarize. |
| `FOLLOW_UP` | "tell me more", "and?", "আরও বলো" | Reuse previous scrape session's cached context. |
| `COMMAND` | "deploy this", "run test" | Execute backend command, no scraping. |
| `UNKNOWN` | gibberish / out-of-scope | Ask for clarification, no scraping. |

**Classifier logic:** Pattern-score → highest-scoring category wins. No keyword list is hardcoded in the planner — they live in `scrapePolicies/greeting`, `scrapePolicies/complex`, etc., and the classifier reads them dynamically from Firestore at startup.

---

## 3. Scraping Engine — Core Flow (for every conversation type)

```
┌──────────────────────────────────────────────────────────────────┐
│  USER SEND MESSAGE                                                │
└───────────────────────────┬──────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│  STEP 1 — CLASSIFY INTENT                                        │
│  Read scrapePolicies from Firestore                              │
│  Match message against regex patterns + urgency score            │
│  Output: chatType = GREETING | QUESTION | COMMAND | UNKNOWN      │
└───────────────────────────┬──────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│  STEP 2 — LOOKUP POLICY FOR THIS TYPE                            │
│  Read scrapePolicies/{type} from Firestore                       │
│  If policy.enabled == false → skip to local knowledge            │
└───────────────────────────┬──────────────────────────────────────┘
                            │
               ┌────────────┴────────────┐
               │ POLICY ENABLED?          │
               └──────┬───────────────┬───┘
                      │ YES           │ NO
                      ▼               ▼
              ┌──────────┐    ┌──────────────────┐
              │ scraping │    │ core_knowledge   │
              │  path    │    │  .json + respond │
              └────┬─────┘    └──────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────────────┐
│  STEP 3 — BUILD SEARCH ENTRY POINT                               │
│  read searchEngines[] from policy                               │
│  For each engine: build URL from template config                 │
│  (Google: https://google.com/search?q={query}&tbm={type})        │
│  Filter through allowedDomains — skip disallowed hosts           │
└───────────────────────────┬──────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│  STEP 4 — LAUNCH PLAYWRIGHT SESSIONS (parallel)                  │
│  POST /navigate  → each search engine URL                        │
│  Wait for page.metatitle to load                                 │
│  Apply rateLimitMs per domain (shared token bucket)              │
└───────────────────────────┬──────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│  STEP 5 — EXTRACT RESULTS (by extractStrategy)                   │
│  "article-extract" → getAccessibilityTree + getPageContent       │
│         → run readability / Readability.js innerText extraction   │
│  "youtube-webpage" → extract transcript, title, publishDate      │
│  "selenium"        → click-pagination, scroll, lazy-load capture  │
│  Default            → accessibility tree + text content           │
└───────────────────────────┬──────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│  STEP 6 — SCRAPE DEEPER (only if maxDepth > 0)                   │
│  For each result URL:                                             │
│  1. Check if domain ∈ allowedDomains                             │
│  2. POST /navigate to result URL                                 │
│  3. Extract page content by extractStrategy                       │
│  4. Crawl up to maxDepth - 1 levels of outbound links             │
│  5. Stop on redirect loops (max 5 hops per chain)                │
└───────────────────────────┬──────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│  STEP 7 — MERGE + SUMMARIZE                                      │
│  Combine all extracted raw text chunks                            │
│  Deduplicate by URL + content hash                                │
│  Deduplicate by content similarity threshold (0.85 Jaccard)       │
│  Run summarizer (local or AI) on per-source chunk                 │
│  Merge into coherent final answer                                 │
└───────────────────────────┬──────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│  STEP 8 — STORE SESSION DATA IN FIRESTORE                        │
│  scrapeHistory/{sessionId}:                                      │
│    query, chatType, sources[], rawChunks[], finalAnswer          │
│    confidence, timestamp, userFeedback                            │
└───────────────────────────┬──────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│  STEP 9 — RETURN RESPONSE                                         │
│  Return merged answer to user                                     │
│  If confidence < threshold → ask user "was this helpful?"         │
│  Store user correction in system_learning                         │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. How Each Conversation Type Works Dynamically

### GREETING / SIMILAR

```
No scraping.
Lookup handled in priority order:
  1. core_knowledge.json  →  greeting patterns
  2. If found → respond from local knowledge immediately
  3. If not found → generic default greeting from hardcoded fallback
  Duration: < 50ms
```

### SIMPLE_QUESTION

```
Scraping flow:
  1. Build search URL from searchEngines[0] in current policy
  2. POST /navigate (search engine result page)
  3. POST /screenshot for visual verification (optionally)
  4. GET /content → parse result links
  5. POST /navigate → top 3 result URLs (if not disallowed domain)
  6. GET /accessibility → extract main article text from each
  7. Return: top 3 extracted answers with source URLs
  Max depth: 1 crawl (search page + 3 results = 4 pages)
```

### COMPLEX_QUESTION

```
Scraping flow:
  1. Classify sub-questions: break complex input into facets
  2. For each facet:
       a. search facet → GET SERP
       b. Navigate top 3–5 results per facet
       c. Deep-crawl each result (maxDepth - 1 levels)
  3. Merge all facets → summarise with overlap-resolution
  4. Apply fact-agreement scoring if ≥2 sources converge on same point
  5. Flag uncertainty if sources disagree (confidence < threshold)
  Max depth: 3 crawl levels per facet
```

### FOLLOW_UP

```
No fresh scraping unless context is stale (> cacheTTL seconds).
Steps:
  1. Check scrapeHistory/{lastSessionId} for cached context
  2. If still fresh → answer from cached context + add to follow-up context
  3. If stale → re-scrape only the latest queries
  4. Append new context to same session in Firestore
```

### COMMAND

```
No scraping. Execute system commands via:
  1. Check command against allowed-command whitelist in Firestore
  2. Run command in backend
  3. Return structured output
  Scraping is never triggered by command-type messages
```

---

## 5. Data-Extraction Patterns (dynamic — not coded)

The extract strategy is chosen at runtime from `scrapePresets`:

| Strategy | Pattern | Use When |
|---|---|---|
| `article-extract` | `<article>` tag + readability algo + meta tags | Blog posts, news articles, tutorials |
| `youtube-webpage` | Search page heading text + word count | YouTube video transcript pages |
| `selenium` | Click-pagination + scroll-to-bottom + waiting for lazy-load elements | E-commerce, infinite-scroll feeds |
| `table-extract` | `//table` → parse rows/cols as JSON | Product comparison, pricing tables |
| `code-extract` | Look for `<pre><code>` blocks with language hint | Stack Overflow, documentation sites |
| `image-extract` | Parse `<img src>` + `alt` + surrounding context | Image search, visual Q&A |
| `discussion-extract` | Parse comment clusters by nesting depth + vote counts | Reddit, Hacker News, forum threads |

Extract strategies are stored as **templates in Firestore** — adding a new strategy is adding a document, not changing code.

---

## 6. Dynamic Routing Table (Firestore-controlled)

```
Policy → what_url_template → extract_strategy
  ───────────────────────────────────────────────────────
  google       → https://google.com/search?q={q}&tbm={type}    →  article-extract
  bing         → https://bing.com/search?q={q}                  →  article-extract
  duckduckgo   → https://duckduckgo.com/?q={q}                  →  article-extract
  youtube      → https://youtube.com/results?search_query={q}   →  youtube-webpage
  reddit       → https://reddit.com/search?q={q}                 →  discussion-extract
  stackoverflow→ https://stackoverflow.com/search?q={q}          →  code-extract
  github       → https://github.com/search?q={q}                  →  code-extract
  wikipedia    → https://wikipedia.org/w/index.php?search={q}&t=search → article-extract
```

Adding a new target = adding a new `scrapePresets/{id}` document. Zero code changes.

---

## 7. Rate-Limit, Retry, and Cache

All driven from Firestore `scrapePolicies`:

```
rateLimitMs: per-domain token bucket (shared across all processes)

maxDepth: 0 = single search hover; 1 = search + 1 crawl; 2 = deep crawl

retryPolicy:
  maxAttempts: int
  backoffMs: int
  statusCodes: [502, 503, 504, 429]  → eligible for retry

cacheTTL: seconds
  Check Firestore scrapeHistory: if same query was asked < TTL ago → return cached
```

---

## 8. Firebase Collections — Summary

```
scrapePolicies/
  global       → global toggle, defaults
  greeting     → for hi/hello/hlw type messages
  question     → for simple typed questions
  complex      → for hard research-type questions
  command      → scrape disabled here

scrapePresets/
  {id}         → named scraping configurations (search URL template,
                 extract strategy, allowed sources, max depth)

scrapeAllowedDomains/
  {domain}     → per-domain whitelist with trust level, allowed path,
                 rate limit

scrapeHistory/
  {sessionId}  → query, type, sources, raw chunks, final answer,
                 confidence, feedback, timestamp

scrapeEvent/
  {randomId}   → per-request event (navigate clicked?, extract ran?,
                 domain skipped?, error code, timing)
```

---

## 9. Files to Modify / Create

| File | Action |
|---|---|
| `browser-automation-tool/src/browserController.ts` | **Heavily update** — add crawl(), extractText(), getSearchResults(), checkDomainAllowed(), injectReadability() |
| `browser-automation-tool/src/server.ts` | **Heavily update** — add /scrape, /search, /extract, /health endpoints; Firebase credential ingestion |
| `functions/src/scrapeEngine.ts` | **New** — orchestrates scraping, reads Firebase config, calls Playwright server |
| `dataconnect/schema/scrapeSchema.yaml` | **New** — Firestore Document schemas for scrape* collections |
| `functions/src/scrapeHistoryManager.ts` | **New** — Firestore read/write for scrapePolicies, history, events |
| `functions/src/chatClassifier.ts` | **New** — intent classifier (hybrid regex + embedding-trigger) |
| `autonomous_seed_knowledge.json` | **Update** — add scene-scraper-collection knowledge entries |
