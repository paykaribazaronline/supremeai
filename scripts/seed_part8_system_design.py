#!/usr/bin/env python3
"""
Part 8 — System Design
Seeds SupremeAI Firebase with deep knowledge about:
  • Distributed systems fundamentals (CAP theorem, consistency models)
  • Scalability patterns (horizontal/vertical, stateless design, sharding)
  • Message queues and event streaming (Kafka, Pub/Sub, RabbitMQ patterns)
  • Event Sourcing and CQRS
  • API design (REST, GraphQL, gRPC, WebSockets)
  • Resilience patterns (retry, circuit breaker, bulkhead, timeout)
  • System design case studies (URL shortener, notification system, feed)

Collections written:
  • system_learning      (SystemLearning model records)
  • system_design_knowledge (rich topic documents)

Run:
  pip install firebase-admin
  python seed_part8_system_design.py [--dry-run]
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from seed_lib import _learning, run_part

# ============================================================================
# SYSTEM_LEARNING records
# ============================================================================

SYSTEM_LEARNINGS = {

    "sysdesign_cap_theorem": _learning(
        type_="PATTERN",
        category="DISTRIBUTED_SYSTEMS",
        content=(
            "CAP Theorem (Brewer's theorem): in a distributed system, you can only guarantee "
            "2 of 3 properties simultaneously: "
            "C — Consistency: every read receives the most recent write or an error. "
            "A — Availability: every request receives a non-error response (may be stale). "
            "P — Partition tolerance: system continues despite network partitions. "
            "In practice: network partitions WILL happen → must choose between C and A. "
            "CP systems: MongoDB, HBase, Zookeeper — sacrifice availability for consistency. "
            "AP systems: Cassandra, CouchDB, DynamoDB — sacrifice consistency for availability. "
            "CA is impossible in a real distributed system."
        ),
        solutions=[
            "Choose CP for: financial transactions, inventory management, order processing",
            "Choose AP for: social feeds, product catalog, recommendation engines",
            "Use eventual consistency + conflict resolution for AP systems",
            "Add read-your-writes consistency: sticky sessions or version tokens",
            "PACELC extends CAP: also considers latency/consistency trade-off when no partition",
        ],
        severity="HIGH",
        confidence=0.97,
        times_applied=89,
        context={
            "coined_by": "Eric Brewer, 2000; proven by Seth Gilbert and Nancy Lynch, 2002",
            "firestore": "Firestore is CP — strong consistency within a region",
        },
    ),

    "sysdesign_horizontal_scaling": _learning(
        type_="PATTERN",
        category="SCALABILITY",
        content=(
            "Horizontal scaling (scale-out) vs vertical scaling (scale-up): "
            "Vertical: add more CPU/RAM to existing server — simple but limited (scale ceiling) and single point of failure. "
            "Horizontal: add more servers — unlimited scale but requires stateless design. "
            "Stateless design principles: no in-memory session; store session in Redis; "
            "no local file storage — use Cloud Storage; no local cache — use Redis. "
            "Any instance should be able to handle any request from any user. "
            "Cloud Run and Kubernetes scale horizontally automatically."
        ),
        solutions=[
            "Move all session state to Redis before enabling multiple instances",
            "Move all file storage to GCS/S3 — never write to local disk in horizontally-scaled services",
            "Use distributed cache (Redis) not local cache (Caffeine) for shared data",
            "Make all service operations idempotent — retry safe when load balancer retries",
            "Test with 3+ instances in staging to catch stateful bugs before production",
        ],
        severity="HIGH",
        confidence=0.96,
        times_applied=112,
        context={
            "cloud_run": "Cloud Run scales to 0-1000 instances automatically — must be stateless",
            "session_tip": "spring-session-data-redis moves HttpSession to Redis in one dependency + @Bean",
        },
    ),

    "sysdesign_consistent_hashing": _learning(
        type_="PATTERN",
        category="DISTRIBUTED_SYSTEMS",
        content=(
            "Consistent hashing: distribute data/load across nodes such that when nodes are added/removed, "
            "only K/n keys are remapped (K=keys, n=nodes). "
            "Use case: distributing cache data across Redis cluster nodes; "
            "routing requests to backend servers; sharding Cassandra/DynamoDB. "
            "Virtual nodes: each physical node gets multiple positions on the hash ring "
            "to improve load distribution (Cassandra uses 256 vnodes per node by default). "
            "Implementation: murmur3 hash function on key → position on ring → find next clockwise node."
        ),
        solutions=[
            "Use consistent hashing for Redis Cluster — client libraries handle it automatically",
            "For custom sharding: use Rendezvous hashing or Jump hashing as simpler alternatives",
            "DynamoDB and Cassandra use consistent hashing internally — no manual implementation needed",
            "When adding nodes: only move data from next clockwise node, not full reshard",
            "Add virtual nodes to prevent hotspots when nodes have unequal capacity",
        ],
        severity="HIGH",
        confidence=0.93,
        times_applied=44,
        context={
            "libraries": "Java: Guava ConsistentHashFunction; Jedis/Lettuce for Redis Cluster",
            "alternatives": "Rendezvous hashing (simpler, no ring), Jump hashing (fast, bucket-based)",
        },
    ),

    "sysdesign_message_queue_patterns": _learning(
        type_="PATTERN",
        category="MESSAGING",
        content=(
            "Message queue patterns: "
            "Point-to-Point: one producer → one consumer; task queue; job distribution. "
            "Publish-Subscribe: one producer → many consumers; event notification. "
            "Competing Consumers: multiple workers read from same queue — parallel processing. "
            "Dead Letter Queue (DLQ): failed messages move to DLQ after N retries — "
            "prevents poison messages from blocking queue. "
            "Priority Queue: high-priority messages processed first (RabbitMQ priority queue). "
            "Message replay: Kafka retains messages — replay from any offset for recovery."
        ),
        solutions=[
            "Use DLQ for every queue — without DLQ a single bad message can halt processing",
            "Set message TTL to prevent queue bloat from unprocessed messages",
            "Make consumers idempotent — at-least-once delivery means duplicate messages",
            "Use message deduplication ID (Kafka key, SQS deduplication ID) for exactly-once",
            "Monitor: queue depth, consumer lag, DLQ message count — alert on growing DLQ",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=78,
        context={
            "kafka_vs_rabbitmq": "Kafka: durable log, replay, high throughput; RabbitMQ: routing, priorities, lower throughput",
            "spring": "Spring Kafka @KafkaListener; Spring AMQP @RabbitListener",
        },
    ),

    "sysdesign_api_design_rest": _learning(
        type_="PATTERN",
        category="API_DESIGN",
        content=(
            "REST API design best practices: "
            "Nouns not verbs: /api/users not /api/getUsers. "
            "HTTP verbs: GET (read), POST (create), PUT (full update), PATCH (partial update), DELETE. "
            "Status codes: 200 OK, 201 Created, 204 No Content, 400 Bad Request, "
            "401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict, 422 Unprocessable, 429 Rate Limited, 500 Server Error. "
            "Versioning: /api/v1/users or Accept: application/vnd.api.v1+json. "
            "HATEOAS: include links in response for discoverability (Spring HATEOAS). "
            "Pagination: ?page=0&size=20 or ?after=<cursor>&limit=20."
        ),
        solutions=[
            "Use consistent naming: plural nouns (/users, /orders), lowercase, kebab-case",
            "Return Problem Details (RFC 7807) for errors: {type, title, status, detail, instance}",
            "Version APIs before breaking changes — never break existing clients",
            "Use ETag + conditional requests for safe caching of mutable resources",
            "Document with OpenAPI 3.0: springdoc-openapi generates from annotations",
        ],
        severity="HIGH",
        confidence=0.97,
        times_applied=201,
        context={
            "spring_springdoc": "springdoc-openapi-starter-webmvc-ui generates /swagger-ui.html automatically",
            "problem_details": "Spring Boot 3: ProblemDetail class built-in for RFC 7807 error responses",
        },
    ),

    "sysdesign_graphql": _learning(
        type_="PATTERN",
        category="API_DESIGN",
        content=(
            "GraphQL: query language for APIs — client specifies exactly what data it needs. "
            "Benefits over REST: no over-fetching, no under-fetching (N+1 avoided with DataLoader), "
            "single endpoint, strongly typed schema, introspection. "
            "Schema-first: define types and queries in .graphql files; generate resolvers. "
            "N+1 in GraphQL: solved with DataLoader — batches and caches field resolvers. "
            "Spring for GraphQL (Spring Boot 3.2): @QueryMapping, @MutationMapping, @SchemaMapping. "
            "Use when: complex data requirements, multiple frontends with different data needs, mobile with bandwidth concerns."
        ),
        solutions=[
            "Use DataLoader for every 1:many relationship to prevent N+1 queries",
            "Implement query complexity limits to prevent expensive/malicious queries",
            "Add field-level authorisation: use @SchemaMapping with security context",
            "Use persisted queries in production — only execute pre-registered queries",
            "Paginate with Relay cursor connection spec for consistent pagination across clients",
        ],
        severity="MEDIUM",
        confidence=0.91,
        times_applied=38,
        context={
            "spring": "@Controller + @QueryMapping + @SchemaMapping (Spring for GraphQL)",
            "when_not_to": "Simple CRUD APIs with known fixed clients — REST is simpler",
        },
    ),

    "sysdesign_grpc": _learning(
        type_="PATTERN",
        category="API_DESIGN",
        content=(
            "gRPC: high-performance RPC framework using HTTP/2 and Protocol Buffers. "
            "Benefits: 3-10x faster serialisation than JSON, bidirectional streaming, "
            "strongly-typed contracts via .proto files, code generation for all languages. "
            "Service types: unary (request-response), server streaming, client streaming, bidirectional streaming. "
            "Use when: microservice-to-microservice communication, real-time streaming, "
            "polyglot services, strict performance requirements. "
            "Spring Boot: grpc-spring-boot-starter adds @GrpcService annotation."
        ),
        solutions=[
            "Define service contract in .proto file first; generate Java stubs with protoc",
            "Use gRPC for internal service communication; REST/GraphQL for external client APIs",
            "Add gRPC interceptors for: logging, authentication, metrics, tracing",
            "Use gRPC-Gateway to expose gRPC as REST for clients that can't use gRPC",
            "Enable gRPC reflection in dev for grpcurl debugging; disable in production",
        ],
        severity="MEDIUM",
        confidence=0.90,
        times_applied=29,
        context={
            "vs_rest": "gRPC: 3-10x faster, streaming, bi-directional; REST: simpler, browser-friendly",
            "spring_lib": "net.devh:grpc-spring-boot-starter for Spring Boot gRPC server",
        },
    ),

    "sysdesign_event_sourcing": _learning(
        type_="PATTERN",
        category="ARCHITECTURE",
        content=(
            "Event Sourcing: store ALL changes to application state as a sequence of events "
            "instead of current state. The current state is derived by replaying events. "
            "Benefits: full audit log (GDPR-compliant history), temporal queries (state at any point in time), "
            "event replay for new projections, natural fit for CQRS. "
            "Challenges: event schema evolution, eventual consistency for queries, "
            "snapshot needed for entities with thousands of events. "
            "Event store: append-only; events are immutable facts. "
            "Frameworks: Axon Framework (Java), EventStoreDB, Marten (.NET)."
        ),
        solutions=[
            "Store events as: {eventId, aggregateId, eventType, payload, timestamp, version}",
            "Use snapshots every N events to avoid replaying thousands of events on load",
            "Version event schemas: add new optional fields; never remove or rename fields",
            "Combine with CQRS: event side = source of truth; read side = denormalised projections",
            "Test with event replay: verify aggregate state from a known sequence of events",
        ],
        severity="HIGH",
        confidence=0.90,
        times_applied=31,
        context={
            "use_when": "Audit requirements, financial systems, complex domain with multiple projections",
            "frameworks": "Axon Framework (Java), EventStoreDB, Marten (.NET), Eventuate",
        },
    ),

    "sysdesign_resilience4j": _learning(
        type_="PATTERN",
        category="RESILIENCE",
        content=(
            "Resilience4j patterns for fault-tolerant microservices: "
            "Circuit Breaker: CLOSED → OPEN after N failures → HALF_OPEN after wait → back to CLOSED. "
            "Rate Limiter: limit calls to external API to prevent overwhelming it. "
            "Retry: retry failed calls N times with exponential backoff + jitter. "
            "Bulkhead: limit concurrent calls to external service (semaphore) — prevents one "
            "slow dependency from consuming all threads. "
            "Time Limiter: timeout for calls that hang — prevent resource exhaustion. "
            "Combining: Rate Limiter → Circuit Breaker → Retry → Time Limiter → Bulkhead → actual call."
        ),
        solutions=[
            "Add spring-boot-starter-aop + resilience4j-spring-boot3 for annotation support",
            "@CircuitBreaker(name='paymentService', fallbackMethod='paymentFallback')",
            "@Retry(name='externalApi', fallbackMethod='fallback')",
            "@Bulkhead(name='inventory', type=SEMAPHORE) to limit concurrent inventory calls",
            "Configure via application.yml: resilience4j.circuitbreaker.instances.paymentService.*",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=89,
        context={
            "library": "io.github.resilience4j:resilience4j-spring-boot3",
            "order": "Rate Limiter → Circuit Breaker → Retry → Time Limiter → Bulkhead",
        },
    ),

    "sysdesign_database_sharding": _learning(
        type_="PATTERN",
        category="SCALABILITY",
        content=(
            "Database sharding: partition data across multiple database instances. "
            "Horizontal sharding: split rows across shards based on shard key (user_id % N). "
            "Vertical sharding: split tables across databases by domain. "
            "Shard key selection: must distribute data evenly; avoid hotspots; "
            "support common query patterns without cross-shard joins. "
            "Cross-shard queries: impossible without scatter-gather (expensive) — "
            "design queries to stay within one shard. "
            "Resharding: moving data when adding shards — use consistent hashing to minimise moves."
        ),
        solutions=[
            "Try read replicas and caching before sharding — sharding adds enormous complexity",
            "Choose shard key based on access patterns: user_id for user data, tenant_id for multi-tenant",
            "Use middleware for transparent sharding: Vitess (MySQL), Citus (PostgreSQL)",
            "Avoid cross-shard transactions — design to keep related data in same shard",
            "For multi-tenant SaaS: shard by tenant_id — all tenant data stays in one shard",
        ],
        severity="HIGH",
        confidence=0.91,
        times_applied=29,
        context={
            "alternatives": "Try: read replicas, caching, table partitioning, NoSQL migration before sharding",
            "tools": "Vitess (YouTube's MySQL sharding layer), Citus (Postgres), Amazon Aurora sharding",
        },
    ),

    "sysdesign_idempotency": _learning(
        type_="PATTERN",
        category="DISTRIBUTED_SYSTEMS",
        content=(
            "Idempotency: an operation that produces the same result if executed multiple times. "
            "Critical for: retry logic, message queue at-least-once delivery, "
            "payment processing, order creation. "
            "Implementation: "
            "(1) Idempotency Key: client sends unique UUID per operation; "
            "server checks if key was already processed and returns cached result. "
            "(2) Conditional update: UPDATE only if current version matches expected version. "
            "(3) Natural idempotency: PUT is naturally idempotent (full replacement); GET/HEAD/DELETE too. "
            "(4) Check-then-act: read current state; only proceed if state allows the operation."
        ),
        solutions=[
            "Accept Idempotency-Key header on POST/PATCH endpoints; cache result by key for 24h",
            "Store (idempotency_key, result) in Redis with TTL — return cached result on retry",
            "Make Kafka consumer idempotent: check if event_id already processed in DB",
            "Database UPSERT (INSERT ... ON CONFLICT DO UPDATE) for naturally idempotent writes",
            "Use @Version optimistic locking to prevent duplicate concurrent writes",
        ],
        severity="CRITICAL",
        confidence=0.95,
        times_applied=67,
        context={
            "payment": "Always implement idempotency for payment APIs — duplicate charges are catastrophic",
            "stripe": "Stripe Idempotency-Key header is the industry standard pattern",
        },
    ),

    "sysdesign_websocket_realtime": _learning(
        type_="PATTERN",
        category="REAL_TIME",
        content=(
            "Real-time communication options for web/mobile: "
            "WebSocket: full-duplex persistent connection — best for chat, live collaboration, gaming. "
            "Server-Sent Events (SSE): server→client only; automatic reconnect; HTTP-based; "
            "good for live dashboards, notifications. "
            "Long Polling: client polls; server holds request until event — simpler than WebSocket "
            "but less efficient. "
            "Spring WebSocket: @EnableWebSocketMessageBroker with STOMP protocol. "
            "Firebase Realtime DB / Firestore: managed WebSocket with offline support — "
            "recommended for SupremeAI mobile apps."
        ),
        solutions=[
            "Use Firebase Firestore real-time listeners for SupremeAI — no custom WebSocket infra",
            "For chat: Spring WebSocket + STOMP + SockJS for browser fallback",
            "For notifications: SSE with @GetMapping(produces=MediaType.TEXT_EVENT_STREAM_VALUE)",
            "Scale WebSocket with sticky sessions (same user → same server) or shared pub/sub (Redis)",
            "Implement heartbeat: server sends ping every 25s; client disconnects if no pong in 60s",
        ],
        severity="HIGH",
        confidence=0.93,
        times_applied=56,
        context={
            "firebase": "Firestore onSnapshot() listener = managed WebSocket with auto-reconnect and offline cache",
            "spring_websocket": "@MessageMapping + @SendTo for STOMP message routing",
        },
    ),
}

# ============================================================================
# SYSTEM_DESIGN_KNOWLEDGE rich topic documents
# ============================================================================

SYSTEM_DESIGN_KNOWLEDGE_DOCS = {

    "distributed_systems_guide": {
        "topic": "Distributed Systems — Core Concepts",
        "category": "DISTRIBUTED_SYSTEMS",
        "description": "Fundamental concepts for building reliable distributed systems.",
        "key_concepts": {
            "CAP_Theorem": {
                "consistency": "All nodes see same data at same time",
                "availability": "Every request gets a response",
                "partition_tolerance": "System works despite network failures",
                "truth": "Choose C or A when partition occurs; P is always required in practice",
            },
            "PACELC": {
                "description": "Extends CAP: when no Partition, choose between latency (L) and consistency (C)",
                "example": "DynamoDB: AP (partition), LC (no partition) — eventual consistency for low latency",
            },
            "Consistency_Models": {
                "strong": "Read always returns latest write — PostgreSQL, Firestore within region",
                "eventual": "Reads eventually see all writes — DynamoDB, Cassandra, DNS",
                "causal": "Causally related reads see correct order — MongoDB sessions",
                "read_your_writes": "User always sees their own writes — achieved with sticky session or version token",
            },
            "Fallacies_of_Distributed_Computing": [
                "The network is reliable",
                "Latency is zero",
                "Bandwidth is infinite",
                "The network is secure",
                "Topology doesn't change",
                "There is one administrator",
                "Transport cost is zero",
                "The network is homogeneous",
            ],
        },
        "failure_modes": {
            "crash_fault": "Node stops — detectable (no response)",
            "byzantine_fault": "Node behaves arbitrarily (malicious/buggy) — hardest to handle",
            "network_partition": "Two groups of nodes cannot communicate",
            "split_brain": "Two nodes believe they are the leader — solved with leader election (Raft, Paxos)",
        },
        "tools": {
            "consensus": "Raft (etcd, CockroachDB), Paxos, Zab (Zookeeper)",
            "service_discovery": "Consul, etcd, Kubernetes DNS, Eureka",
            "distributed_lock": "Redisson (Redis), Zookeeper, etcd",
        },
        "confidence": 0.95,
    },

    "api_design_guide": {
        "topic": "API Design — REST, GraphQL, gRPC",
        "category": "API_DESIGN",
        "description": "Choosing and implementing the right API style for your use case.",
        "comparison": {
            "REST": {
                "best_for": "External APIs, browser clients, simple CRUD",
                "pros": ["Simple", "Cacheable", "HTTP-native", "Universal tooling"],
                "cons": ["Over/under fetching", "Multiple round trips", "Versioning complexity"],
                "spring": "@RestController + @RequestMapping + ResponseEntity",
            },
            "GraphQL": {
                "best_for": "Complex data requirements, multiple clients, mobile bandwidth optimization",
                "pros": ["Client-defined queries", "No over-fetching", "Strong typing", "Introspection"],
                "cons": ["N+1 without DataLoader", "Caching complex", "Learning curve", "Query complexity attacks"],
                "spring": "Spring for GraphQL: @QueryMapping, @MutationMapping",
            },
            "gRPC": {
                "best_for": "Internal microservice communication, streaming, performance-critical",
                "pros": ["10x faster than REST JSON", "Bidirectional streaming", "Code generation", "Strong typing"],
                "cons": ["Browser unfriendly", "Binary format (debugging harder)", "Less ecosystem tooling"],
                "spring": "grpc-spring-boot-starter + @GrpcService",
            },
            "WebSocket": {
                "best_for": "Real-time bidirectional communication (chat, gaming, collaboration)",
                "pros": ["Full-duplex", "Low latency", "Persistent connection"],
                "cons": ["Complex server scaling", "Connection management", "No HTTP caching"],
                "spring": "@EnableWebSocketMessageBroker + STOMP",
            },
        },
        "rest_design_rules": {
            "urls": "Nouns: /users, /orders/{id}/items — never verbs",
            "methods": "GET=read, POST=create, PUT=replace, PATCH=update, DELETE=delete",
            "status_codes": "201 Created (POST), 200 OK (GET/PUT/PATCH), 204 No Content (DELETE), 404 Not Found, 400 Bad Request, 401 Unauth, 403 Forbidden, 409 Conflict",
            "versioning": "/api/v1/ prefix or Accept-Version header",
            "pagination": "?page=0&size=20 or ?after=cursor&limit=20",
            "filtering": "?status=ACTIVE&type=ADMIN",
            "sorting": "?sort=createdAt,desc",
            "error_format": "RFC 7807 Problem Details: {type, title, status, detail, instance}",
        },
        "confidence": 0.96,
    },

    "scalability_patterns_guide": {
        "topic": "Scalability Patterns — Design for Growth",
        "category": "SCALABILITY",
        "description": "Patterns for building systems that scale from 1,000 to 1,000,000,000 users.",
        "scaling_stages": {
            "stage_1_single_server": {
                "users": "0-10k",
                "setup": "Single server: app + DB + file storage",
                "bottleneck": "Single point of failure; DB becomes bottleneck first",
            },
            "stage_2_separate_db": {
                "users": "10k-100k",
                "setup": "App server(s) → separate DB server",
                "additions": "CDN for static assets, load balancer if multiple app servers",
            },
            "stage_3_cache_layer": {
                "users": "100k-1M",
                "setup": "Load balancer → app servers → Redis → DB",
                "additions": "Redis cache for hot data, DB read replicas",
            },
            "stage_4_message_queue": {
                "users": "1M-10M",
                "setup": "Add message queue for async processing, separate worker nodes",
                "additions": "Kafka/RabbitMQ for async jobs, separate services by function",
            },
            "stage_5_microservices": {
                "users": "10M+",
                "setup": "Full microservices, service mesh, database per service",
                "additions": "Service mesh (Istio), API gateway, distributed tracing",
            },
        },
        "stateless_design": [
            "No in-memory session state — use Redis",
            "No local disk writes — use Cloud Storage (GCS, S3)",
            "No local cache — use Redis (shared)",
            "Environment configuration via environment variables (12-factor app)",
            "Any instance can handle any request — enables auto-scaling",
        ],
        "database_scaling_progression": [
            "1. Add indexes and optimise queries",
            "2. Add read replicas for read-heavy workloads",
            "3. Add Redis cache layer",
            "4. Table partitioning (PostgreSQL native partitioning)",
            "5. Vertical scaling (larger DB instance)",
            "6. Sharding (last resort — massive complexity)",
        ],
        "confidence": 0.94,
    },

    "resilience_patterns_guide": {
        "topic": "Resilience Patterns — Building Fault-Tolerant Systems",
        "category": "RESILIENCE",
        "description": "Patterns to make distributed systems resilient to failures.",
        "patterns": {
            "Retry": {
                "description": "Retry failed operations with delay",
                "config": "maxAttempts=3, waitDuration=500ms, exponential multiplier=2, maxWait=5s",
                "add_jitter": "±25% random jitter to prevent thundering herd",
                "when_not_to": "Non-idempotent operations (payment charge) unless idempotency key is used",
                "spring": "@Retry(name='externalApi', fallbackMethod='fallback')",
            },
            "Circuit_Breaker": {
                "description": "Stop calling failing service; return fallback immediately",
                "states": "CLOSED → (N failures) → OPEN → (wait) → HALF_OPEN → (probe success) → CLOSED",
                "config": "failureRateThreshold=50, waitDurationInOpenState=60s, permittedCallsInHalfOpen=3",
                "spring": "@CircuitBreaker(name='service', fallbackMethod='fallback')",
            },
            "Bulkhead": {
                "description": "Isolate failures — limit concurrent calls per dependency",
                "types": "Thread pool isolation (Hystrix) or Semaphore isolation (Resilience4j)",
                "benefit": "Slow dependency cannot consume all threads and starve other services",
                "spring": "@Bulkhead(name='inventory', type=SEMAPHORE, fallbackMethod='fallback')",
            },
            "Timeout": {
                "description": "Fail fast on slow calls — free threads for other requests",
                "config": "Set per-call timeout: 3s for external APIs, 30s for DB queries",
                "spring": "@TimeLimiter(name='service', fallbackMethod='fallback')",
            },
            "Fallback": {
                "description": "Return alternative value when primary fails",
                "types": "Default value, cached result, graceful degradation, error propagation",
                "example": "Return cached product list when inventory service is down",
            },
        },
        "resilience4j_config_example": (
            "# application.yml\n"
            "resilience4j:\n"
            "  circuitbreaker:\n"
            "    instances:\n"
            "      paymentService:\n"
            "        registerHealthIndicator: true\n"
            "        failureRateThreshold: 50\n"
            "        waitDurationInOpenState: 60s\n"
            "        permittedNumberOfCallsInHalfOpenState: 3\n"
            "  retry:\n"
            "    instances:\n"
            "      externalApi:\n"
            "        maxAttempts: 3\n"
            "        waitDuration: 500ms\n"
            "        enableExponentialBackoff: true\n"
            "        exponentialBackoffMultiplier: 2\n"
        ),
        "confidence": 0.95,
    },

    "system_design_case_studies": {
        "topic": "System Design Case Studies",
        "category": "SYSTEM_DESIGN",
        "description": "Step-by-step design of common large-scale systems.",
        "case_studies": {
            "URL_Shortener": {
                "requirements": "Create short URL, redirect to original, analytics",
                "scale": "100M URLs created/day, 10B redirects/day",
                "key_design": {
                    "id_generation": "Base62 encode of auto-increment ID or MD5 hash of URL",
                    "storage": "Key-value store: Redis (hot URLs) + MySQL (persistence)",
                    "redirect": "301 (permanent, browser caches) vs 302 (temporary, track each hit)",
                    "analytics": "Write click events to Kafka → batch process → aggregate in ClickHouse",
                },
            },
            "Notification_System": {
                "requirements": "Send push/email/SMS notifications reliably at scale",
                "scale": "100M notifications/day across channels",
                "key_design": {
                    "fanout": "Produce one message per user; workers fan out to channels",
                    "channels": "Push (FCM/APNs), Email (SendGrid), SMS (Twilio)",
                    "retry": "DLQ for failed deliveries; exponential backoff retry",
                    "dedup": "Idempotency key prevents duplicate notifications",
                    "priority": "Priority queue: critical alerts first",
                },
            },
            "Social_Feed": {
                "requirements": "Display ordered feed of posts from followed users",
                "scale": "100M users, 1M posts/day, 10M feed reads/min",
                "key_design": {
                    "fanout_on_write": "When post created, push to followers' feed caches — fast read",
                    "fanout_on_read": "Compute feed at read time from followed users' posts — low write cost",
                    "hybrid": "Fanout-on-write for normal users; fanout-on-read for celebrities (1M+ followers)",
                    "storage": "Redis sorted set per user: ZADD feed:{userId} {timestamp} {postId}",
                },
            },
            "Rate_Limiter": {
                "requirements": "Limit API calls per user/IP to prevent abuse",
                "algorithms": {
                    "token_bucket": "Tokens refill at fixed rate; burst allowed up to bucket size",
                    "sliding_window": "Count requests in rolling window — accurate, more memory",
                    "fixed_window": "Count per fixed time window — boundary burst vulnerability",
                },
                "implementation": "Redis INCR + EXPIRE per {user_id}:{window} key",
                "response": "429 Too Many Requests + Retry-After: {seconds} header",
            },
        },
        "design_interview_framework": [
            "1. Clarify requirements: functional, non-functional (scale, latency, consistency)",
            "2. Back-of-envelope estimation: QPS, storage, bandwidth",
            "3. High-level design: main components and data flow",
            "4. Deep dive: focus on 2-3 critical components",
            "5. Identify bottlenecks and solutions",
            "6. Discuss trade-offs explicitly",
        ],
        "confidence": 0.93,
    },

    "event_sourcing_cqrs_guide": {
        "topic": "Event Sourcing + CQRS — Complete Guide",
        "category": "ARCHITECTURE",
        "description": "Combining Event Sourcing and CQRS for complex, scalable domain systems.",
        "event_sourcing": {
            "core_idea": "Store events (what happened) not current state (what is)",
            "event_structure": {
                "eventId": "UUID v4",
                "aggregateId": "ID of the aggregate (orderId, userId)",
                "aggregateType": "Order, User, Payment",
                "eventType": "OrderPlaced, OrderShipped, PaymentFailed",
                "payload": "Event-specific data as JSON",
                "version": "Sequential version number for ordering",
                "timestamp": "When the event occurred",
                "metadata": "causationId, correlationId, userId",
            },
            "benefits": [
                "Complete audit log of all changes",
                "Temporal queries: state at any point in time",
                "Event replay to rebuild projections",
                "Natural CQRS fit",
                "Debugging: replay events to reproduce bugs",
            ],
            "challenges": [
                "Event schema evolution (must handle old event versions)",
                "Snapshot needed when events accumulate (>100 per aggregate)",
                "Eventual consistency for read projections",
                "Tooling and infrastructure more complex",
            ],
        },
        "cqrs": {
            "write_side": "Commands → CommandHandler → Aggregate → Domain Events → Event Store",
            "read_side": "Events → EventHandler → Read Model (projection) → Query",
            "benefits": [
                "Read and write models optimised independently",
                "Read model can be in different DB (Elasticsearch for search)",
                "Commands and queries scale independently",
                "Clear separation of concerns",
            ],
        },
        "axon_framework_example": (
            "// Command\n"
            "record PlaceOrderCommand(@TargetAggregateIdentifier String orderId, ...) {}\n\n"
            "// Aggregate\n"
            "@Aggregate class OrderAggregate {\n"
            "    @CommandHandler\n"
            "    public OrderAggregate(PlaceOrderCommand cmd) {\n"
            "        apply(new OrderPlacedEvent(cmd.orderId(), ...));\n"
            "    }\n"
            "    @EventSourcingHandler\n"
            "    void on(OrderPlacedEvent event) { this.status = OrderStatus.PLACED; }\n"
            "}\n\n"
            "// Projection\n"
            "@Component class OrderProjection {\n"
            "    @EventHandler\n"
            "    void on(OrderPlacedEvent event) {\n"
            "        orderReadRepo.save(new OrderReadModel(event.orderId(), ...));\n"
            "    }\n"
            "}"
        ),
        "confidence": 0.91,
    },
}

# ============================================================================
# ENTRY POINT
# ============================================================================

if __name__ == "__main__":
    run_part(
        part_name="Part 8 — System Design",
        collections={
            "system_learning": SYSTEM_LEARNINGS,
            "system_design_knowledge": SYSTEM_DESIGN_KNOWLEDGE_DOCS,
        },
    )
