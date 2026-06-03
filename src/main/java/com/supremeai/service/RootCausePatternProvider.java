package com.supremeai.service;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class RootCausePatternProvider {

    private static final Pattern IMPORT_PATTERN = Pattern.compile("cannot find symbol|unresolved symbol|Import.*not found");

    public Map<String, RootCausePattern> providePatterns() {
        Map<String, RootCausePattern> rootCausePatterns = new ConcurrentHashMap<>();

        // ── Original 6 ──────────────────────────────────────────────────────

        // Null Pointer Issues
        rootCausePatterns.put("null_pointer", new RootCausePattern(
            "null_pointer",
            "Null Pointer Exception",
            Pattern.compile("NullPointer|null"),
            CorrectionAction.AUTO_FIX_NULL,
            0.9
        ));

        // Missing Import/Dependency
        rootCausePatterns.put("missing_import", new RootCausePattern(
            "missing_import",
            "Missing Import or Dependency",
            IMPORT_PATTERN,
            CorrectionAction.ADD_IMPORT,
            0.85
        ));

        // Syntax Errors
        rootCausePatterns.put("syntax_error", new RootCausePattern(
            "syntax_error",
            "Syntax Error",
            Pattern.compile("syntax error|unexpected token|missing semicolon"),
            CorrectionAction.FIX_SYNTAX,
            0.95
        ));

        // Type Mismatch
        rootCausePatterns.put("type_mismatch", new RootCausePattern(
            "type_mismatch",
            "Type Mismatch",
            Pattern.compile("incompatible types|cannot be applied to|type mismatch"),
            CorrectionAction.FIX_TYPE,
            0.8
        ));

        // Division by Zero
        rootCausePatterns.put("division_by_zero", new RootCausePattern(
            "division_by_zero",
            "Division by Zero",
            Pattern.compile("/ 0|division by zero|ArithmeticException"),
            CorrectionAction.ADD_ZERO_CHECK,
            0.98
        ));

        // Array Out of Bounds
        rootCausePatterns.put("array_out_of_bounds", new RootCausePattern(
            "array_out_of_bounds",
            "Array Index Out of Bounds",
            Pattern.compile("ArrayIndexOutOfBounds|index out of range"),
            CorrectionAction.ADD_BOUNDS_CHECK,
            0.95
        ));

        // ── New patterns ─────────────────────────────────────────────────────

        // Firestore connection failure
        rootCausePatterns.put("firestore_unavailable", new RootCausePattern(
            "firestore_unavailable",
            "Firestore UNAVAILABLE or DEADLINE_EXCEEDED",
            Pattern.compile("UNAVAILABLE|DEADLINE_EXCEEDED|StatusRuntimeException|grpc"),
            CorrectionAction.MANUAL_REVIEW,
            0.88
        ));

        // JWT / Authentication failure
        rootCausePatterns.put("jwt_auth_failure", new RootCausePattern(
            "jwt_auth_failure",
            "JWT Authentication Failure (401 / token expired)",
            Pattern.compile("(?i)jwt|token.*expired|invalid.*token|401|JwtException|SignatureException"),
            CorrectionAction.MANUAL_REVIEW,
            0.90
        ));

        // CORS error
        rootCausePatterns.put("cors_error", new RootCausePattern(
            "cors_error",
            "CORS Policy Violation",
            Pattern.compile("(?i)cors|Access-Control-Allow-Origin|blocked.*origin|cross.origin"),
            CorrectionAction.MANUAL_REVIEW,
            0.92
        ));

        // Spring Bean creation failure
        rootCausePatterns.put("bean_creation", new RootCausePattern(
            "bean_creation",
            "Spring BeanCreationException or UnsatisfiedDependency",
            Pattern.compile("BeanCreationException|UnsatisfiedDependencyException|Error creating bean"),
            CorrectionAction.MANUAL_REVIEW,
            0.93
        ));

        // Out of Memory
        rootCausePatterns.put("out_of_memory", new RootCausePattern(
            "out_of_memory",
            "OutOfMemoryError — Heap or Metaspace exhausted",
            Pattern.compile("OutOfMemoryError|Java heap space|Metaspace|GC overhead limit"),
            CorrectionAction.MANUAL_REVIEW,
            0.97
        ));

        // Thread deadlock
        rootCausePatterns.put("deadlock", new RootCausePattern(
            "deadlock",
            "Thread Deadlock — threads waiting on each other",
            Pattern.compile("(?i)deadlock|thread.*blocked|waiting to lock|BLOCKED"),
            CorrectionAction.MANUAL_REVIEW,
            0.85
        ));

        // GC overhead
        rootCausePatterns.put("gc_overhead", new RootCausePattern(
            "gc_overhead",
            "GC Overhead Limit Exceeded — insufficient heap",
            Pattern.compile("GC overhead|garbage collection.*overhead"),
            CorrectionAction.MANUAL_REVIEW,
            0.94
        ));

        // Rate limit / 429
        rootCausePatterns.put("rate_limit", new RootCausePattern(
            "rate_limit",
            "Rate Limit Exceeded (HTTP 429)",
            Pattern.compile("429|rate limit|Too Many Requests|quota.*exceeded"),
            CorrectionAction.MANUAL_REVIEW,
            0.96
        ));

        // Timeout (provider / network)
        rootCausePatterns.put("timeout", new RootCausePattern(
            "timeout",
            "Timeout — network, provider, or DB connection",
            Pattern.compile("TimeoutException|SocketTimeout|ReadTimeout|ConnectTimeout|504"),
            CorrectionAction.MANUAL_REVIEW,
            0.91
        ));

        // Serialization / JSON parse error
        rootCausePatterns.put("serialization_error", new RootCausePattern(
            "serialization_error",
            "JSON Serialization / Deserialization Failure",
            Pattern.compile("JsonParseException|MismatchedInputException|InvalidDefinitionException|cannot deserialize"),
            CorrectionAction.MANUAL_REVIEW,
            0.89
        ));

        // Connection pool exhausted
        rootCausePatterns.put("connection_pool", new RootCausePattern(
            "connection_pool",
            "Connection Pool Exhausted",
            Pattern.compile("pool.*timeout|Unable to acquire.*connection|connection pool"),
            CorrectionAction.MANUAL_REVIEW,
            0.87
        ));

        // Missing property / config
        rootCausePatterns.put("missing_config", new RootCausePattern(
            "missing_config",
            "Missing Required Configuration Property",
            Pattern.compile("Could not resolve placeholder|property.*not found|Missing environment variable"),
            CorrectionAction.MANUAL_REVIEW,
            0.92
        ));

        // Stack overflow (infinite recursion)
        rootCausePatterns.put("stack_overflow", new RootCausePattern(
            "stack_overflow",
            "StackOverflowError — infinite recursion",
            Pattern.compile("StackOverflowError|infinite recursion"),
            CorrectionAction.MANUAL_REVIEW,
            0.97
        ));

        // HTTP 500 (unhandled exception in controller)
        rootCausePatterns.put("http_500", new RootCausePattern(
            "http_500",
            "HTTP 500 — unhandled exception in REST controller",
            Pattern.compile("HTTP 500|Internal Server Error|WhiteLabel Error|No message available"),
            CorrectionAction.MANUAL_REVIEW,
            0.83
        ));

        return rootCausePatterns;
    }
}
