#!/usr/bin/env python3
"""
Part 6 — Performance Optimisation
Seeds SupremeAI Firebase with deep knowledge about:
  • Caching strategies (application cache, CDN, HTTP cache headers)
  • JVM performance tuning (GC, heap sizing, JIT)
  • Database performance (query tuning, connection pooling, read replicas)
  • API performance (pagination, compression, streaming, async)
  • Frontend performance (React rendering, code splitting, lazy loading)
  • Load balancing and horizontal scaling
  • Profiling and performance analysis

Collections written:
  • system_learning       (SystemLearning model records)
  • performance_knowledge (rich topic documents)

Run:
  pip install firebase-admin
  python seed_part6_performance.py [--dry-run]
"""

import sys
import os

sys.path.insert(0, os.path.dirname(__file__))

from seed_lib import _learning, run_part

# ============================================================================
# SYSTEM_LEARNING records
# ============================================================================

SYSTEM_LEARNINGS = {
    "perf_caching_strategy": _learning(
        type_="PATTERN",
        category="PERFORMANCE",
        content=(
            "Multi-level caching strategy — apply from fastest to slowest: "
            "L1 — In-process cache (Caffeine): nanosecond access, shared across requests in same JVM. "
            "L2 — Distributed cache (Redis): microsecond access, shared across all instances. "
            "L3 — Database / external API: millisecond to second access. "
            "Cache-aside pattern: check cache → miss → read DB → populate cache → return. "
            "Write-through: write to cache AND DB in same transaction. "
            "Write-behind (write-back): write to cache; DB updated asynchronously (risk of data loss). "
            "Spring: @Cacheable on service methods; configure cache manager with Caffeine + Redis."
        ),
        solutions=[
            "Use Caffeine for local cache (low-latency, high-throughput); Redis for shared/distributed",
            "@Cacheable(value='users', key='#id') — Spring auto-manages cache population and eviction",
            "@CacheEvict(value='users', key='#user.id') on update/delete methods",
            "Set TTL: static data (1 hour), session data (15 min), dynamic counts (30 sec)",
            "Monitor cache hit rate — target > 80%; investigate if < 60%",
        ],
        severity="HIGH",
        confidence=0.96,
        times_applied=134,
        context={
            "spring_config": "spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=300s",
            "redis_spring": "spring.cache.type=redis + spring.cache.redis.time-to-live=300000",
        },
    ),
    "perf_spring_boot_async": _learning(
        type_="PATTERN",
        category="PERFORMANCE",
        content=(
            "Async processing in Spring Boot for throughput improvement: "
            "@Async annotation: run a method in a thread pool instead of the caller's thread. "
            "Return CompletableFuture<T> for async with result; void for fire-and-forget. "
            "@EnableAsync must be on a @Configuration class. "
            "Configure thread pool: ThreadPoolTaskExecutor with corePoolSize, maxPoolSize, queueCapacity. "
            "Spring WebFlux (Reactor): fully non-blocking reactive stack — handles 10x more concurrent "
            "requests than Spring MVC with same thread count (no thread-per-request model)."
        ),
        solutions=[
            "Add @EnableAsync to @SpringBootApplication class",
            "Create custom executor bean: @Bean TaskExecutor asyncExecutor() with explicit pool sizing",
            "Return CompletableFuture.supplyAsync() for chaining async operations",
            "For I/O-bound workloads: Spring WebFlux + WebClient (reactive) vs RestTemplate (blocking)",
            "Monitor thread pool with Micrometer: executor.active, executor.pool.size, executor.queue.size",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=89,
        context={
            "pool_sizing": "I/O-bound: 10× CPU cores; CPU-bound: CPU cores + 1",
            "warning": "Propagate MDC context to async threads: DelegatingSecurityContextTaskExecutor + MDC copy",
        },
    ),
    "perf_jvm_gc_tuning": _learning(
        type_="PATTERN",
        category="PERFORMANCE",
        content=(
            "JVM GC tuning for Spring Boot production workloads: "
            "G1GC (default in Java 9+): good balance for heap 4GB-32GB; "
            "tune with -XX:MaxGCPauseMillis=200 (target max pause). "
            "ZGC (Java 15+): sub-millisecond pauses; use for latency-sensitive services with heap > 8GB; "
            "enable with -XX:+UseZGC. "
            "Shenandoah: concurrent, low-pause; good for large heaps. "
            "Heap sizing: -Xms = -Xmx (avoid heap resizing); "
            "-XX:MaxRAMPercentage=75.0 for containers (JVM uses 75% of container limit)."
        ),
        solutions=[
            "Container: -XX:MaxRAMPercentage=75.0 instead of fixed -Xmx to adapt to container size",
            "Enable GC logging: -Xlog:gc*:file=/app/logs/gc.log:time,level for production analysis",
            "Use G1GC for most apps; switch to ZGC if GC pauses > 100ms are impacting P99 latency",
            "Profile with async-profiler to find allocation hotspots causing GC pressure",
            "Use JVM metaspace: -XX:MaxMetaspaceSize=256m to prevent unbounded metaspace growth",
        ],
        severity="HIGH",
        confidence=0.93,
        times_applied=67,
        context={
            "cloud_run": "-XX:+UseContainerSupport (enabled by default since Java 10) + -XX:MaxRAMPercentage=75.0",
            "profiler": "async-profiler (open source) or JFR (Java Flight Recorder, built-in Java 11+)",
        },
    ),
    "perf_api_pagination": _learning(
        type_="PATTERN",
        category="PERFORMANCE",
        content=(
            "API pagination strategies: "
            "Offset pagination: SELECT ... LIMIT 20 OFFSET 100 — simple but gets slower as offset increases "
            "(O(offset) rows scanned + discarded). Breaks on concurrent inserts. "
            "Keyset (cursor) pagination: WHERE id > :lastId LIMIT 20 — constant O(1) time, "
            "works correctly with concurrent writes. Best for: large tables, infinite scroll. "
            "Cursor pagination: opaque Base64 cursor encoding (id + timestamp for multi-sort). "
            "Page size limits: cap at 100 items max; default 20. "
            "Include next/prev links (HATEOAS) or next_cursor in response."
        ),
        solutions=[
            "Replace OFFSET pagination with keyset when table > 100k rows",
            "Spring Data JPA: Slice<T> or Page<T>; use Slice for cursor, Page for offset",
            "Expose via: GET /api/users?after=<cursor>&limit=20 — RESTful cursor API",
            "Cache popular first pages with @Cacheable (ttl=60s) — most traffic hits page 1",
            "GraphQL: use Relay cursor connection spec for standardised pagination",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=112,
        context={
            "benchmark": "OFFSET=10000 on 1M rows: 50ms; keyset equivalent: 2ms — 25x faster",
            "spring_tip": "Pageable with Sort.by('id').ascending() for keyset compatibility",
        },
    ),
    "perf_http_compression": _learning(
        type_="PATTERN",
        category="PERFORMANCE",
        content=(
            "HTTP response compression reduces bandwidth and improves perceived latency: "
            "Gzip: widely supported, 60-80% size reduction for JSON/HTML/CSS/JS. "
            "Brotli: 15-25% better than gzip but requires more CPU; supported in all modern browsers. "
            "Spring Boot: server.compression.enabled=true; server.compression.min-response-size=1024 "
            "(only compress responses > 1KB). "
            "Compress at: reverse proxy (nginx/Cloud Run ingress) for best performance — "
            "offloads CPU from Spring Boot."
        ),
        solutions=[
            "Enable: server.compression.enabled=true in application.properties",
            "Set mime types: server.compression.mime-types=application/json,application/xml,text/html",
            "Use Cloud Run or nginx for Brotli compression — Spring Boot only supports gzip natively",
            "Verify compression: curl -H 'Accept-Encoding: gzip' -I https://api.example.com/api/data",
            "Do NOT compress already-compressed content (images, video, encrypted data)",
        ],
        severity="MEDIUM",
        confidence=0.94,
        times_applied=78,
        context={
            "bandwidth_saving": "Typical JSON API response: 10KB → 2KB with gzip (80% reduction)"
        },
    ),
    "perf_database_read_replicas": _learning(
        type_="PATTERN",
        category="PERFORMANCE",
        content=(
            "Database read replicas for scaling read-heavy workloads: "
            "Architecture: primary handles writes; replicas handle reads via async replication. "
            "Replication lag: typically 10-100ms for Postgres streaming replication — "
            "reads may return stale data; use for eventually-consistent use cases. "
            "Spring configuration: @Primary DataSource for writes, secondary DataSource for reads. "
            "Use @Transactional(readOnly=true) on service methods — Spring routes to read replica "
            "when read-replica routing is configured."
        ),
        solutions=[
            "Use Spring's AbstractRoutingDataSource to route readOnly=true to replica",
            "Add cloud SQL read replica in GCP: gcloud sql instances create replica --master-instance-name=primary",
            "Use Vitess or ProxySQL for transparent read/write splitting at proxy layer",
            "For Firestore: reads are already distributed — no manual replica management needed",
            "Monitor replication lag: pg_stat_replication.write_lag on primary",
        ],
        severity="HIGH",
        confidence=0.91,
        times_applied=44,
        context={
            "cloud_sql": "Cloud SQL read replicas: automatic, managed, in same or different region",
            "lag_threshold": "Alert if replication lag > 5s — reads may be significantly stale",
        },
    ),
    "perf_react_rendering": _learning(
        type_="PATTERN",
        category="FRONTEND_PERFORMANCE",
        content=(
            "React rendering performance optimisation: "
            "React.memo: prevents re-render if props haven't changed (shallow compare). "
            "useMemo: memoise expensive computed values; useCallback: memoised function reference. "
            "Key prop: stable key on list items prevents unnecessary DOM reconciliation. "
            "Virtualisation: react-window or react-virtual for lists > 100 items — renders only visible rows. "
            "Code splitting: React.lazy + Suspense for route-level code splitting. "
            "Profiler: React DevTools Profiler identifies slow components and unnecessary re-renders."
        ),
        solutions=[
            "Wrap pure functional components with React.memo() to prevent prop-unchanged re-renders",
            "useMemo(() => expensiveCalc(data), [data]) — only recalculates when data changes",
            "useCallback(() => handleClick(), [dependency]) — stable function reference for child components",
            "Use react-window FixedSizeList for large lists — renders only visible rows",
            "Lazy load routes: const Dashboard = React.lazy(() => import('./Dashboard'))",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=89,
        context={
            "anti_pattern": "Inline object/array creation in JSX props causes re-renders on every parent render",
            "tool": "React DevTools Profiler + why-did-you-render library for re-render debugging",
        },
    ),
    "perf_cdn_static_assets": _learning(
        type_="PATTERN",
        category="PERFORMANCE",
        content=(
            "CDN (Content Delivery Network) for static asset delivery: "
            "Firebase Hosting CDN: automatically serves static assets from nearest PoP worldwide. "
            "Cache-Control headers: immutable assets (JS/CSS bundles with hash in filename): "
            "Cache-Control: public, max-age=31536000, immutable. "
            "HTML entry points: Cache-Control: no-cache (revalidate, may use browser cache). "
            "API responses: Cache-Control: private, no-cache (no CDN caching of personalised data). "
            "ETag + Last-Modified: enable conditional requests — server returns 304 Not Modified if unchanged."
        ),
        solutions=[
            "Configure Firebase Hosting: set cache headers in firebase.json headers array",
            "Use content-hash filenames (bundle.abc123.js) for immutable caching of JS/CSS",
            "Set Cache-Control: no-cache on index.html so browser always checks for new deployment",
            "Add CDN in front of Cloud Run API for public, cacheable API responses",
            "Use Cloudflare or Google Cloud CDN for API edge caching of public endpoints",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=67,
        context={
            "firebase_hosting": "Vite/CRA build output automatically deployed to Firebase Hosting CDN",
            "ttl_guide": "Immutable assets: 1 year; API public: 60s; API private: no-cache",
        },
    ),
    "perf_spring_boot_profiling": _learning(
        type_="PATTERN",
        category="PERFORMANCE",
        content=(
            "Profiling Spring Boot applications to find bottlenecks: "
            "JFR (Java Flight Recorder): built into JDK 11+; low-overhead; records CPU, memory, I/O, locks. "
            "Start: jcmd <pid> JFR.start name=profile duration=60s filename=profile.jfr. "
            "async-profiler: low-overhead CPU and allocation profiler; works with JFR. "
            "Spring Actuator /actuator/threaddump: shows thread states (BLOCKED = lock contention). "
            "VisualVM / JConsole: remote attach to JVM for real-time monitoring. "
            "Heap dump: jmap -dump:file=heap.hprof <pid> → analyse with Eclipse MAT."
        ),
        solutions=[
            "Start with /actuator/metrics and /actuator/threaddump before full profiling",
            "Enable JFR: -XX:StartFlightRecording=duration=300s,filename=/tmp/profile.jfr",
            "Use async-profiler for production CPU profiling: no safepoint bias, very low overhead",
            "Analyse heap dump with Eclipse MAT: find GC roots, retained heap, leak suspects",
            "Add @Timed on suspected slow methods to isolate latency without full profiler",
        ],
        severity="MEDIUM",
        confidence=0.92,
        times_applied=45,
        context={
            "jfr": "Java Flight Recorder: < 1% overhead; safe for production profiling",
            "download": "async-profiler: github.com/async-profiler/async-profiler",
        },
    ),
    "perf_webflux_reactive": _learning(
        type_="PATTERN",
        category="PERFORMANCE",
        content=(
            "Spring WebFlux (Project Reactor) for high-concurrency I/O-bound workloads: "
            "Non-blocking: threads never block waiting for I/O — can handle 10,000+ concurrent connections "
            "with a few threads. "
            "Mono<T>: 0 or 1 element; Flux<T>: 0 or N elements. "
            "Use when: many concurrent users, I/O-heavy (DB/API calls), streaming data. "
            "Do NOT mix blocking calls in reactive chain — use Schedulers.boundedElastic() for blocking code. "
            "R2DBC: reactive non-blocking database driver (replaces JDBC in reactive stack)."
        ),
        solutions=[
            "Replace RestTemplate with WebClient for non-blocking external HTTP calls",
            "Use R2DBC + Spring Data R2DBC instead of JPA for reactive database access",
            "Wrap blocking calls: Mono.fromCallable(blockingCall).subscribeOn(Schedulers.boundedElastic())",
            "Use WebFlux for: streaming endpoints (SSE, WebSocket), high-concurrency APIs",
            "Stick with Spring MVC for: complex business logic, transaction-heavy, team familiarity",
        ],
        severity="HIGH",
        confidence=0.90,
        times_applied=38,
        context={
            "benchmark": "Spring WebFlux handles 3-10x more concurrent requests than Spring MVC for I/O-bound workloads",
            "trade_off": "Harder to debug, different programming model; use only when throughput is critical",
        },
    ),
    "perf_database_connection_tuning": _learning(
        type_="PATTERN",
        category="PERFORMANCE",
        content=(
            "Database connection optimisation: "
            "HikariCP pool sizing formula: pool_size = (core_count × 2) + effective_spindle_count. "
            "Too small: threads wait for connections → high latency. "
            "Too large: DB overwhelmed → context switch overhead. "
            "Statement caching: HikariCP sets prepStmtCacheSize=250, prepStmtCacheSqlLimit=2048 for MySQL. "
            "Connection validation: connectionTestQuery for legacy drivers; testOnBorrow=true (overhead). "
            "Prefer keepaliveTime to avoid idle connection timeout from DB/network."
        ),
        solutions=[
            "Start with maximumPoolSize=10; monitor pool metrics before increasing",
            "Set minimumIdle=maximumPoolSize to keep pool stable (no growth/shrink overhead)",
            "MySQL optimization: cachePrepStmts=true&useServerPrepStmts=true in JDBC URL",
            "Set connectionTimeout=5000ms (not default 30s) for fast failure detection",
            "Monitor: hikaricp.connections.pending > 0 means pool is undersized",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=78,
        context={
            "metric": "hikaricp.connections.pending in Prometheus/Actuator — pool too small if > 0",
            "cloud_sql": "Cloud SQL max_connections varies by instance tier; check before setting pool size",
        },
    ),
    "improvement_performance_testing": _learning(
        type_="IMPROVEMENT",
        category="PERFORMANCE",
        content=(
            "Performance testing strategy before production release: "
            "Load test: simulate expected peak traffic and verify SLOs are met. "
            "Stress test: increase load until system breaks — find the breaking point. "
            "Soak test: run load for 24h — find memory leaks and connection pool exhaustion. "
            "Spike test: sudden 10x load — verify auto-scaling and graceful degradation. "
            "Tools: k6 (JavaScript, cloud-native), Gatling (Scala, CI integration), "
            "Apache JMeter (GUI, Java)."
        ),
        solutions=[
            "Write k6 scripts for critical user flows: login, create order, search products",
            "Run load tests in CI on every major release against staging environment",
            "Set performance regression gate: fail CI if P95 latency increases > 20%",
            "Use k6 Cloud or Gatling Enterprise for distributed load from multiple regions",
            "Profile during load test to find bottlenecks: JFR + async-profiler under load",
        ],
        severity="HIGH",
        confidence=0.93,
        times_applied=44,
        context={
            "k6_script": "k6 run --vus 100 --duration 5m script.js  # 100 virtual users, 5 min",
            "tool": "k6 (open source, Grafana-owned), Gatling, Apache JMeter",
        },
    ),
}

# ============================================================================
# PERFORMANCE_KNOWLEDGE rich topic documents
# ============================================================================

PERFORMANCE_KNOWLEDGE_DOCS = {
    "caching_patterns_guide": {
        "topic": "Caching — Complete Strategy Guide",
        "category": "CACHING",
        "description": "Multi-level caching patterns for maximum application performance.",
        "cache_levels": {
            "L1_browser": {
                "type": "Browser cache",
                "latency": "Instant (0ms)",
                "control": "Cache-Control, ETag, Last-Modified headers",
                "use_for": "Static assets, public API responses",
            },
            "L2_cdn": {
                "type": "CDN (Firebase Hosting, Cloudflare, Cloud CDN)",
                "latency": "1-10ms (nearest PoP)",
                "control": "Cache-Control on responses; purge via API",
                "use_for": "Public static and semi-dynamic content",
            },
            "L3_application": {
                "type": "In-process (Caffeine) or distributed (Redis)",
                "latency": "Nanoseconds (Caffeine) to microseconds (Redis)",
                "control": "@Cacheable TTL, eviction policy",
                "use_for": "Frequently-read DB data, computed results, session data",
            },
            "L4_database": {
                "type": "DB query cache / read replica",
                "latency": "1-50ms",
                "control": "Query optimisation, indexes, read replicas",
                "use_for": "All persistent data",
            },
        },
        "spring_cache_config": (
            "@Configuration @EnableCaching\n"
            "public class CacheConfig {\n"
            "    @Bean CacheManager cacheManager() {\n"
            "        // L1: Caffeine (local)\n"
            "        CaffeineCacheManager mgr = new CaffeineCacheManager();\n"
            "        mgr.setCaffeine(Caffeine.newBuilder()\n"
            "            .maximumSize(500)\n"
            "            .expireAfterWrite(5, TimeUnit.MINUTES)\n"
            "            .recordStats());\n"
            "        return mgr;\n"
            "    }\n"
            "}"
        ),
        "eviction_policies": {
            "LRU": "Least Recently Used — evict oldest accessed entry",
            "LFU": "Least Frequently Used — evict least accessed entry",
            "FIFO": "First In First Out — evict oldest entry regardless of access",
            "TTL": "Time To Live — evict after fixed duration",
            "TTI": "Time To Idle — evict if not accessed for duration",
        },
        "cache_stampede_prevention": (
            "Cache stampede (dogpile): all requests rush to DB when popular cache entry expires. "
            "Solutions: (1) Stale-while-revalidate: serve stale data while updating async. "
            "(2) Probabilistic early expiration: probabilistically refresh before expiry. "
            "(3) Distributed lock: only one request regenerates; others wait or get stale."
        ),
        "confidence": 0.95,
    },
    "jvm_performance_guide": {
        "topic": "JVM Performance — Tuning and Analysis",
        "category": "JVM",
        "description": "JVM internals and tuning for production Java/Spring Boot applications.",
        "garbage_collectors": {
            "G1GC": {
                "flag": "-XX:+UseG1GC (default Java 9+)",
                "heap_size": "4GB - 32GB",
                "pause_target": "-XX:MaxGCPauseMillis=200 (target, not guarantee)",
                "use_when": "Most production Spring Boot applications",
            },
            "ZGC": {
                "flag": "-XX:+UseZGC",
                "heap_size": "8GB+ (shines with large heaps)",
                "pause": "Sub-millisecond regardless of heap size",
                "use_when": "Latency-critical services; large heap; Java 15+",
            },
            "Shenandoah": {
                "flag": "-XX:+UseShenandoahGC",
                "pause": "Sub-millisecond; concurrent evacuation",
                "use_when": "OpenJDK 12+; similar use case to ZGC",
            },
            "SerialGC": {
                "flag": "-XX:+UseSerialGC",
                "use_when": "Single-core, very small heap (CLI tools, batch jobs)",
            },
        },
        "memory_areas": {
            "Heap": "Young gen (Eden + S0/S1) + Old gen; controlled by -Xms/-Xmx",
            "Metaspace": "Class metadata; was PermGen pre-Java 8; -XX:MaxMetaspaceSize=256m",
            "Stack": "Per-thread method frames; -Xss=512k (default 1m) for high thread count",
            "DirectBuffer": "Off-heap NIO buffers; -XX:MaxDirectMemorySize=512m",
        },
        "jvm_flags_cloud_run": [
            "-XX:+UseContainerSupport  # Respect container limits",
            "-XX:MaxRAMPercentage=75.0  # Use 75% of container memory for heap",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=200",
            "-Xss=512k  # Reduce stack size for higher thread count",
            "-XX:+ExitOnOutOfMemoryError  # Crash fast; let orchestrator restart",
        ],
        "profiling_tools": {
            "JFR": "Built-in Java 11+; < 1% overhead; records everything",
            "async-profiler": "Open source; no safepoint bias; CPU + alloc + lock",
            "VisualVM": "GUI; connects remotely via JMX",
            "Eclipse_MAT": "Heap dump analyser; find memory leaks",
        },
        "confidence": 0.93,
    },
    "api_performance_guide": {
        "topic": "API Performance — Design for Speed",
        "category": "API_PERFORMANCE",
        "description": "Designing and implementing high-performance REST APIs.",
        "techniques": {
            "pagination": {
                "cursor_based": "WHERE id > :cursor LIMIT 20 — O(1) regardless of offset",
                "offset_max": "Cap OFFSET at 1000; force cursor pagination beyond that",
            },
            "field_selection": {
                "description": "Let clients request only needed fields — reduces payload size",
                "rest": "GET /api/users?fields=id,name,email",
                "graphql": "Native field selection in every GraphQL query",
            },
            "compression": {
                "gzip": "60-80% JSON payload reduction; enable server.compression.enabled=true",
                "brotli": "15-25% better than gzip; enable at nginx/CDN layer",
            },
            "connection_keep_alive": {
                "http_11": "Keep-Alive by default — reuse TCP connection",
                "http_2": "Multiplexing — multiple requests over one connection",
                "spring": "HTTP/2 supported in Spring Boot 2.2+ with embedded Tomcat",
            },
            "caching_headers": {
                "ETag": "Fingerprint of response; client sends If-None-Match for 304 check",
                "Last_Modified": "Response timestamp; client sends If-Modified-Since",
                "Cache_Control": "max-age, no-cache, no-store, private, public",
            },
            "batch_endpoints": {
                "description": "Single endpoint to fetch multiple resources in one request",
                "example": "GET /api/users?ids=1,2,3 instead of 3 separate requests",
                "graphql": "GraphQL DataLoader handles batch loading natively",
            },
        },
        "response_time_targets": {
            "database_read": "< 10ms",
            "in_memory_cache": "< 1ms",
            "external_api_call": "< 200ms with timeout",
            "complex_computation": "< 500ms; consider async if longer",
            "file_upload": "Streaming — don't buffer entire file in memory",
        },
        "spring_async_controller": (
            "@GetMapping('/api/data')\n"
            "public CompletableFuture<ResponseEntity<Data>> getData() {\n"
            "    return CompletableFuture.supplyAsync(() -> {\n"
            "        Data data = dataService.fetchData(); // runs in async pool\n"
            "        return ResponseEntity.ok(data);\n"
            "    }, asyncExecutor);\n"
            "}"
        ),
        "confidence": 0.94,
    },
    "react_performance_guide": {
        "topic": "React Performance — Rendering Optimisation",
        "category": "FRONTEND_PERFORMANCE",
        "description": "Optimising React applications for fast rendering and user experience.",
        "rendering_optimisation": {
            "React_memo": "Wrap component: export default React.memo(MyComponent) — skips re-render if props unchanged",
            "useMemo": "Memoize expensive value: const sorted = useMemo(() => sort(data), [data])",
            "useCallback": "Stable function ref: const fn = useCallback(() => doThing(id), [id])",
            "key_prop": "Use stable, unique key on list items — not array index",
            "state_colocation": "Keep state close to where it's used — prevents unnecessary parent re-renders",
            "context_splitting": "Split large Context into smaller pieces — consumers only re-render on relevant change",
        },
        "code_splitting": {
            "route_level": "const Dashboard = React.lazy(() => import('./pages/Dashboard'))",
            "component_level": "Lazy load heavy components (charts, editors) on user interaction",
            "bundle_analysis": "npx vite-bundle-analyzer or npx source-map-explorer build/static/js/*.js",
        },
        "virtualisation": {
            "react_window": "FixedSizeList for uniform rows; VariableSizeList for variable height",
            "react_virtual": "TanStack Virtual — more flexible, works with any layout",
            "use_when": "Lists > 100 items; tables > 50 rows",
        },
        "image_optimisation": [
            "Use WebP format (30% smaller than JPEG at same quality)",
            "Lazy load images: <img loading='lazy' />",
            "Responsive images: srcset for different viewport widths",
            "Use Next.js Image component or similar for automatic optimisation",
        ],
        "web_vitals_targets": {
            "LCP": "Largest Contentful Paint < 2.5s",
            "FID": "First Input Delay < 100ms (now INP: Interaction to Next Paint < 200ms)",
            "CLS": "Cumulative Layout Shift < 0.1",
            "TTFB": "Time to First Byte < 800ms",
            "FCP": "First Contentful Paint < 1.8s",
        },
        "confidence": 0.93,
    },
    "load_testing_guide": {
        "topic": "Load Testing — Tools and Strategies",
        "category": "PERFORMANCE_TESTING",
        "description": "Testing application performance under load before production release.",
        "test_types": {
            "Load_Test": "Simulate expected peak traffic; verify SLOs are met",
            "Stress_Test": "Increase load past expected peak; find breaking point",
            "Soak_Test": "Normal load for 24h; find memory leaks, connection exhaustion",
            "Spike_Test": "Sudden 10x load increase; verify scaling and graceful degradation",
            "Smoke_Test": "Minimal load just to verify the system is working",
        },
        "k6_example": (
            "import http from 'k6/http';\n"
            "import { sleep, check } from 'k6';\n\n"
            "export const options = {\n"
            "  stages: [\n"
            "    { duration: '2m', target: 50 },   // ramp up\n"
            "    { duration: '5m', target: 100 },  // peak load\n"
            "    { duration: '2m', target: 0 },    // ramp down\n"
            "  ],\n"
            "  thresholds: {\n"
            "    http_req_duration: ['p(95)<500'],  // 95% requests < 500ms\n"
            "    http_req_failed: ['rate<0.01'],    // < 1% error rate\n"
            "  },\n"
            "};\n\n"
            "export default function () {\n"
            "  const res = http.get('https://supremeai-lhlwyikwlq-uc.a.run.app/api/health');\n"
            "  check(res, { 'status 200': (r) => r.status === 200 });\n"
            "  sleep(1);\n"
            "}"
        ),
        "tools": {
            "k6": "Open source, JavaScript API, cloud/local, excellent Grafana integration",
            "Gatling": "Scala-based, CI-first, HTML reports, enterprise features",
            "JMeter": "GUI-based, Java, extensive plugins, widely known",
            "Locust": "Python-based, good for complex scenarios",
            "Artillery": "Node.js-based, YAML config, good for event-driven testing",
        },
        "analysis_checklist": [
            "P50, P95, P99 latency — not just average",
            "Error rate by endpoint and error type",
            "JVM heap growth over time (soak test)",
            "DB connection pool utilisation",
            "Pod/instance count during scale-out",
            "GC pause frequency and duration under load",
        ],
        "confidence": 0.93,
    },
}

# ============================================================================
# ENTRY POINT
# ============================================================================

if __name__ == "__main__":
    run_part(
        part_name="Part 6 — Performance Optimisation",
        collections={
            "system_learning": SYSTEM_LEARNINGS,
            "performance_knowledge": PERFORMANCE_KNOWLEDGE_DOCS,
        },
    )
