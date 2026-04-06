#!/usr/bin/env python3
"""
Part 2 — Software Architecture
Seeds SupremeAI Firebase with deep knowledge about:
  • SOLID principles & clean code
  • Gang-of-Four design patterns (Creational / Structural / Behavioural)
  • Architectural patterns (Layered, Hexagonal, Clean Architecture, CQRS)
  • Microservices design (service decomposition, inter-service communication)
  • Domain-Driven Design (DDD) building blocks
  • Event-Driven Architecture

Collections written:
  • system_learning          (SystemLearning model records)
  • software_architecture    (rich topic documents)

Run:
  pip install firebase-admin
  python seed_part2_software_architecture.py [--dry-run]
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from seed_lib import _learning, run_part

# ============================================================================
# SYSTEM_LEARNING records
# ============================================================================

SYSTEM_LEARNINGS = {

    "solid_single_responsibility": _learning(
        type_="PATTERN",
        category="SOLID",
        content=(
            "Single Responsibility Principle (SRP): a class should have one, and only one, "
            "reason to change. A 'reason to change' = an actor (user, admin, reporting team) "
            "whose requirements drive changes. "
            "Violation signs: class name contains 'And', 'Manager', 'Helper', 'Util' doing multiple jobs; "
            "method count > 10; file > 300 lines; multiple private helper method groups. "
            "Fix: extract responsibilities into focused classes — UserValidator, UserPersistence, UserNotifier."
        ),
        solutions=[
            "If class name contains 'Manager' or 'Service' doing 5+ things, split by actor",
            "Extract inner helper classes or static util methods into dedicated classes",
            "Use @Service for business logic, @Repository for data, @Component for plumbing",
            "Target: each class < 200 lines, each method < 20 lines",
        ],
        severity="HIGH",
        confidence=0.97,
        times_applied=144,
        context={"author": "Robert C. Martin (Uncle Bob), Clean Code 2008"},
    ),

    "solid_open_closed": _learning(
        type_="PATTERN",
        category="SOLID",
        content=(
            "Open/Closed Principle (OCP): software entities should be open for extension, "
            "closed for modification. Implement new behaviour by adding new code, not changing existing code. "
            "Techniques: Strategy pattern (inject behaviour), Template Method (override hooks), "
            "Decorator (wrap and extend), Chain of Responsibility (pluggable pipeline). "
            "Spring example: instead of if/else on payment type, create PaymentStrategy interface; "
            "each payment method implements it; add new methods without touching existing code."
        ),
        solutions=[
            "Replace if/else on type with polymorphism: interface + implementations",
            "Use Spring's dependency injection to swap strategies without code changes",
            "Define extension points as interfaces in the core; implementations in plugins/modules",
            "Apply OCP to APIs: add new endpoints/fields without removing old ones (backward compat)",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=89,
        context={"pattern_family": "Strategy, Template Method, Decorator, Chain of Responsibility"},
    ),

    "solid_liskov_substitution": _learning(
        type_="PATTERN",
        category="SOLID",
        content=(
            "Liskov Substitution Principle (LSP): subtypes must be substitutable for their base types "
            "without altering correctness. Violation: Square extends Rectangle but overrides setWidth/setHeight "
            "breaking the Rectangle contract. "
            "LSP violations signal: override throws UnsupportedOperationException, "
            "override returns narrower type, override has stronger preconditions. "
            "Fix: favour composition over inheritance; use interfaces that match behaviour contracts."
        ),
        solutions=[
            "Prefer interface implementation over class inheritance for polymorphism",
            "If subclass throws UnsupportedOperationException, break the hierarchy",
            "Use the 'is-substitutable-for' test: can you use the subclass everywhere the superclass is used?",
            "Design interfaces around what clients need, not what implementations can provide",
        ],
        severity="HIGH",
        confidence=0.93,
        times_applied=67,
        context={"coined_by": "Barbara Liskov, 1987"},
    ),

    "solid_dependency_inversion": _learning(
        type_="PATTERN",
        category="SOLID",
        content=(
            "Dependency Inversion Principle (DIP): high-level modules should not depend on low-level modules; "
            "both should depend on abstractions. Abstractions should not depend on details. "
            "Spring Boot example: UserService (high-level) depends on UserRepository interface (abstraction), "
            "not on JpaUserRepository (implementation). The @Autowired field is the interface type. "
            "Benefits: swap implementations without changing business logic, easy unit testing with mocks."
        ),
        solutions=[
            "Always @Autowired the interface, never the concrete implementation class",
            "Define repository interfaces in the domain layer; implementations in infrastructure layer",
            "Use constructor injection (not field injection) for mandatory dependencies — enables final fields",
            "In tests: inject mock via constructor, not via @MockBean field injection magic",
        ],
        severity="HIGH",
        confidence=0.96,
        times_applied=178,
        context={"spring_boot_tip": "Constructor injection makes DIP automatic with Spring + Lombok @RequiredArgsConstructor"},
    ),

    "pattern_strategy": _learning(
        type_="PATTERN",
        category="DESIGN_PATTERN",
        content=(
            "Strategy Pattern: define a family of algorithms, encapsulate each, make them interchangeable. "
            "Structure: Context holds a reference to Strategy interface; delegates work to it. "
            "Spring Boot example: PaymentContext holds PaymentStrategy; StripeStrategy, PayPalStrategy, "
            "CryptoStrategy are all implementations. Select strategy at runtime based on payment type. "
            "Use @Component + @Qualifier or a Map<String, PaymentStrategy> injected by Spring."
        ),
        solutions=[
            "Inject Map<String, Strategy> in Spring to select strategy by key dynamically",
            "Use @ConditionalOnProperty to activate strategies based on configuration",
            "Combine with Factory pattern: StrategyFactory.getStrategy(type) returns correct impl",
            "Name strategies consistently: ${Name}Strategy implements ${Domain}Strategy",
        ],
        severity="MEDIUM",
        confidence=0.97,
        times_applied=112,
        context={"anti_pattern": "if/else or switch on type string — replace with Strategy"},
    ),

    "pattern_builder": _learning(
        type_="PATTERN",
        category="DESIGN_PATTERN",
        content=(
            "Builder Pattern: construct complex objects step-by-step, separating construction from representation. "
            "Java: Lombok @Builder generates a builder class automatically — add to any class. "
            "Use when: object has > 4 constructor parameters, many optional parameters, or immutable objects needed. "
            "Fluent builders chain method calls: User.builder().name('Alice').email('a@b.com').role(ADMIN).build(). "
            "Director variant: encapsulate common build sequences in a director class."
        ),
        solutions=[
            "Add @Builder + @Value (Lombok) for immutable value objects with builder",
            "For DTOs with validation: use @Builder + javax.validation annotations on the class",
            "Consider @SuperBuilder for inheritance hierarchies",
            "Builder.toBuilder() creates a copy with selected fields changed — useful for update DTOs",
        ],
        severity="MEDIUM",
        confidence=0.96,
        times_applied=134,
        context={"lombok": "@Builder annotation eliminates 50+ lines of boilerplate per class"},
    ),

    "pattern_observer": _learning(
        type_="PATTERN",
        category="DESIGN_PATTERN",
        content=(
            "Observer Pattern: define a one-to-many dependency so when one object changes state, "
            "all dependents are notified automatically. "
            "Spring Boot implementation: ApplicationEventPublisher publishes events; "
            "@EventListener methods in any @Component handle them. "
            "@TransactionalEventListener fires after the current transaction commits — "
            "use for sending emails, notifications, or triggering async jobs after a DB write succeeds."
        ),
        solutions=[
            "Use ApplicationEventPublisher + @EventListener instead of direct service-to-service calls",
            "@TransactionalEventListener(phase=AFTER_COMMIT) for post-transaction side effects",
            "@Async on @EventListener for fire-and-forget notifications (requires @EnableAsync)",
            "Spring's event system is local-only — use Kafka/RabbitMQ for cross-service events",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=78,
        context={"benefit": "Decouples publishers from subscribers — breaks circular dependencies"},
    ),

    "pattern_decorator": _learning(
        type_="PATTERN",
        category="DESIGN_PATTERN",
        content=(
            "Decorator Pattern: attach additional responsibilities to an object dynamically. "
            "Alternative to subclassing for extending functionality. "
            "Spring example: wrap a Repository with a CachingRepository decorator that checks "
            "Redis before hitting the database. Both implement the same interface. "
            "Real-world Spring examples: HttpServletRequestWrapper, Spring Security filter chain, "
            "Jackson's ObjectMapper wrappers, Caffeine cache around service methods."
        ),
        solutions=[
            "@Cacheable annotation is Spring's built-in Decorator for caching — add to any service method",
            "Use BeanPostProcessor to wrap beans with cross-cutting concerns (logging, metrics)",
            "For manual decoration: delegate constructor injection — new CachingRepo(new JpaRepo())",
            "Spring AOP is a Decorator factory: @Aspect + @Around pointcut = transparent decoration",
        ],
        severity="MEDIUM",
        confidence=0.93,
        times_applied=56,
        context={"aop_connection": "AOP proxies ARE decorators — Spring uses JDK dynamic proxy or CGLIB"},
    ),

    "architecture_hexagonal": _learning(
        type_="PATTERN",
        category="ARCHITECTURE",
        content=(
            "Hexagonal Architecture (Ports & Adapters / Clean Architecture): "
            "Core domain/application logic is at the centre, knowing nothing about delivery or storage. "
            "Ports: interfaces the domain exposes (inbound) or requires (outbound). "
            "Adapters: implement ports — REST controller (inbound), JPA repository (outbound). "
            "Package structure: domain (entities, value objects, domain services, ports), "
            "application (use cases / commands / queries), infrastructure (adapters: REST, JPA, Kafka). "
            "Benefit: swap database, framework, or UI without touching business logic."
        ),
        solutions=[
            "Define port interfaces in the application layer; adapters in the infrastructure layer",
            "Use cases (application services) orchestrate domain objects and call ports",
            "Never import Spring @Autowired or JPA annotations in the domain layer",
            "Test use cases with fake adapters (in-memory maps) — no Spring context needed",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=63,
        context={
            "coined_by": "Alistair Cockburn, 2005",
            "also_known_as": "Ports & Adapters, Clean Architecture (Robert Martin), Onion Architecture",
        },
    ),

    "architecture_cqrs": _learning(
        type_="PATTERN",
        category="ARCHITECTURE",
        content=(
            "Command Query Responsibility Segregation (CQRS): separate the read (query) model "
            "from the write (command) model. Commands change state; queries return data — never both. "
            "Benefits: read model can be optimised independently (denormalised, cached, different DB). "
            "Command side: validates, processes, emits domain events. "
            "Query side: subscribes to events, maintains a read-optimised projection. "
            "Full CQRS + Event Sourcing: state = replay of all events from event store."
        ),
        solutions=[
            "Start with simple CQRS (separate service methods); add separate stores when scaling",
            "Command bus: @CommandHandler annotated methods (Axon Framework pattern)",
            "Query bus: @QueryHandler methods with projections updated by @EventHandler",
            "For reporting/analytics: CQRS read model in separate DB (Elasticsearch, BigQuery)",
        ],
        severity="HIGH",
        confidence=0.91,
        times_applied=38,
        context={
            "frameworks": ["Axon Framework (Java)", "EventStoreDB", "MediatR (.NET)"],
            "warning": "CQRS adds complexity — only use when read/write loads are significantly different",
        },
    ),

    "microservices_decomposition": _learning(
        type_="PATTERN",
        category="MICROSERVICES",
        content=(
            "Microservice decomposition strategies: "
            "(1) Decompose by business capability: each service owns a bounded context (Orders, Inventory, Users). "
            "(2) Decompose by subdomain (DDD): identify core, supporting, generic subdomains. "
            "(3) Strangler Fig: incrementally migrate monolith to microservices via a facade/proxy. "
            "Service size: team can build and maintain it in one sprint (2-week). "
            "Anti-patterns: nanoservices (too fine-grained, network overhead), "
            "shared database (tight coupling), distributed monolith (synchronous chain of calls)."
        ),
        solutions=[
            "Use DDD bounded contexts to find natural service boundaries",
            "Each service owns its own database — no shared schemas",
            "Prefer async (event/message) communication over sync REST for decoupling",
            "Apply Strangler Fig when migrating: route traffic to new service, retire monolith endpoint",
        ],
        severity="HIGH",
        confidence=0.93,
        times_applied=51,
        context={
            "book": "Building Microservices, Sam Newman",
            "anti_pattern": "Shared database between services = distributed monolith",
        },
    ),

    "ddd_aggregate": _learning(
        type_="PATTERN",
        category="DDD",
        content=(
            "DDD Aggregate: a cluster of domain objects treated as a single unit for data changes. "
            "Aggregate Root: the entry point — all access to aggregate members goes through it. "
            "Invariants: business rules that must always be true within an aggregate. "
            "Example: Order (aggregate root) owns OrderLine items. "
            "Never reference inner aggregate objects from outside — only reference aggregate roots by ID. "
            "Repository: one per aggregate root; loads/saves the entire aggregate atomically."
        ),
        solutions=[
            "Identify invariants first — they define aggregate boundaries",
            "Keep aggregates small (< 10 entities) to avoid large transaction scope",
            "Reference other aggregates by ID only — prevents cross-aggregate coupling",
            "One transaction = one aggregate — if you need to change two, use eventual consistency",
        ],
        severity="HIGH",
        confidence=0.92,
        times_applied=44,
        context={
            "book": "Domain-Driven Design, Eric Evans (Blue Book)",
            "invariant_example": "Order total must not exceed customer credit limit",
        },
    ),

    "pattern_saga_distributed_tx": _learning(
        type_="PATTERN",
        category="MICROSERVICES",
        content=(
            "Saga Pattern: manage distributed transactions without two-phase commit (2PC). "
            "Choreography Saga: each service publishes events; next service reacts — no central coordinator. "
            "Orchestration Saga: a saga orchestrator sends commands to services; handles compensations. "
            "Compensation: when a step fails, run compensating transactions to undo previous steps. "
            "Example: Order Saga — CreateOrder → ReserveInventory → ChargePay → ShipOrder; "
            "if ChargePay fails: ReleaseInventory → CancelOrder."
        ),
        solutions=[
            "Prefer orchestration sagas — easier to track state, add monitoring, handle failures",
            "Store saga state in a database — make sagas resumable after crashes",
            "Every saga step must be idempotent — it may be retried on failure",
            "Use Axon Framework (Java) or Temporal for production saga orchestration",
        ],
        severity="CRITICAL",
        confidence=0.90,
        times_applied=29,
        context={
            "alternatives": "Use 2PC only within a single bounded context; Saga for cross-service",
            "tool": "Axon Framework SagaLifecycle, Temporal Workflow, Apache Camel",
        },
    ),

    "improvement_clean_code_naming": _learning(
        type_="IMPROVEMENT",
        category="CLEAN_CODE",
        content=(
            "Clean code naming conventions: "
            "Variables/fields: noun or noun phrase describing what it IS (userCount, orderTotal). "
            "Methods: verb or verb phrase describing what it DOES (calculateTotal, findByEmail). "
            "Booleans: is/has/can/should prefix (isActive, hasPermission, canDelete). "
            "Classes: nouns (UserService, OrderRepository, PaymentProcessor). "
            "Avoid: abbreviations (usr, ord), generic names (data, info, manager), "
            "single-letter vars except loop counters (i, j, k)."
        ),
        solutions=[
            "Replace abbreviations with full words: usr → user, cfg → config, msg → message",
            "Replace generic names: data → userData, result → orderResult, list → activeUsers",
            "Boolean: rename isFlag → isLoggedIn, rename check → hasValidToken",
            "If a name needs a comment to explain it, rename it instead of commenting",
        ],
        severity="MEDIUM",
        confidence=0.95,
        times_applied=210,
        context={"reference": "Clean Code, Chapter 2 — Meaningful Names, Robert C. Martin"},
    ),
}

# ============================================================================
# SOFTWARE_ARCHITECTURE rich topic documents
# ============================================================================

SOFTWARE_ARCHITECTURE_DOCS = {

    "solid_principles_overview": {
        "topic": "SOLID Principles — Complete Reference",
        "category": "SOLID",
        "description": (
            "SOLID is a set of five object-oriented design principles that produce code that is "
            "easier to maintain, extend, and test. Coined by Robert C. Martin."
        ),
        "principles": {
            "S_SRP": {
                "name": "Single Responsibility",
                "rule": "A class has one reason to change",
                "smell": "God class, >300 lines, 'Manager' class doing everything",
                "fix": "Extract into focused classes: Validator, Persister, Notifier",
            },
            "O_OCP": {
                "name": "Open/Closed",
                "rule": "Open for extension, closed for modification",
                "smell": "if/else or switch on type string",
                "fix": "Strategy pattern, polymorphism, plugin architecture",
            },
            "L_LSP": {
                "name": "Liskov Substitution",
                "rule": "Subtypes must be substitutable for base types",
                "smell": "Subclass throws UnsupportedOperationException",
                "fix": "Composition over inheritance; interface segregation",
            },
            "I_ISP": {
                "name": "Interface Segregation",
                "rule": "Clients should not depend on interfaces they don't use",
                "smell": "Fat interface with 15+ methods; implementations leave 10 empty",
                "fix": "Split into role-based interfaces: Readable, Writable, Deletable",
            },
            "D_DIP": {
                "name": "Dependency Inversion",
                "rule": "Depend on abstractions, not concretions",
                "smell": "@Autowired ConcreteServiceImpl; new ConcreteClass() inside logic",
                "fix": "Constructor inject interface; use factory or DI container",
            },
        },
        "spring_boot_application": (
            "Spring Boot embodies all SOLID principles:\n"
            "S: @Service/@Repository/@Controller separation\n"
            "O: @Conditional + interface-based extension points\n"
            "L: Spring proxies respect contracts\n"
            "I: Spring Data interfaces (CrudRepository vs JpaRepository vs PagingAndSortingRepository)\n"
            "D: @Autowired always to interface types"
        ),
        "confidence": 0.97,
    },

    "design_patterns_creational": {
        "topic": "Creational Design Patterns",
        "category": "DESIGN_PATTERNS",
        "description": "Patterns that deal with object creation mechanisms.",
        "patterns": {
            "Singleton": {
                "intent": "Ensure only one instance exists globally",
                "spring_equivalent": "All @Component/@Service beans are singletons by default",
                "java_impl": "Enum singleton: public enum Config { INSTANCE; ... }",
                "warning": "Global mutable state — avoid in concurrent systems; prefer DI",
            },
            "Factory_Method": {
                "intent": "Define interface for creating objects; subclass decides which class to instantiate",
                "example": "NotificationFactory.create('EMAIL') returns EmailNotification",
                "spring_impl": "Map<String, NotificationStrategy> injected by Spring; get by type key",
            },
            "Abstract_Factory": {
                "intent": "Create families of related objects without specifying concrete classes",
                "example": "UIComponentFactory creates Button+TextBox for Web vs Mobile",
                "spring_impl": "@Configuration classes that @Bean-produce related infrastructure objects",
            },
            "Builder": {
                "intent": "Construct complex objects step by step",
                "java_impl": "Lombok @Builder auto-generates fluent builder",
                "use_when": "4+ constructor params, many optional params, immutable objects",
            },
            "Prototype": {
                "intent": "Create new objects by cloning an existing object",
                "java_impl": "implement Cloneable; override clone(); or use serialisation copy",
                "use_when": "Object creation is expensive; need modified copy of existing object",
            },
        },
        "confidence": 0.95,
    },

    "design_patterns_structural": {
        "topic": "Structural Design Patterns",
        "category": "DESIGN_PATTERNS",
        "description": "Patterns that deal with object composition and relationships.",
        "patterns": {
            "Adapter": {
                "intent": "Convert interface of one class to another expected by clients",
                "example": "LegacyPaymentAdapter wraps old payment library to match new PaymentPort interface",
                "spring_impl": "Implement port interface; delegate to third-party SDK",
            },
            "Decorator": {
                "intent": "Add responsibilities to objects dynamically",
                "spring_impl": "@Cacheable, @Transactional, @Async — Spring AOP decorates methods",
                "manual_impl": "CachingRepository(JpaRepository) — constructor injection",
            },
            "Facade": {
                "intent": "Simplified interface to a complex subsystem",
                "example": "OrderFacade hides complexity of Payment+Inventory+Notification services",
                "spring_impl": "Service class that orchestrates multiple other services = Facade",
            },
            "Proxy": {
                "intent": "Provide surrogate or placeholder for another object",
                "spring_impl": "Spring creates JDK or CGLIB proxies for @Transactional/@Cacheable beans",
                "use_when": "Lazy loading, access control, logging, remote access",
            },
            "Composite": {
                "intent": "Compose objects into tree structures to represent part-whole hierarchies",
                "example": "UI component tree: Form contains Fieldset contains TextField",
                "use_when": "Recursive tree structures; uniform treatment of leaf and composite",
            },
        },
        "confidence": 0.94,
    },

    "design_patterns_behavioural": {
        "topic": "Behavioural Design Patterns",
        "category": "DESIGN_PATTERNS",
        "description": "Patterns about communication and responsibility between objects.",
        "patterns": {
            "Strategy": {
                "intent": "Define family of algorithms, make them interchangeable",
                "spring_impl": "Map<String, PaymentStrategy> injected by Spring; select by type",
                "replaces": "if/else or switch on type string",
            },
            "Observer": {
                "intent": "One-to-many dependency; notify all dependents on state change",
                "spring_impl": "ApplicationEventPublisher + @EventListener + @TransactionalEventListener",
                "use_when": "Decoupling services; sending notifications after business events",
            },
            "Command": {
                "intent": "Encapsulate request as an object; support undo/redo",
                "spring_impl": "CQRS command objects; @CommandHandler methods (Axon Framework)",
                "use_when": "Queuing operations, undoable operations, audit logging",
            },
            "Chain_of_Responsibility": {
                "intent": "Pass request along a chain of handlers",
                "spring_impl": "Spring Security filter chain, HandlerInterceptor chain, Spring Integration",
                "use_when": "Request validation pipeline, middleware, event processing",
            },
            "Template_Method": {
                "intent": "Define skeleton of algorithm; defer specific steps to subclasses",
                "spring_impl": "JdbcTemplate, RestTemplate, AbstractController",
                "use_when": "Fixed algorithm structure with pluggable steps",
            },
            "State": {
                "intent": "Allow object to alter behaviour when internal state changes",
                "example": "Order state machine: PENDING→CONFIRMED→SHIPPED→DELIVERED→CANCELLED",
                "spring_impl": "Spring State Machine or explicit state + strategy per state",
            },
        },
        "confidence": 0.95,
    },

    "microservices_patterns": {
        "topic": "Microservices Design Patterns",
        "category": "MICROSERVICES",
        "description": "Patterns for designing, deploying, and operating microservices at scale.",
        "patterns": {
            "API_Gateway": {
                "intent": "Single entry point for all client requests; handles routing, auth, rate limiting",
                "tools": ["Spring Cloud Gateway", "Kong", "AWS API Gateway", "Traefik"],
                "responsibilities": ["Authentication", "Rate limiting", "Load balancing", "SSL termination", "Request routing"],
            },
            "Service_Discovery": {
                "intent": "Services find each other dynamically without hardcoded addresses",
                "tools": ["Kubernetes Service DNS", "Eureka (Spring Cloud)", "Consul"],
                "client_side": "Load-balanced HTTP client queries registry",
                "server_side": "Load balancer queries registry and routes",
            },
            "Circuit_Breaker": {
                "intent": "Detect failing dependencies and fail fast instead of cascading failures",
                "tools": ["Resilience4j", "Spring Cloud Circuit Breaker"],
                "states": ["CLOSED (normal)", "OPEN (failing fast)", "HALF_OPEN (probing recovery)"],
            },
            "Saga": {
                "intent": "Manage distributed transactions across services without 2PC",
                "types": ["Choreography (event-driven)", "Orchestration (central coordinator)"],
                "tools": ["Axon Framework", "Temporal", "Apache Camel"],
            },
            "Outbox_Pattern": {
                "intent": "Atomically persist business data and the event to publish in one DB transaction",
                "flow": "Write to business table + outbox table in same transaction; relay reads outbox and publishes",
                "tools": ["Debezium CDC", "Transactional Outbox with polling"],
            },
            "Sidecar": {
                "intent": "Deploy cross-cutting concerns as a separate container alongside the main service",
                "examples": ["Envoy proxy (service mesh)", "Logging agent", "Secret rotator"],
                "tools": ["Istio + Envoy", "Linkerd", "Dapr"],
            },
        },
        "confidence": 0.93,
    },

    "ddd_building_blocks": {
        "topic": "Domain-Driven Design (DDD) Building Blocks",
        "category": "DDD",
        "description": (
            "DDD provides a vocabulary and set of patterns for modelling complex business domains. "
            "The goal is to align the code model with the business domain model."
        ),
        "tactical_patterns": {
            "Entity": "Has identity (ID); mutable; same ID = same entity despite attribute changes",
            "Value_Object": "No identity; defined by attributes; immutable; Money(100, USD) == Money(100, USD)",
            "Aggregate": "Cluster of entities + value objects with consistency boundary; one aggregate root",
            "Domain_Event": "Something that happened in the domain; past tense; OrderPlaced, PaymentFailed",
            "Repository": "Abstraction for persisting/loading aggregates; one per aggregate root",
            "Domain_Service": "Stateless operation that doesn't naturally belong to any entity",
            "Factory": "Complex aggregate creation logic extracted from constructors",
            "Application_Service": "Orchestrates use cases; coordinates domain objects and ports",
        },
        "strategic_patterns": {
            "Bounded_Context": "Explicit boundary within which a model applies; ubiquitous language is consistent",
            "Context_Map": "Visual map of relationships between bounded contexts",
            "Ubiquitous_Language": "Shared vocabulary between devs and domain experts used in code",
            "Anti_Corruption_Layer": "Translator between bounded contexts with different models",
            "Shared_Kernel": "Small subset of domain shared between two contexts (use sparingly)",
        },
        "spring_boot_mapping": {
            "Entity": "@Entity class with @Id",
            "Value_Object": "@Embeddable class (immutable, no separate table)",
            "Repository": "JpaRepository interface",
            "Domain_Event": "ApplicationEvent subclass",
            "Application_Service": "@Service class orchestrating use cases",
        },
        "confidence": 0.92,
    },

    "event_driven_architecture": {
        "topic": "Event-Driven Architecture (EDA)",
        "category": "ARCHITECTURE",
        "description": (
            "EDA: components produce, detect, and consume events. "
            "Services are decoupled — producers don't know consumers. "
            "Enables high scalability, resilience, and loose coupling."
        ),
        "event_types": {
            "domain_event": "Something that happened in the business domain — OrderPlaced, UserRegistered",
            "integration_event": "Cross-service domain event published to message broker",
            "command_event": "Instruction to perform an action — send to specific consumer",
            "notification_event": "Informational — no response expected; fan-out to many consumers",
        },
        "messaging_platforms": {
            "Apache_Kafka": {
                "use_when": "High throughput, event streaming, event sourcing, audit log",
                "characteristics": "Durable log, replay, consumer groups, partitions",
                "spring": "Spring Kafka (@KafkaListener, KafkaTemplate)",
            },
            "RabbitMQ": {
                "use_when": "Task queues, work distribution, flexible routing, lower throughput",
                "characteristics": "AMQP, exchanges, queues, bindings, dead-letter queues",
                "spring": "Spring AMQP (@RabbitListener, RabbitTemplate)",
            },
            "Google_Pub_Sub": {
                "use_when": "GCP-native; serverless; global fan-out",
                "characteristics": "At-least-once delivery, push/pull, ordering keys",
                "spring": "Spring Cloud GCP Pub/Sub",
            },
            "Firebase_Realtime": {
                "use_when": "Mobile/web real-time sync; small payloads; offline support",
                "characteristics": "WebSocket, offline cache, optimistic updates",
            },
        },
        "best_practices": [
            "Events should be immutable — never edit a published event",
            "Include event ID, timestamp, source, version in every event header",
            "Events are facts (past tense): OrderPlaced, not PlaceOrder",
            "Make event consumers idempotent — handle duplicate delivery gracefully",
            "Use schema registry (Confluent Schema Registry) to manage event schema evolution",
        ],
        "confidence": 0.93,
    },
}

# ============================================================================
# ENTRY POINT
# ============================================================================

if __name__ == "__main__":
    run_part(
        part_name="Part 2 — Software Architecture",
        collections={
            "system_learning": SYSTEM_LEARNINGS,
            "software_architecture": SOFTWARE_ARCHITECTURE_DOCS,
        },
    )
