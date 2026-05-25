#!/usr/bin/env python3
"""
Part 3 — Databases & Data Engineering
Seeds SupremeAI Firebase with deep knowledge about:
  • SQL fundamentals (JOINs, indexes, query optimisation, EXPLAIN)
  • PostgreSQL, MySQL specifics
  • NoSQL patterns (Firestore, MongoDB, Redis)
  • Database design (normalisation, denormalisation, schema design)
  • Transactions, ACID, isolation levels
  • Database migrations (Flyway, Liquibase)
  • Connection pooling (HikariCP)
  • Firestore-specific patterns for SupremeAI

Collections written:
  • system_learning     (SystemLearning model records)
  • database_knowledge  (rich topic documents)

Run:
  pip install firebase-admin
  python seed_part3_databases.py [--dry-run]
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from seed_lib import _learning, run_part

# ============================================================================
# SYSTEM_LEARNING records
# ============================================================================

SYSTEM_LEARNINGS = {

    "db_index_design": _learning(
        type_="PATTERN",
        category="DATABASE",
        content=(
            "Index design rules for SQL databases: "
            "(1) Index columns used in WHERE, JOIN ON, and ORDER BY clauses. "
            "(2) Composite index column order matters: put the most selective column first, "
            "then columns used in range predicates last. "
            "(3) Covering index: include all columns needed by a query in the index itself "
            "to avoid hitting the table (INDEX ONLY SCAN). "
            "(4) Partial index: index only rows matching a condition — "
            "CREATE INDEX ON orders(user_id) WHERE status='PENDING'. "
            "(5) Never index low-cardinality columns (boolean, enum with 3 values) alone."
        ),
        solutions=[
            "Run EXPLAIN ANALYZE on slow queries to see which indexes are (or aren't) being used",
            "Create composite index (a, b, c) if queries filter on (a), (a,b), or (a,b,c)",
            "Use pg_stat_user_indexes to find unused indexes that waste write performance",
            "Add covering index: CREATE INDEX COVERING ON orders(user_id) INCLUDE (total, status)",
        ],
        severity="HIGH",
        confidence=0.97,
        times_applied=134,
        context={
            "rule": "Index = faster reads, slower writes — balance based on read:write ratio",
            "tool": "EXPLAIN ANALYZE in PostgreSQL; EXPLAIN FORMAT=JSON in MySQL",
        },
    ),

    "db_n_plus_one": _learning(
        type_="ERROR",
        category="DATABASE",
        content=(
            "N+1 query problem: executing 1 query to fetch N parent records, "
            "then N separate queries to fetch each parent's related records. "
            "Example: fetch 100 orders then for each order fetch its customer = 101 queries. "
            "Symptoms: 100s of near-identical SQL queries in logs; slow API responses; "
            "Spring logs showing repeated SELECT WHERE id=? with incrementing IDs."
        ),
        solutions=[
            "JPA: use JOIN FETCH in JPQL: 'SELECT o FROM Order o JOIN FETCH o.customer'",
            "JPA: @EntityGraph(attributePaths='customer') on repository method",
            "Spring Data JPA: @Query with JOIN FETCH for specific queries",
            "GraphQL: use DataLoader / batch loader to avoid N+1 in resolvers",
            "Firestore: use IN queries (max 30 IDs) or denormalise related data into parent doc",
        ],
        severity="HIGH",
        confidence=0.98,
        error_count=47,
        times_applied=45,
        resolved=True,
        resolution="Add JOIN FETCH or @EntityGraph to eagerly load associations in one SQL query",
        context={
            "detection": "Enable spring.jpa.show-sql=true and count identical queries",
            "hibernate_log": "logging.level.org.hibernate.SQL=DEBUG shows all generated queries",
        },
    ),

    "db_transaction_isolation": _learning(
        type_="PATTERN",
        category="DATABASE",
        content=(
            "Transaction isolation levels (SQL standard): "
            "READ UNCOMMITTED: can read dirty data — almost never correct. "
            "READ COMMITTED: default in most DBs — can get non-repeatable reads. "
            "REPEATABLE READ: same query returns same rows in a transaction — MySQL InnoDB default. "
            "SERIALIZABLE: full isolation, highest consistency, lowest concurrency. "
            "Phenomenon prevented by each level: "
            "dirty read (UC), non-repeatable read (RC), phantom read (RR). "
            "PostgreSQL bonus: MVCC gives snapshot isolation at READ COMMITTED level."
        ),
        solutions=[
            "Use @Transactional(isolation=REPEATABLE_READ) for financial calculations",
            "Use @Transactional(isolation=SERIALIZABLE) for inventory reservation (prevent oversell)",
            "Prefer optimistic locking (@Version) over high isolation for low-contention scenarios",
            "READ COMMITTED (default) is correct for most web applications",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=67,
        context={
            "postgres_default": "READ COMMITTED with MVCC — effectively snapshot isolation",
            "mysql_default": "REPEATABLE READ with gap locks",
        },
    ),

    "db_connection_pooling_hikari": _learning(
        type_="PATTERN",
        category="DATABASE",
        content=(
            "HikariCP is Spring Boot's default connection pool (fastest Java pool). "
            "Key settings: "
            "maximumPoolSize: CPU count × 2 + active disk spindles (for web apps: 10-20). "
            "minimumIdle: match maximumPoolSize to avoid pool size fluctuation. "
            "connectionTimeout: 30000ms default — time to wait for connection from pool. "
            "maxLifetime: 1800000ms (30 min) — recycle connections before DB kills them. "
            "keepaliveTime: 60000ms — send keepalive query to prevent idle connection timeout."
        ),
        solutions=[
            "Set maximumPoolSize=10 as starting point; increase if you see connection timeout errors",
            "Monitor pool with Micrometer + Actuator: hikaricp.connections.active metric",
            "Set connectionTimeout=5000 for responsive failure detection instead of default 30s",
            "Use HikariDataSource.getHikariConfigMXBean() for runtime pool tuning without restart",
            "Cloud Run: max pool size = max concurrent requests / services in cluster",
        ],
        severity="HIGH",
        confidence=0.96,
        times_applied=88,
        context={
            "property_prefix": "spring.datasource.hikari.*",
            "pool_size_formula": "Ideal pool size = ((core_count * 2) + effective_spindle_count)",
        },
    ),

    "db_firestore_data_model": _learning(
        type_="PATTERN",
        category="FIRESTORE",
        content=(
            "Firestore data modelling rules: "
            "(1) Denormalise for reads — store data together that is read together. "
            "(2) Subcollections vs embedded arrays: subcollections for large/growing lists; "
            "arrays for small static lists (< 20 items). "
            "(3) Avoid deeply nested paths > 3 levels. "
            "(4) Queries are limited: no joins, no inequality on multiple fields, "
            "no full-text search. "
            "(5) Composite indexes required for queries with multiple WHERE clauses. "
            "(6) Document size limit: 1MB per document. "
            "(7) Write rate per document: 1 write/second (use batching + sharded counters for high-write)."
        ),
        solutions=[
            "Co-locate data that is always read together in the same document",
            "For counters (likes, views): use distributed counters (10+ shards) to exceed 1 write/s limit",
            "Use Firestore transactions for read-then-write operations (atomic updates)",
            "Add composite index for every combination of WHERE + ORDER BY fields",
            "Use collection group queries for querying across all subcollections with the same name",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=112,
        context={
            "pricing": "Charged per document read/write/delete — minimise documents read per request",
            "tool": "Firebase Console → Firestore → Indexes for composite index management",
        },
    ),

    "db_flyway_migrations": _learning(
        type_="PATTERN",
        category="DATABASE",
        content=(
            "Flyway versioned migration best practices: "
            "Naming: V{version}__{description}.sql — V1__create_users_table.sql. "
            "Never edit an existing applied migration — create a new version. "
            "Repeatable migrations (R__) for stored procedures and views that change. "
            "Undo migrations (U__) for Flyway Pro — define rollback SQL. "
            "Error recovery: 'flyway repair' fixes checksum mismatches from accidental edits. "
            "CI/CD: run 'flyway migrate' before starting the application; "
            "Spring Boot runs it automatically on startup (spring.flyway.enabled=true)."
        ),
        solutions=[
            "Never edit V*.sql after it has been applied to any environment",
            "Test migrations in a clean database before merging to main branch",
            "Add 'spring.flyway.out-of-order=false' to enforce migration ordering",
            "For big schema changes: use multiple small migrations, not one giant one",
            "Baseline existing database: flyway baseline with current state before enabling Flyway",
        ],
        severity="HIGH",
        confidence=0.97,
        times_applied=78,
        context={
            "spring_config": "spring.flyway.enabled=true (default), spring.flyway.locations=classpath:db/migration",
            "repair_command": "./mvnw flyway:repair -Dflyway.url=... -Dflyway.user=... -Dflyway.password=...",
        },
    ),

    "db_postgres_performance": _learning(
        type_="PATTERN",
        category="DATABASE",
        content=(
            "PostgreSQL performance tuning: "
            "shared_buffers: 25% of total RAM — primary data cache. "
            "effective_cache_size: 75% of total RAM — planner estimate of OS cache. "
            "work_mem: RAM per sort/hash operation — increase for slow ORDER BY/GROUP BY. "
            "max_connections: 100–200; use PgBouncer for >200 concurrent connections. "
            "autovacuum: keep default but monitor pg_stat_user_tables.n_dead_tup; "
            "dead tuples bloat tables and slow queries. "
            "VACUUM ANALYZE weekly on heavily updated tables."
        ),
        solutions=[
            "Run pg_tune for instant config recommendations based on server RAM and disk",
            "Monitor slow queries: pg_stat_statements extension + log_min_duration_statement=1000",
            "Use EXPLAIN (ANALYZE, BUFFERS) to see buffer hits vs disk reads",
            "Add PgBouncer in transaction mode for connection pooling beyond 100 connections",
            "Partition large tables (>10M rows) by range or list to prune partitions on query",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=55,
        context={
            "tool": "pgBadger for log analysis; pganalyze for cloud monitoring",
            "quick_win": "ANALYZE <table> rebuilds statistics after bulk INSERT — fixes bad query plans",
        },
    ),

    "db_redis_patterns": _learning(
        type_="PATTERN",
        category="NOSQL",
        content=(
            "Redis usage patterns for Spring Boot applications: "
            "(1) Cache-aside: check Redis first; on miss, read DB, write to Redis with TTL. "
            "(2) Session store: spring-session-data-redis replaces HTTP session with Redis. "
            "(3) Rate limiting: INCR + EXPIRE per user IP/key for sliding window counting. "
            "(4) Pub/Sub: lightweight message bus for same-machine or low-latency events. "
            "(5) Distributed lock: SETNX + EXPIRE (or Redisson RLock) for cross-node locking. "
            "(6) Leaderboard: ZADD + ZRANGE with sorted sets for real-time ranking."
        ),
        solutions=[
            "Use @Cacheable/@CacheEvict with spring-boot-starter-data-redis for transparent caching",
            "Always set TTL — never cache indefinitely (data can become stale)",
            "Use Redisson for distributed locks — handles expiry and re-entry correctly",
            "Monitor hit rate: cache_hits/(cache_hits+cache_misses) > 80% is target",
            "Use Redis Cluster for HA; set maxmemory-policy=allkeys-lru for cache eviction",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=97,
        context={
            "spring_config": "spring.data.redis.host, spring.cache.type=redis, spring.cache.redis.time-to-live=3600000",
            "data_structures": "String(cache), Hash(object), List(queue), Set(unique), ZSet(ranking)",
        },
    ),

    "db_sql_query_optimisation": _learning(
        type_="PATTERN",
        category="DATABASE",
        content=(
            "SQL query optimisation checklist: "
            "(1) Use SELECT only needed columns — avoid SELECT *. "
            "(2) Filter early: apply WHERE conditions before JOIN (use subqueries or CTEs). "
            "(3) Use EXISTS instead of IN for subqueries with large result sets. "
            "(4) Avoid functions on indexed columns in WHERE: WHERE YEAR(created_at)=2024 "
            "prevents index use — rewrite as WHERE created_at BETWEEN ... AND ... "
            "(5) Paginate with keyset (cursor) pagination instead of OFFSET for large datasets. "
            "(6) Use LIMIT early in subqueries to reduce intermediate result sizes."
        ),
        solutions=[
            "Replace SELECT * with explicit column list — reduces network transfer and prevents unnecessary data loading",
            "Replace WHERE id IN (SELECT id FROM ...) with EXISTS for better performance",
            "Replace OFFSET pagination with WHERE id > :lastId LIMIT 20 for consistent performance",
            "Add composite index to cover multi-column WHERE and ORDER BY in one scan",
            "Use CTEs (WITH clause) for readability but note CTEs may materialise in PostgreSQL",
        ],
        severity="HIGH",
        confidence=0.96,
        times_applied=111,
        context={
            "explain_output": "Seq Scan = no index; Index Scan = index used; Index Only Scan = covering index",
        },
    ),

    "db_mongodb_patterns": _learning(
        type_="PATTERN",
        category="NOSQL",
        content=(
            "MongoDB data modelling patterns: "
            "Embed vs Reference: embed data that is always read together and does not grow unboundedly. "
            "Bucket pattern: pre-aggregate time-series data into buckets (e.g., hourly buckets of sensor readings) "
            "to reduce document count and enable fast range queries. "
            "Computed pattern: pre-compute expensive fields on write to avoid on-read computation. "
            "Schema versioning: include 'schema_version' field in documents; "
            "handle multiple versions in application code during migration."
        ),
        solutions=[
            "Use embedded documents for 1:1 and 1:few relationships with bounded size",
            "Use DBRef/manual reference for 1:many or many:many relationships",
            "Create compound indexes on fields used together in queries",
            "Use aggregation pipeline instead of multiple queries for complex transformations",
            "Use change streams for real-time event-driven updates (replaces polling)",
        ],
        severity="MEDIUM",
        confidence=0.92,
        times_applied=43,
        context={"tip": "MongoDB 16MB document limit — for large embeds consider GridFS"},
    ),

    "db_optimistic_locking": _learning(
        type_="PATTERN",
        category="DATABASE",
        content=(
            "Optimistic locking: assume conflicts are rare; detect on write instead of locking on read. "
            "JPA implementation: add @Version Integer version field to entity. "
            "On UPDATE: WHERE id=? AND version=? — if 0 rows updated, another transaction won. "
            "JPA throws OptimisticLockException when version mismatch detected. "
            "Better than pessimistic locking for most web apps: higher throughput, no deadlocks. "
            "Use pessimistic locking (@Lock(PESSIMISTIC_WRITE)) only for financial operations "
            "where lost update = data corruption."
        ),
        solutions=[
            "Add @Version private Integer version; to any JPA entity with concurrent updates",
            "Catch OptimisticLockException in service layer; retry or return 409 Conflict to client",
            "For REST APIs: use ETag header based on entity version for HTTP-level optimistic locking",
            "In Firestore: use transactions (runTransaction) for optimistic concurrency",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=59,
        context={"spring_jpa": "@Version annotation on Long or Integer field; Spring Data handles everything"},
    ),

    "improvement_db_query_monitoring": _learning(
        type_="IMPROVEMENT",
        category="DATABASE",
        content=(
            "Database query monitoring best practices: "
            "Enable Spring Boot slow query logging: spring.jpa.show-sql=true for dev; "
            "logging.level.org.hibernate.SQL=DEBUG. "
            "For production: use p6spy or datasource-proxy to log slow queries (>500ms) only. "
            "PostgreSQL: enable pg_stat_statements; query pg_stat_statements view for "
            "top queries by total time. "
            "Set alert: 95th percentile query latency > 500ms → investigate indexes."
        ),
        solutions=[
            "Add datasource-proxy to log slow queries with parameters in production",
            "Query pg_stat_statements WHERE mean_exec_time > 100 ORDER BY total_exec_time DESC",
            "Set up Grafana dashboard for: queries/sec, slow query count, connection pool usage",
            "Add @Timed on Spring Data repository methods to capture per-method latency",
        ],
        severity="MEDIUM",
        confidence=0.92,
        times_applied=67,
        context={"tool": "p6spy, datasource-proxy, pgBadger, pganalyze, Datadog APM"},
    ),
}

# ============================================================================
# DATABASE_KNOWLEDGE rich topic documents
# ============================================================================

DATABASE_KNOWLEDGE_DOCS = {

    "sql_fundamentals": {
        "topic": "SQL Fundamentals — Complete Reference",
        "category": "SQL",
        "description": "Core SQL concepts every backend developer must master.",
        "join_types": {
            "INNER JOIN": "Rows matching in both tables — most common",
            "LEFT JOIN": "All rows from left + matching right; NULLs for no match in right",
            "RIGHT JOIN": "All rows from right + matching left; NULLs for no match in left",
            "FULL OUTER JOIN": "All rows from both tables; NULLs where no match",
            "CROSS JOIN": "Cartesian product — all combinations; use with care (N×M rows)",
            "SELF JOIN": "Join table to itself; useful for hierarchical data (employee → manager)",
        },
        "aggregate_functions": ["COUNT(*)", "SUM(col)", "AVG(col)", "MIN(col)", "MAX(col)", "GROUP_CONCAT / STRING_AGG"],
        "window_functions": {
            "ROW_NUMBER()": "Unique sequential number per partition",
            "RANK()": "Rank with gaps on ties",
            "DENSE_RANK()": "Rank without gaps on ties",
            "LAG(col, n)": "Value from n rows before current",
            "LEAD(col, n)": "Value from n rows after current",
            "SUM() OVER (PARTITION BY ...)": "Running total within partition",
        },
        "indexes": {
            "B-Tree": "Default; great for equality and range queries",
            "Hash": "Perfect for equality only; faster than B-Tree for =",
            "GIN": "Full-text search, JSONB, arrays",
            "GiST": "Geometric, full-text, range types",
            "BRIN": "Huge tables with natural physical ordering (timestamps)",
            "Partial": "Index only rows matching a condition — smaller, faster",
            "Covering": "Include extra columns to avoid table lookup (INDEX ONLY SCAN)",
        },
        "best_practices": [
            "EXPLAIN ANALYZE before and after index changes",
            "Never SELECT * in production queries",
            "Use parameterised queries / PreparedStatement — prevents SQL injection",
            "Paginate with keyset (cursor) not OFFSET for tables > 100k rows",
            "Use RETURNING clause (PostgreSQL) to avoid separate SELECT after INSERT",
        ],
        "confidence": 0.97,
    },

    "nosql_comparison": {
        "topic": "NoSQL Database Comparison & Selection Guide",
        "category": "NOSQL",
        "description": "Choosing the right NoSQL database for your use case.",
        "databases": {
            "Firestore": {
                "type": "Document store",
                "strengths": "Mobile/web real-time sync, offline support, serverless, Firebase integration",
                "weaknesses": "No JOIN, no full-text search, 1 write/sec per document limit",
                "use_when": "Mobile apps, real-time dashboards, SupremeAI primary store",
                "query_power": "Low — equality filters, range on one field, array contains",
            },
            "MongoDB": {
                "type": "Document store",
                "strengths": "Flexible schema, powerful aggregation pipeline, full-text search",
                "weaknesses": "No ACID multi-document by default (4.0+: transactions), memory heavy",
                "use_when": "Product catalogs, user profiles, content management",
                "query_power": "High — rich query language + aggregation pipeline",
            },
            "Redis": {
                "type": "Key-value / data structure store",
                "strengths": "Sub-millisecond latency, rich data structures, pub/sub",
                "weaknesses": "Limited query capability, in-memory (expensive for large data)",
                "use_when": "Cache, session store, rate limiting, leaderboards, pub/sub",
                "query_power": "Very low — key access only (with patterns via SCAN)",
            },
            "Elasticsearch": {
                "type": "Search engine / document store",
                "strengths": "Full-text search, complex aggregations, log analysis",
                "weaknesses": "Not for primary store, complex operations, eventually consistent",
                "use_when": "Search, log analytics, product search with facets",
                "query_power": "Very high — full-text, geo, aggregations",
            },
            "Cassandra": {
                "type": "Wide-column store",
                "strengths": "Linear scalability, high write throughput, multi-region",
                "weaknesses": "No joins, model queries first (schema driven by query patterns)",
                "use_when": "Time-series, IoT, chat message history at massive scale",
                "query_power": "Low — primary key + clustering key only",
            },
        },
        "selection_guide": {
            "need_real_time_sync": "Firestore",
            "need_full_text_search": "Elasticsearch (or Algolia)",
            "need_cache": "Redis",
            "need_flexible_schema_complex_queries": "MongoDB",
            "need_high_write_scale": "Cassandra or Kafka + time-series DB",
            "need_relational_with_ACID": "PostgreSQL",
        },
        "confidence": 0.94,
    },

    "firestore_best_practices": {
        "topic": "Firestore Best Practices for SupremeAI",
        "category": "FIRESTORE",
        "description": (
            "Firestore is SupremeAI's primary database. These practices are essential "
            "for correct, performant, and cost-efficient operation."
        ),
        "data_modelling_rules": [
            "Co-locate data that is read together in the same document",
            "Use subcollections for 1:many relationships with unbounded growth",
            "Embed arrays for small bounded lists (< 20 items)",
            "Never embed documents that are updated frequently by independent processes",
            "Include denormalised copies of frequently-read foreign fields (user displayName in posts)",
        ],
        "query_limitations": [
            "No SQL JOIN — model data to avoid joins",
            "Inequality filter only on ONE field per query",
            "No OR queries — run two queries and merge in code",
            "No full-text search — use Algolia or Elasticsearch as search layer",
            "Array CONTAINS can check one value per query",
        ],
        "security_rules_examples": {
            "authenticated_users_only": "allow read: if request.auth != null;",
            "owner_only_write": "allow write: if request.auth.uid == resource.data.userId;",
            "field_validation": "allow write: if request.resource.data.score is number && request.resource.data.score >= 0;",
            "admin_check": "allow write: if request.auth.token.admin == true;",
        },
        "cost_optimisation": [
            "Read specific fields with .select(['field1', 'field2']) to reduce read bandwidth",
            "Use onSnapshot listeners instead of polling — charged once per update, not per poll",
            "Bundle initial data loads with Firebase Hosting to reduce first-page Firestore reads",
            "Use offline persistence for mobile — reduces reads on reconnect",
            "Cache frequently-read config documents in application memory (TTL 5-10 mins)",
        ],
        "collections_in_supreme_ai": {
            "system_learning": "SystemLearning model records — core AI knowledge",
            "patterns": "Architecture & design patterns",
            "generation_errors_and_fixes": "Error-fix pairs",
            "copilot_workflow": "Step-by-step AI workflow",
            "copilot_error_detection": "Error detection methods",
            "ai_fundamentals": "LLM & AI knowledge (Part 1)",
            "software_architecture": "Architecture patterns (Part 2)",
            "database_knowledge": "Database patterns (Part 3)",
            "security_knowledge": "Security patterns (Part 4)",
            "devops_knowledge": "DevOps & Cloud patterns (Part 5)",
        },
        "confidence": 0.96,
    },

    "database_design_guide": {
        "topic": "Database Design — Normalisation to Schema Design",
        "category": "DATABASE_DESIGN",
        "description": "Designing databases for correctness, performance, and maintainability.",
        "normal_forms": {
            "1NF": "Atomic values, no repeating groups, primary key defined",
            "2NF": "1NF + no partial dependencies on composite primary key",
            "3NF": "2NF + no transitive dependencies (non-key depends on non-key)",
            "BCNF": "3NF + every determinant is a candidate key",
            "when_to_denormalise": "When query performance requires it; document the denormalisation and maintain consistency in application code",
        },
        "schema_design_tips": [
            "Every table needs a primary key (surrogate UUID or BIGSERIAL — avoid composite PKs for FKs)",
            "Add created_at TIMESTAMPTZ NOT NULL DEFAULT NOW() and updated_at to every entity table",
            "Add soft delete: deleted_at TIMESTAMPTZ nullable + partial index WHERE deleted_at IS NULL",
            "Use CHECK constraints to enforce domain rules at DB level (status IN ('ACTIVE','INACTIVE'))",
            "Foreign keys should always have an index on the FK column for JOIN performance",
        ],
        "anti_patterns": [
            "EAV (Entity-Attribute-Value): terrible query performance; use JSONB instead",
            "Storing comma-separated lists in a column — use junction table or array",
            "Using application-layer UUIDs as surrogate keys without an index",
            "SELECT * in ORM queries causing unnecessary column reads",
            "Mixing timezones — always store UTC; convert on display",
        ],
        "postgresql_tips": [
            "Use TIMESTAMPTZ (with timezone) not TIMESTAMP for all datetime columns",
            "Use JSONB not JSON for queryable semi-structured data",
            "Use ENUM type for fixed categorical values (faster than VARCHAR)",
            "Use BIGSERIAL or gen_random_uuid() for primary keys",
            "Enable pg_trgm for LIKE search acceleration",
        ],
        "confidence": 0.95,
    },

    "database_transactions_guide": {
        "topic": "Transactions, ACID, and Concurrency Control",
        "category": "DATABASE",
        "description": "Understanding transactions for correct concurrent data management.",
        "acid_properties": {
            "Atomicity": "All operations succeed or all are rolled back — no partial updates",
            "Consistency": "Transaction brings DB from one valid state to another — constraints maintained",
            "Isolation": "Concurrent transactions execute as if serialised — no dirty reads between",
            "Durability": "Committed data survives crashes — written to durable storage",
        },
        "isolation_levels": {
            "READ_UNCOMMITTED": "Can read uncommitted data (dirty reads) — almost never correct",
            "READ_COMMITTED": "Default in most DBs — no dirty reads; non-repeatable reads possible",
            "REPEATABLE_READ": "MySQL InnoDB default — no non-repeatable reads; phantom reads possible",
            "SERIALIZABLE": "Full isolation — all phenomena prevented; lowest concurrency",
        },
        "spring_transactional": {
            "annotation": "@Transactional",
            "propagation_required": "REQUIRED (default) — join existing or create new transaction",
            "propagation_new": "REQUIRES_NEW — always create new transaction (suspends current)",
            "read_only": "readOnly=true on query methods — enables DB optimisations, no flush",
            "rollback_for": "rollbackFor=Exception.class — roll back on checked exceptions too",
            "pitfall": "@Transactional only works when called through Spring proxy — self-calls bypass it",
        },
        "concurrency_control": {
            "optimistic": "@Version field — detect conflicts on write; best for low-contention",
            "pessimistic": "@Lock(PESSIMISTIC_WRITE) — DB-level row lock; best for financial writes",
            "distributed_lock": "Redisson RLock — cross-node locking for distributed systems",
        },
        "confidence": 0.96,
    },

    "migration_strategies": {
        "topic": "Database Migration Strategies",
        "category": "DATABASE_MIGRATIONS",
        "description": "Safe strategies for evolving database schema in production.",
        "tools": {
            "Flyway": "SQL-based migrations; V__description.sql naming; Spring Boot auto-run",
            "Liquibase": "XML/YAML/SQL changesets; rollback support; more complex but more features",
        },
        "safe_migration_patterns": {
            "add_column": "Always nullable or with DEFAULT — zero downtime",
            "rename_column": "Expand: add new column; contract: migrate data; remove old column over 3 deployments",
            "drop_column": "First deploy: stop using column; second deploy: drop column",
            "add_index": "CREATE INDEX CONCURRENTLY (PostgreSQL) — non-blocking index build",
            "change_type": "Add new column with correct type; migrate data; switch app; drop old column",
        },
        "zero_downtime_migration": [
            "Backward-compatible migration first (add nullable columns, add indexes CONCURRENTLY)",
            "Deploy new code that works with both old and new schema",
            "Run data migration as a background job",
            "Remove old schema elements in the next release",
        ],
        "flyway_tips": [
            "V1__initial_schema.sql, V2__add_user_email_index.sql (sequential integers)",
            "Always test migrations against a copy of production data before applying",
            "Use flyway:validate in CI to detect unapplied/modified migrations",
            "Run flyway:info to see status of all migrations",
        ],
        "confidence": 0.94,
    },
}

# ============================================================================
# ENTRY POINT
# ============================================================================

if __name__ == "__main__":
    run_part(
        part_name="Part 3 — Databases & Data Engineering",
        collections={
            "system_learning": SYSTEM_LEARNINGS,
            "database_knowledge": DATABASE_KNOWLEDGE_DOCS,
        },
    )
