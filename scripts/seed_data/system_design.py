"""
System Design, Scalability, Cloud Platforms, Message Queues, Observability
~30 learnings + ~8 patterns = ~38 documents
"""
from seed_data.helpers import _learning, _pattern

SYSTEM_DESIGN_LEARNINGS = {

    # ── Distributed Systems ────────────────────────────────────────────────
    "sd_cap_theorem": _learning(
        "PATTERN", "SYSTEM_DESIGN",
        "CAP theorem: Distributed system can guarantee at most 2 of 3: Consistency, Availability, "
        "Partition tolerance. CP (consistent but may reject requests): Zookeeper, etcd. "
        "AP (available but eventually consistent): Cassandra, DynamoDB. Partition tolerance is non-negotiable.",
        ["CP systems: Strong consistency, may return errors during partition — banking, inventory",
         "AP systems: Always available, eventual consistency — social media feeds, caching",
         "PACELC: Extension — during Partition choose A or C; Else choose Latency or Consistency",
         "Real-world: Most systems choose AP with eventual consistency and conflict resolution"],
        "HIGH", 0.96, times_applied=55,
        context={"applies_to": ["ALL"], "domain": "distributed systems"}
    ),
    "sd_load_balancing": _learning(
        "PATTERN", "SYSTEM_DESIGN",
        "Load balancing strategies: Round-robin (simple), least connections (best for varying request times), "
        "weighted (different server capacities), IP hash (session affinity). "
        "L4 (TCP) vs L7 (HTTP) load balancers. Health checks to remove unhealthy instances.",
        ["Nginx: upstream backend { least_conn; server app1:8080; server app2:8080; }",
         "K8s: Service type LoadBalancer or Ingress controller",
         "Cloud: GCP Cloud Load Balancing, AWS ALB/NLB, Azure Application Gateway",
         "Health check: GET /health every 10s, 3 failures → remove from pool, 2 successes → re-add"],
        "HIGH", 0.96, times_applied=65,
        context={"applies_to": ["ALL"]}
    ),
    "sd_horizontal_scaling": _learning(
        "PATTERN", "SYSTEM_DESIGN",
        "Horizontal vs vertical scaling: Horizontal (add more machines) preferred — no single point of failure, "
        "linear cost scaling. Vertical (bigger machine) simpler but has limits. "
        "Stateless services scale horizontally. Store state externally (Redis, DB). Auto-scale on metrics.",
        ["Stateless: No in-memory sessions — use Redis/JWT for auth state",
         "Auto-scale: K8s HPA (CPU > 70% → add pod), Cloud Run (request concurrency)",
         "Database scaling: Read replicas, sharding, connection pooling",
         "Rule: If you can't add a second instance and have it work, it's not horizontally scalable"],
        "HIGH", 0.96, times_applied=60,
        context={"applies_to": ["ALL"]}
    ),
    "sd_database_sharding": _learning(
        "PATTERN", "SYSTEM_DESIGN",
        "Database sharding: Split data across multiple databases by shard key. "
        "Range-based (date ranges), hash-based (user_id % N), directory-based (lookup table). "
        "Challenges: Cross-shard queries, rebalancing, joins. Use only when single DB can't handle load.",
        ["Hash shard: shard = hash(user_id) % num_shards — even distribution",
         "Range shard: 2024 data → shard_1, 2025 data → shard_2 — easy archive",
         "Avoid: Cross-shard joins, cross-shard transactions — redesign schema instead",
         "Alternative: Read replicas + connection pooling before sharding, it is simpler"],
        "MEDIUM", 0.94, times_applied=30,
        context={"applies_to": ["PostgreSQL", "MySQL", "MongoDB"]}
    ),
    "sd_caching_strategy": _learning(
        "PATTERN", "SYSTEM_DESIGN",
        "Caching strategies: Cache-aside (app manages cache), Write-through (write to cache+DB), "
        "Write-behind (write to cache, async to DB), Read-through (cache loads on miss). "
        "Invalidation: TTL (simplest), event-based (pub/sub), versioned keys.",
        ["Cache-aside: if (cache.has(key)) return cache.get(key); val = db.find(key); cache.set(key, val, ttl); return val;",
         "Write-through: cache.set(key, val); db.save(val); — consistent but slower writes",
         "TTL: Set reasonable expiry (5min for hot data, 1hr for config, 24hr for static)",
         "Stampede prevention: Use distributed locks or probabilistic early expiration"],
        "HIGH", 0.96, times_applied=70,
        context={"applies_to": ["Redis", "ALL"]}
    ),
    "sd_circuit_breaker": _learning(
        "PATTERN", "SYSTEM_DESIGN",
        "Circuit breaker pattern: Prevent cascading failures. States: CLOSED (normal), OPEN (fail fast), "
        "HALF-OPEN (test recovery). Track failure rate. Open circuit after threshold. "
        "Try again after timeout. Libraries: Resilience4j (Java), polly (.NET), opossum (Node).",
        ["Resilience4j: @CircuitBreaker(name='userService', fallbackMethod='fallback')",
         "Config: failureRateThreshold=50, waitDurationInOpenState=30s, slidingWindowSize=10",
         "Fallback: Return cached data, default response, or graceful degradation message",
         "Monitor: Track circuit state changes, failure rates, response times in metrics"],
        "HIGH", 0.95, times_applied=45,
        context={"applies_to": ["Java", "Node.js", ".NET"]}
    ),
    "sd_eventual_consistency": _learning(
        "PATTERN", "SYSTEM_DESIGN",
        "Eventual consistency: All replicas converge to same state given enough time. "
        "Patterns: Event sourcing (store events, derive state), CQRS (separate read/write models), "
        "Saga (distributed transactions via compensating actions). Use for high availability.",
        ["Event sourcing: Store OrderCreated, ItemAdded, OrderPaid → replay to get current state",
         "CQRS: Write → command model (normalized), Read → query model (denormalized, fast)",
         "Saga: Step1 → Step2 → Step3, if Step3 fails → compensate Step2 → compensate Step1",
         "Idempotency: All handlers must be idempotent — processing same event twice = same result"],
        "HIGH", 0.95, times_applied=40,
        context={"applies_to": ["ALL"]}
    ),
    "sd_api_gateway": _learning(
        "PATTERN", "SYSTEM_DESIGN",
        "API Gateway: Single entry point for microservices. Handles routing, auth, rate limiting, "
        "SSL termination, request transformation, response caching. "
        "Products: Kong, Traefik, AWS API Gateway, GCP API Gateway, Nginx.",
        ["Routing: /api/users → user-service, /api/orders → order-service",
         "Auth: Validate JWT at gateway — don't repeat in each service",
         "Rate limit: Per-user/per-IP limits at gateway level",
         "Transform: Add request ID header, strip internal headers from responses"],
        "HIGH", 0.95, times_applied=50,
        context={"applies_to": ["ALL"]}
    ),

    # ── Cloud Platforms ────────────────────────────────────────────────────
    "sd_aws_core": _learning(
        "PATTERN", "CLOUD",
        "AWS core services: EC2 (VMs), Lambda (serverless functions), S3 (object storage), "
        "RDS (managed SQL), DynamoDB (managed NoSQL), SQS (message queue), SNS (pub/sub), "
        "ECS/EKS (containers), CloudFront (CDN), Route 53 (DNS), IAM (auth).",
        ["Compute: Lambda for event-driven, ECS for containers, EC2 for full control",
         "Storage: S3 for files (99.999999999% durability), EBS for block storage",
         "Database: RDS for SQL (PostgreSQL/MySQL), DynamoDB for NoSQL key-value",
         "Queue: SQS for point-to-point, SNS for fan-out pub/sub, EventBridge for event routing"],
        "HIGH", 0.95, times_applied=80,
        context={"applies_to": ["ALL"], "provider": "AWS"}
    ),
    "sd_gcp_core": _learning(
        "PATTERN", "CLOUD",
        "GCP core services: Compute Engine (VMs), Cloud Run (serverless containers), Cloud Functions "
        "(serverless), GKE (Kubernetes), Cloud SQL, Firestore, Cloud Storage, Pub/Sub, "
        "Cloud CDN, Cloud Load Balancing, IAM, Secret Manager.",
        ["Serverless: Cloud Run for containers (auto-scale to zero), Cloud Functions for event triggers",
         "Database: Cloud SQL (PostgreSQL/MySQL), Firestore (NoSQL), Bigtable (wide-column), Spanner (global SQL)",
         "Messaging: Pub/Sub for async messaging (at-least-once delivery)",
         "Firebase: Auth, Firestore, Hosting, Cloud Functions — full stack for web/mobile"],
        "HIGH", 0.96, times_applied=90,
        context={"applies_to": ["ALL"], "provider": "GCP"}
    ),
    "sd_azure_core": _learning(
        "PATTERN", "CLOUD",
        "Azure core services: App Service (web apps), Azure Functions (serverless), AKS (Kubernetes), "
        "Cosmos DB (multi-model NoSQL), Azure SQL, Blob Storage, Service Bus (messaging), "
        "Azure AD (identity), Key Vault (secrets), Application Insights (monitoring).",
        ["Compute: App Service for web apps, Functions for serverless, AKS for K8s",
         "Database: Cosmos DB (global distribution, 5 consistency levels), Azure SQL (managed SQL Server)",
         "Messaging: Service Bus (enterprise messaging), Event Hubs (streaming), Event Grid (events)",
         "Identity: Azure AD for SSO, managed identities for service-to-service auth"],
        "MEDIUM", 0.94, times_applied=50,
        context={"applies_to": ["ALL"], "provider": "Azure"}
    ),
    "sd_serverless": _learning(
        "PATTERN", "CLOUD",
        "Serverless architecture: Pay per execution, auto-scale, zero ops. Best for event-driven, "
        "APIs, scheduled tasks. Cold start (100ms-10s depending on runtime). Keep functions small. "
        "Stateless — use external store for state. Watch for vendor lock-in.",
        ["AWS Lambda: 15min max, 10GB memory, 6MB payload, 1000 concurrent (default)",
         "Cloud Functions: 9min max (HTTP), 540min (event), 8GB memory",
         "Cold start mitigation: Keep warm with scheduled pings, use provisioned concurrency",
         "Anti-pattern: Long-running tasks, stateful workflows (use Step Functions/Workflows instead)"],
        "HIGH", 0.95, times_applied=55,
        context={"applies_to": ["ALL"]}
    ),

    # ── Message Queues ─────────────────────────────────────────────────────
    "sd_kafka": _learning(
        "PATTERN", "MESSAGING",
        "Apache Kafka: Distributed event streaming. Topics → Partitions → Consumer Groups. "
        "High throughput (millions/sec), durable (replicated log), ordered within partition. "
        "Use for: Event sourcing, log aggregation, stream processing, CDC.",
        ["Producer: producer.send(new ProducerRecord<>('orders', orderId, orderJson));",
         "Consumer: consumer.subscribe('orders'); records = consumer.poll(Duration.ofMillis(100));",
         "Partitioning: Messages with same key go to same partition → ordered per entity",
         "Retention: Keep messages for days/weeks — consumers can replay from any offset"],
        "HIGH", 0.95, times_applied=50,
        context={"applies_to": ["Java", "Python"]}
    ),
    "sd_message_patterns": _learning(
        "PATTERN", "MESSAGING",
        "Message queue patterns: Point-to-point (one consumer), Pub/Sub (fan-out to many), "
        "Request-reply (async RPC), Dead letter queue (failed messages). "
        "Exactly-once is hard — design for at-least-once with idempotent consumers.",
        ["Point-to-point: SQS, RabbitMQ default — one message → one consumer",
         "Pub/Sub: SNS → SQS fan-out, Kafka consumer groups, Google Pub/Sub",
         "DLQ: After N retries → move to dead letter queue for manual inspection",
         "Idempotent consumer: Use message ID + deduplication table to handle duplicates"],
        "HIGH", 0.95, times_applied=45,
        context={"applies_to": ["ALL"]}
    ),

    # ── Observability ──────────────────────────────────────────────────────
    "sd_three_pillars": _learning(
        "PATTERN", "OBSERVABILITY",
        "Three pillars of observability: (1) Logs — structured JSON, correlation IDs. "
        "(2) Metrics — counters, gauges, histograms (Prometheus). "
        "(3) Traces — distributed request tracing (OpenTelemetry). All three needed for production.",
        ["Logs: { 'level': 'ERROR', 'message': '...', 'traceId': 'abc123', 'service': 'user-svc' }",
         "Metrics: http_requests_total{method='GET',status='200'}, request_duration_seconds histogram",
         "Traces: OpenTelemetry SDK → span per service hop → visualize in Jaeger/Tempo",
         "Correlation: Propagate traceId across services via headers (traceparent)"],
        "HIGH", 0.96, times_applied=60,
        context={"applies_to": ["ALL"]}
    ),
    "sd_alerting": _learning(
        "PATTERN", "OBSERVABILITY",
        "Alerting best practices: Define SLOs (e.g., 99.9% uptime, p99 latency < 500ms). "
        "Alert on SLO burn rate, not individual errors. Use severity levels (P1-P4). "
        "PagerDuty/OpsGenie for on-call. Runbooks for every alert. Avoid alert fatigue.",
        ["SLO: 99.9% availability = 8.76 hours downtime/year allowed",
         "Burn rate: Alert when error budget consumed faster than expected (14.4x = page, 6x = ticket)",
         "Runbook: Every alert links to a doc: What it means, impact, steps to investigate/fix",
         "Anti-pattern: Alerting on every 500 error → alert fatigue → people ignore alerts"],
        "HIGH", 0.95, times_applied=40,
        context={"applies_to": ["ALL"]}
    ),
    "sd_distributed_tracing": _learning(
        "PATTERN", "OBSERVABILITY",
        "Distributed tracing with OpenTelemetry: Instrument services with OTEL SDK. "
        "Each request gets a trace ID propagated through all services. Spans represent operations. "
        "Export to Jaeger, Tempo, or cloud-native (Cloud Trace, X-Ray). Critical for microservices debugging.",
        ["Java: OpenTelemetry Java agent (auto-instrument) — attach as -javaagent:opentelemetry-javaagent.jar",
         "Python: from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor",
         "Propagation: W3C TraceContext header (traceparent) propagated automatically",
         "Visualize: Jaeger UI shows request flow across services with timing per span"],
        "HIGH", 0.95, times_applied=35,
        context={"applies_to": ["Java", "Python", "Node.js"]}
    ),
}

SYSTEM_DESIGN_PATTERNS = {
    "pat_service_discovery": _pattern(
        "Service Discovery Pattern", "SYSTEM_DESIGN",
        "Dynamic service discovery for microservices — register on startup, discover at runtime",
        "Microservices architecture with dynamic scaling",
        "// Spring Cloud + K8s DNS\n// Option 1: K8s DNS (simplest)\n// Service 'user-service' reachable at http://user-service.default.svc.cluster.local:8080\nrestTemplate.getForObject(\"http://user-service/api/users/{id}\", User.class, id);\n\n// Option 2: Spring Cloud + Eureka\n@EnableDiscoveryClient\npublic class OrderService {\n  @Autowired DiscoveryClient discoveryClient;\n  List<ServiceInstance> instances = discoveryClient.getInstances(\"user-service\");\n}\n\n// Option 3: Consul / etcd for polyglot microservices",
        "Spring Cloud / K8s", 0.94, times_used=40
    ),
    "pat_saga_orchestration": _pattern(
        "Saga Orchestrator Pattern", "SYSTEM_DESIGN",
        "Orchestrated saga for distributed transactions with compensating actions",
        "Multi-service transactions (e.g., order → payment → inventory)",
        "@Service\npublic class OrderSagaOrchestrator {\n  public OrderResult createOrder(OrderRequest req) {\n    try {\n      var order = orderService.createOrder(req);       // Step 1\n      var payment = paymentService.charge(req);         // Step 2\n      var inventory = inventoryService.reserve(req);    // Step 3\n      return OrderResult.success(order);\n    } catch (PaymentException e) {\n      orderService.cancelOrder(order.getId());           // Compensate Step 1\n      throw e;\n    } catch (InventoryException e) {\n      paymentService.refund(payment.getId());            // Compensate Step 2\n      orderService.cancelOrder(order.getId());           // Compensate Step 1\n      throw e;\n    }\n  }\n}",
        "Java / Spring Boot", 0.95, times_used=30
    ),
    "pat_bulkhead": _pattern(
        "Bulkhead Pattern", "SYSTEM_DESIGN",
        "Isolate critical resources to prevent one failing component from exhausting all resources",
        "Microservices calling multiple downstream services",
        "// Resilience4j Bulkhead\n@Bulkhead(name = \"userService\", type = Bulkhead.Type.THREADPOOL)\npublic CompletableFuture<User> getUser(Long id) {\n  return CompletableFuture.supplyAsync(() -> userClient.getUser(id));\n}\n\n// Config: resilience4j.bulkhead.instances.userService:\n//   maxConcurrentCalls: 25\n//   maxWaitDuration: 500ms\n// resilience4j.thread-pool-bulkhead.instances.userService:\n//   maxThreadPoolSize: 10\n//   coreThreadPoolSize: 5\n//   queueCapacity: 20",
        "Spring Boot + Resilience4j", 0.94, times_used=25
    ),
    "pat_cqrs": _pattern(
        "CQRS Implementation", "SYSTEM_DESIGN",
        "Command Query Responsibility Segregation with separate read/write models",
        "High-read, complex-query systems (dashboards, analytics, search)",
        "// Write side (commands)\n@Service\npublic class OrderCommandService {\n  @Autowired OrderRepository writeRepo;\n  @Autowired ApplicationEventPublisher events;\n  \n  public void placeOrder(PlaceOrderCmd cmd) {\n    Order order = Order.create(cmd);\n    writeRepo.save(order);\n    events.publishEvent(new OrderPlacedEvent(order));\n  }\n}\n\n// Read side (queries) — denormalized view\n@EventListener\npublic void onOrderPlaced(OrderPlacedEvent event) {\n  OrderView view = new OrderView(event); // flat, denormalized\n  readRepo.save(view); // separate read-optimized store\n}\n\n@GetMapping(\"/orders\")\npublic Page<OrderView> listOrders(Pageable p) {\n  return readRepo.findAll(p); // fast reads from denormalized store\n}",
        "Spring Boot", 0.95, times_used=30
    ),
}
