"""
Part 4: Design Patterns & Architecture Knowledge
Covers: GoF patterns, architectural patterns, microservices, event-driven, CQRS, DDD
~30 learnings + ~25 patterns = 55 documents
"""
from seed_data.helpers import _learning, _pattern

DESIGN_PATTERN_LEARNINGS = {

    # ── Creational Patterns ────────────────────────────────────────────────
    "dp_singleton": _learning(
        "PATTERN", "DESIGN_PATTERNS",
        "Singleton: One instance globally. In Java: enum Singleton or static holder. "
        "In Spring: @Component/@Service are singletons by default. In Python: module-level instance. "
        "Avoid singletons for mutable state in multi-threaded apps — use DI instead.",
        ["Java enum: enum Config { INSTANCE; private final Properties props; Config() { props = load(); } }",
         "Spring: @Service classes are singletons — just inject them",
         "Python: create instance at module level, import it",
         "Anti-pattern: Global mutable state — use dependency injection instead"],
        "MEDIUM", 0.95, times_applied=60,
        context={"applies_to": ["Java", "Python", "TypeScript"], "gof": "Creational"}
    ),
    "dp_factory": _learning(
        "PATTERN", "DESIGN_PATTERNS",
        "Factory Method & Abstract Factory: Create objects without specifying concrete class. "
        "Use when: constructor logic is complex, return type depends on input, need to swap implementations. "
        "In Spring: @Bean methods in @Configuration are factories.",
        ["Simple factory: static Notification create(String type) { return switch(type) { case \"email\" -> new EmailNotification(); case \"sms\" -> new SmsNotification(); }; }",
         "Abstract factory: interface UIFactory { Button createButton(); Dialog createDialog(); } class DarkUIFactory implements UIFactory { ... }",
         "Spring: @Bean public Storage storage() { return useCloud ? new S3Storage() : new LocalStorage(); }",
         "Python: Protocol + factory function for type-safe factories"],
        "HIGH", 0.95, times_applied=70,
        context={"applies_to": ["Java", "Python", "TypeScript"], "gof": "Creational"}
    ),
    "dp_builder": _learning(
        "PATTERN", "DESIGN_PATTERNS",
        "Builder: Construct complex objects step by step. Use when 4+ constructor parameters or "
        "many optional fields. Fluent API: builder.name('x').age(5).build(). In Java: Lombok @Builder. "
        "In Kotlin: default parameter values make explicit builders rarely needed.",
        ["Java: User.builder().name(\"John\").email(\"j@x.com\").age(25).build()",
         "Lombok: @Builder @Data class User { String name; String email; int age; }",
         "TypeScript: class QueryBuilder { where(field, value) { this.wheres.push({field,value}); return this; } build() { return this.wheres; } }",
         "Kotlin: data class User(val name: String, val email: String = \"\", val age: Int = 0) // no builder needed"],
        "MEDIUM", 0.96, times_applied=80,
        context={"applies_to": ["Java", "TypeScript", "Python"], "gof": "Creational"}
    ),

    # ── Structural Patterns ────────────────────────────────────────────────
    "dp_adapter": _learning(
        "PATTERN", "DESIGN_PATTERNS",
        "Adapter: Convert interface of one class to another. Use when integrating third-party libraries, "
        "legacy code, or different API versions. Composition over inheritance — wrap the adaptee.",
        ["Java: class PayPalAdapter implements PaymentGateway { private final PayPalSDK sdk; public void charge(Money m) { sdk.createPayment(m.toCents()); } }",
         "TypeScript: class FirebaseAuthAdapter implements AuthProvider { constructor(private fb: FirebaseAuth) {} async login(email, pw) { return this.fb.signInWithEmailAndPassword(email, pw); } }",
         "Use case: Wrapping different AI provider APIs behind a common interface",
         "Spring: Adapter beans that implement your interface and delegate to external lib"],
        "HIGH", 0.94, times_applied=55,
        context={"applies_to": ["Java", "TypeScript", "Python"], "gof": "Structural"}
    ),
    "dp_decorator": _learning(
        "PATTERN", "DESIGN_PATTERNS",
        "Decorator: Add behavior dynamically without modifying original class. Wraps object with "
        "same interface. Use for: logging, caching, retry, auth checks, metrics. In Python: @decorator syntax. "
        "In Java: implement same interface, wrap original.",
        ["Java: class LoggingService implements UserService { private final UserService delegate; public User get(Long id) { log.info(\"Getting user {}\", id); return delegate.get(id); } }",
         "Python: @retry(max_attempts=3) def fetch_data(): ...",
         "TypeScript: function withCache<T>(fn: () => Promise<T>, ttl: number): () => Promise<T>",
         "Spring AOP: @Around for cross-cutting concerns (logging, timing, auth)"],
        "HIGH", 0.95, times_applied=65,
        context={"applies_to": ["Java", "Python", "TypeScript"], "gof": "Structural"}
    ),
    "dp_facade": _learning(
        "PATTERN", "DESIGN_PATTERNS",
        "Facade: Simplified interface to a complex subsystem. One entry point that coordinates "
        "multiple services. Use for: orchestrating multi-step processes, simplifying API surface, "
        "wrapping complex libraries.",
        ["Java: @Service class OrderFacade { private final OrderService orders; private final PaymentService payments; private final NotificationService notifications; public OrderResult placeOrder(OrderRequest req) { Order o = orders.create(req); payments.charge(o); notifications.send(o); return new OrderResult(o); } }",
         "Use when: Client would otherwise need to call 3+ services in sequence",
         "Not when: Subsystem is simple enough to use directly",
         "Spring: Facade services coordinate domain services"],
        "MEDIUM", 0.94, times_applied=50,
        context={"applies_to": ["Java", "TypeScript", "Python"], "gof": "Structural"}
    ),

    # ── Behavioral Patterns ────────────────────────────────────────────────
    "dp_strategy": _learning(
        "PATTERN", "DESIGN_PATTERNS",
        "Strategy: Define family of algorithms, encapsulate each, make them interchangeable. "
        "Use when: multiple ways to do something, algorithm varies by context. "
        "In Java: interface + implementations. In Python/JS: pass functions directly.",
        ["Java: interface SortStrategy { List<T> sort(List<T> items); } class QuickSort implements SortStrategy { ... }",
         "Functional: service.process(items, Comparator.comparing(Item::getPrice))",
         "Python: def process(data, strategy: Callable): return strategy(data)",
         "Use case: Different AI providers as strategies — same interface, swap at runtime"],
        "HIGH", 0.95, times_applied=60,
        context={"applies_to": ["Java", "Python", "TypeScript"], "gof": "Behavioral"}
    ),
    "dp_observer": _learning(
        "PATTERN", "DESIGN_PATTERNS",
        "Observer/Event: Publish-subscribe for loose coupling. Subject notifies observers on state change. "
        "In Spring: ApplicationEventPublisher. In Node.js: EventEmitter. In React: useEffect + context. "
        "In Flutter: Stream/StreamController or ChangeNotifier.",
        ["Spring: publisher.publishEvent(new UserCreatedEvent(user)); @EventListener void onUserCreated(UserCreatedEvent e) { sendWelcomeEmail(e.getUser()); }",
         "Node.js: emitter.on('order:created', (order) => notifyUser(order))",
         "React: useEffect(() => { const unsub = onSnapshot(query, (snap) => setData(snap.docs)); return unsub; }, [])",
         "Use for: Decoupling domain events from side effects (email, notifications, audit)"],
        "HIGH", 0.95, times_applied=70,
        context={"applies_to": ["Java", "Node.js", "React", "Flutter"], "gof": "Behavioral"}
    ),
    "dp_command": _learning(
        "PATTERN", "DESIGN_PATTERNS",
        "Command: Encapsulate request as an object. Enables: undo/redo, queuing, logging, macro recording. "
        "Each command has execute() and optionally undo(). Use with command queue for async processing.",
        ["Java: interface Command { void execute(); void undo(); } class CreateUserCommand implements Command { ... }",
         "Queue: commandQueue.add(new DeployCommand(app)); // process later",
         "Undo: commandHistory.push(cmd); cmd.execute(); // undo: commandHistory.pop().undo()",
         "Use case: SupremeAI admin operations — each operation is a command with audit trail"],
        "MEDIUM", 0.93, times_applied=35,
        context={"applies_to": ["Java", "TypeScript"], "gof": "Behavioral"}
    ),

    # ── Architectural Patterns ─────────────────────────────────────────────
    "arch_clean_architecture": _learning(
        "PATTERN", "ARCHITECTURE",
        "Clean Architecture: Dependency rule — outer layers depend on inner. Layers: Entities (domain) → "
        "Use Cases (application) → Interface Adapters (controllers, gateways) → Frameworks (web, DB). "
        "Domain has NO framework imports. Use cases orchestrate domain logic.",
        ["Domain: class Order { private List<OrderItem> items; Money calculateTotal() { ... } } // no Spring imports",
         "Use case: class PlaceOrder { private OrderRepo repo; private PaymentGateway pay; OrderResult execute(PlaceOrderCommand cmd) { ... } }",
         "Adapter: @RestController calls use case, maps DTO ↔ domain",
         "Rule: Inner layers never import from outer layers — use interfaces for dependency inversion"],
        "HIGH", 0.94, times_applied=45,
        context={"applies_to": ["Java", "TypeScript", "Python", "Dart"], "author": "Robert C. Martin"}
    ),
    "arch_hexagonal": _learning(
        "PATTERN", "ARCHITECTURE",
        "Hexagonal (Ports & Adapters): Domain at center. Ports = interfaces. Adapters = implementations. "
        "Driving adapters (REST, CLI, tests) call the domain. Driven adapters (DB, email, APIs) are called by domain. "
        "Domain defines port interfaces — adapters implement them.",
        ["Port: interface OrderRepository { Order save(Order o); Optional<Order> findById(OrderId id); }",
         "Adapter: @Repository class JpaOrderRepository implements OrderRepository { ... }",
         "Driving: @RestController calls OrderService (which uses ports)",
         "Test: class InMemoryOrderRepository implements OrderRepository { private Map<OrderId,Order> store = new HashMap<>(); }"],
        "HIGH", 0.93, times_applied=35,
        context={"applies_to": ["Java", "TypeScript"], "author": "Alistair Cockburn"}
    ),
    "arch_microservices": _learning(
        "PATTERN", "ARCHITECTURE",
        "Microservices: Each service owns its data, communicates via API/events. "
        "Start monolith-first, extract services at clear boundaries. Use API gateway for routing. "
        "Event-driven communication for async workflows. Circuit breaker for resilience.",
        ["Service boundary: One service per domain aggregate (UserService, OrderService, PaymentService)",
         "Communication: Sync (REST/gRPC) for queries, Async (events/messages) for commands",
         "Data: Each service has its own database — no shared DB",
         "Patterns: API Gateway, Circuit Breaker, Saga, Event Sourcing, CQRS"],
        "HIGH", 0.93, times_applied=40,
        context={"applies_to": ["Spring Boot", "Node.js", "Go"], "warning": "Don't start with microservices — start monolith"}
    ),
    "arch_cqrs": _learning(
        "PATTERN", "ARCHITECTURE",
        "CQRS: Separate read and write models. Commands modify state (write). Queries read state (read). "
        "Write model optimized for consistency. Read model optimized for query performance. "
        "Can use different DBs for each. Often paired with Event Sourcing.",
        ["Command: class CreateOrder implements Command { String userId; List<Item> items; }",
         "Query: class GetOrderSummary implements Query<OrderSummary> { String orderId; }",
         "Write store: Normalized relational DB with constraints",
         "Read store: Denormalized views, materialized views, or Elasticsearch for search"],
        "MEDIUM", 0.91, times_applied=25,
        context={"applies_to": ["Java", "TypeScript", ".NET"], "pairs_with": "Event Sourcing"}
    ),
    "arch_event_sourcing": _learning(
        "PATTERN", "ARCHITECTURE",
        "Event Sourcing: Store events, not current state. Reconstruct state by replaying events. "
        "Events are immutable facts: OrderCreated, ItemAdded, OrderShipped. "
        "Benefits: Complete audit trail, temporal queries, easy debugging. Costs: Complexity, eventual consistency.",
        ["Event: record OrderCreated(OrderId id, UserId user, List<Item> items, Instant at) {}",
         "Store: eventStore.append(orderId, new OrderCreated(...))",
         "Rebuild: List<Event> events = eventStore.load(orderId); Order order = Order.rebuild(events);",
         "Snapshot: Periodically save state to avoid replaying all events"],
        "MEDIUM", 0.90, times_applied=20,
        context={"applies_to": ["Java", "TypeScript", ".NET"], "pairs_with": "CQRS"}
    ),
    "arch_saga": _learning(
        "PATTERN", "ARCHITECTURE",
        "Saga pattern: Manage distributed transactions across microservices. "
        "Choreography: Each service publishes events, next service reacts. "
        "Orchestration: Central coordinator directs saga steps. Compensating transactions for rollback.",
        ["Choreography: OrderService → OrderCreated → PaymentService → PaymentCompleted → ShippingService",
         "Orchestration: OrderSaga { step1: createOrder(); step2: chargePayment(); step3: shipOrder(); compensate1: cancelShipment(); ... }",
         "Compensation: If step 3 fails → compensate step 2 (refund) → compensate step 1 (cancel order)",
         "Use choreography for simple flows (<4 steps), orchestration for complex flows"],
        "HIGH", 0.92, times_applied=30,
        context={"applies_to": ["Microservices", "Spring Boot", "Node.js"]}
    ),

    # ── DDD Patterns ───────────────────────────────────────────────────────
    "ddd_aggregate": _learning(
        "PATTERN", "DDD",
        "DDD Aggregates: Cluster of entities with one root entity. All access through aggregate root. "
        "Aggregate root enforces invariants. Transactions should not span multiple aggregates. "
        "Reference other aggregates by ID, not object reference.",
        ["Aggregate: class Order (root) contains OrderItems, ShippingAddress — accessed only through Order",
         "Rule: order.addItem(item) validates business rules, not orderItem.setOrder(order)",
         "Reference by ID: class Order { UserId customerId; } not User customer;",
         "Transaction: One aggregate per transaction — use domain events for cross-aggregate"],
        "HIGH", 0.93, times_applied=35,
        context={"applies_to": ["Java", "TypeScript", "C#"], "methodology": "Domain-Driven Design"}
    ),
    "ddd_value_object": _learning(
        "PATTERN", "DDD",
        "DDD Value Objects: Defined by attributes, not identity. Immutable. Two VOs with same values are equal. "
        "Use for: Money, Email, Address, DateRange. Implement equals/hashCode based on all fields. "
        "In Java: records are perfect VOs. In TypeScript: readonly types.",
        ["Java: record Money(BigDecimal amount, Currency currency) { Money add(Money other) { assert currency.equals(other.currency); return new Money(amount.add(other.amount), currency); } }",
         "TypeScript: type Email = Readonly<{ value: string }>; function createEmail(s: string): Email { if(!isValid(s)) throw new Error(); return { value: s }; }",
         "Use: order.total = Money.of(100, USD); not order.total = 100; order.currency = 'USD';",
         "Test: assertEquals(Money.of(10, USD), Money.of(10, USD)); // true by value"],
        "HIGH", 0.94, times_applied=45,
        context={"applies_to": ["Java", "TypeScript", "C#"], "methodology": "Domain-Driven Design"}
    ),
}

# ============================================================================
#  PATTERNS
# ============================================================================

DESIGN_PATTERNS = {
    "pat_repository_pattern": _pattern(
        "Repository Pattern", "ARCHITECTURE",
        "Abstract data access behind a clean interface. Enables swapping implementations (DB, API, in-memory)",
        "Data layer in any application",
        "interface UserRepository { User save(User user); Optional<User> findById(Long id); List<User> findAll(); void delete(Long id); } class JpaUserRepository implements UserRepository { @PersistenceContext EntityManager em; public User save(User u) { em.persist(u); return u; } }",
        "Java/TypeScript", 0.97, times_used=120
    ),
    "pat_unit_of_work": _pattern(
        "Unit of Work", "ARCHITECTURE",
        "Track changes to objects during a business transaction and coordinate writing changes as a single unit",
        "Complex transactions involving multiple repositories",
        "@Transactional public OrderResult placeOrder(PlaceOrderCommand cmd) { Order order = Order.create(cmd); orderRepo.save(order); Payment payment = paymentService.charge(order); paymentRepo.save(payment); inventory.reduce(order.getItems()); eventPublisher.publish(new OrderPlaced(order)); return OrderResult.success(order); }",
        "Java/Spring", 0.94, times_used=50
    ),
    "pat_circuit_breaker": _pattern(
        "Circuit Breaker", "RESILIENCE",
        "Prevent cascading failures by stopping calls to failing services. States: CLOSED → OPEN → HALF_OPEN",
        "External service calls, API integrations",
        "@CircuitBreaker(name = \"paymentService\", fallbackMethod = \"paymentFallback\") public PaymentResult charge(Order order) { return paymentClient.charge(order.getTotal()); } private PaymentResult paymentFallback(Order order, Exception e) { return PaymentResult.pending(\"Payment service unavailable, queued for retry\"); }",
        "Spring Boot + Resilience4j", 0.94, times_used=40
    ),
    "pat_retry_with_backoff": _pattern(
        "Retry with Exponential Backoff", "RESILIENCE",
        "Retry failed operations with increasing delays to avoid overwhelming the target service",
        "Network calls, API integrations, database connections",
        "@Retry(name = \"externalApi\", fallbackMethod = \"fallback\") @CircuitBreaker(name = \"externalApi\") public Response callApi(Request req) { return client.send(req); } // Config: maxAttempts=3, waitDuration=1s, exponentialBackoffMultiplier=2, retryExceptions=[TimeoutException, IOException]",
        "Java/Spring", 0.95, times_used=55
    ),
    "pat_specification": _pattern(
        "Specification Pattern", "DDD",
        "Encapsulate business rules as reusable, combinable predicates",
        "Complex filtering, validation rules, authorization checks",
        "interface Specification<T> { boolean isSatisfiedBy(T entity); default Specification<T> and(Specification<T> other) { return t -> this.isSatisfiedBy(t) && other.isSatisfiedBy(t); } } class IsActiveUser implements Specification<User> { public boolean isSatisfiedBy(User u) { return u.isActive(); } } // Usage: isActive.and(hasRole(ADMIN)).isSatisfiedBy(user)",
        "Java", 0.92, times_used=30
    ),
    "pat_mediator_cqrs": _pattern(
        "Mediator/CQRS Handler", "ARCHITECTURE",
        "Route commands and queries to their handlers through a mediator, decoupling sender from receiver",
        "CQRS implementations, complex request routing",
        "// Command: record CreateUserCommand(String name, String email) implements Command<UserDTO> {} // Handler: @Component class CreateUserHandler implements CommandHandler<CreateUserCommand, UserDTO> { public UserDTO handle(CreateUserCommand cmd) { User u = new User(cmd.name(), cmd.email()); return toDTO(repo.save(u)); } } // Usage: UserDTO result = mediator.send(new CreateUserCommand(\"John\", \"j@x.com\"));",
        "Java/TypeScript", 0.93, times_used=35
    ),
    "pat_event_bus": _pattern(
        "Event Bus / Domain Events", "ARCHITECTURE",
        "Publish domain events for loose coupling between bounded contexts",
        "Cross-cutting concerns: audit, notifications, cache invalidation",
        "// Event: record UserRegistered(String userId, String email, Instant registeredAt) implements DomainEvent {} // Publisher: eventBus.publish(new UserRegistered(user.getId(), user.getEmail(), Instant.now())); // Listener: @EventListener void onUserRegistered(UserRegistered event) { emailService.sendWelcome(event.email()); auditLog.record(event); analyticsService.track(\"user_registered\", event.userId()); }",
        "Java/Spring", 0.95, times_used=60
    ),
}
