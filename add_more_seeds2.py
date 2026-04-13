import re

with open("src/main/java/org/example/learning/StrategicKnowledgeLearningInitializer.java", "r", encoding="utf-8") as f:
    content = f.read()

additional_seeds_part2 = """

    private void seedUnlimitedEngineeringWisdom() {
        // --- HIGH AVAILABILITY ---
        systemLearningService.recordTechnique(
            "HIGH_AVAILABILITY",
            "Multi-Region Failover Routing",
            "Route traffic to an alternative cloud region immediately if the primary region goes down, ensuring continuous operation.",
            Arrays.asList(
                "Deploy active-passive database replication across regions.",
                "Use Global Load Balancer to detect region failure.",
                "Automatically switch DNS routing to the healthy region."
            ),
            0.96,
            Map.of("evidence_type", "disaster_recovery", "risk_if_ignored", "Complete business outage during a cloud provider region failure.")
        );

        // --- QUEUES & ASYNC ---
        systemLearningService.recordTechnique(
            "MESSAGE_BROKER",
            "Exponential Backoff in Message Consumption",
            "Prevent overwhelming downstream services during partial outages by introducing exponentially increasing delays.",
            Arrays.asList(
                "Capture external API or database timeouts.",
                "Re-queue the message with an incremented 'attempt' count.",
                "Calculate delay: base_delay * (2 ^ attempt).",
                "Move to Dead Letter Queue after max attempts."
            ),
            0.98,
            Map.of("evidence_type", "distributed_systems", "risk_if_ignored", "Thundering herd problem that brings down recovering services.")
        );

        // --- OBSERVABILITY ---
        systemLearningService.recordTechnique(
            "OBSERVABILITY",
            "Distributed Tracing Injection",
            "Propagate a Trace ID across all microservices to track a single user request from ingress to database.",
            Arrays.asList(
                "Generate a UUID at the API Gateway (X-Request-ID).",
                "Include the UUID in all log statements using MDC.",
                "Pass the UUID in headers for downstream HTTP/RPC calls.",
                "Visualize bottlenecks using Jaeger or Zipkin."
            ),
            0.99,
            Map.of("evidence_type", "sre_practice", "risk_if_ignored", "Inability to debug why a specific request is slow across 5 different services.")
        );

        systemLearningService.recordTechnique(
            "OBSERVABILITY",
            "Semantic Alerting Thresholds",
            "Avoid alert fatigue by only triggering PagerDuty for actionable, business-impacting metrics, not raw CPU spikes.",
            Arrays.asList(
                "Monitor High Error Rate (5xx > 1%).",
                "Monitor High Latency (P99 > 2 seconds).",
                "Ignore brief CPU spikes if latency and error rate are normal."
            ),
            0.95,
            Map.of("evidence_type", "sre_alerting", "risk_if_ignored", "Engineers ignore pages because the system constantly alerts on non-issues.")
        );

        // --- STATE MANAGEMENT ---
        systemLearningService.recordTechnique(
            "STATE_MANAGEMENT",
            "Event Sourcing Architecture",
            "Instead of storing the current state, store a sequence of immutable events that led to the state.",
            Arrays.asList(
                "Define events (e.g., 'OrderCreated', 'ItemAdded').",
                "Append events to an append-only log.",
                "Project events into a read-model for fast querying.",
                "Replay events to debug historical states or rebuild read-models."
            ),
            0.85,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Losing the historical context of how data reached its current state.")
        );

        // --- AI SPECIFIC ---
        systemLearningService.recordTechnique(
            "AI_INTEGRATION",
            "Prompt Injection Sanitization",
            "Prevent users from manipulating AI responses by separating user input from system instructions.",
            Arrays.asList(
                "Use distinct system and user message roles.",
                "Filter user input for common jailbreak phrases.",
                "Wrap user input in distinct delimiters (e.g., XML tags).",
                "Validate AI output schema before processing."
            ),
            0.97,
            Map.of("evidence_type", "ai_security", "risk_if_ignored", "Malicious users bypass system constraints or leak system prompts.")
        );

        systemLearningService.recordTechnique(
            "AI_INTEGRATION",
            "LLM Output Schema Enforcement",
            "Never parse raw string output from LLMs. Force structured data (JSON/XML) for predictable processing.",
            Arrays.asList(
                "Instruct the LLM to return strictly valid JSON.",
                "Provide a JSON Schema in the prompt.",
                "Parse the response through a rigid parser.",
                "Retry with error feedback if parsing fails."
            ),
            0.98,
            Map.of("evidence_type", "ai_engineering", "risk_if_ignored", "Application crashes due to unexpected markdown or conversational text in AI response.")
        );

        // --- TESTING ---
        systemLearningService.recordTechnique(
            "TESTING",
            "Property-Based Testing",
            "Generate hundreds of random inputs to test the invariant properties of a function, catching edge cases unit tests miss.",
            Arrays.asList(
                "Define the invariant (e.g., reversing a list twice equals the original list).",
                "Use a library (e.g., jqwik) to generate random lists.",
                "Run the test 1000 times automatically.",
                "Shrink failing inputs to the minimal reproducing case."
            ),
            0.90,
            Map.of("evidence_type", "advanced_testing", "risk_if_ignored", "System fails on unexpected nulls, negative numbers, or empty strings in production.")
        );

        systemLearningService.recordTechnique(
            "TESTING",
            "Chaos Engineering Experiments",
            "Intentionally inject failures (latency, dropped packets, crashed pods) into staging to verify resilience mechanisms.",
            Arrays.asList(
                "Define the steady-state hypothesis.",
                "Introduce an error (e.g., block database port).",
                "Verify that Circuit Breakers and Fallbacks activate.",
                "Restore the system and ensure steady-state returns."
            ),
            0.88,
            Map.of("evidence_type", "resilience_engineering", "risk_if_ignored", "Fallbacks fail in production because they were never tested under real duress.")
        );
    }
"""

idx = content.find("        seedExtendedEngineeringPatterns();")
if idx != -1:
    insert_pos = idx + len("        seedExtendedEngineeringPatterns();")
    content = content[:insert_pos] + "\n        seedUnlimitedEngineeringWisdom();" + content[insert_pos:]
    
    class_end = content.rfind("}")
    content = content[:class_end] + additional_seeds_part2 + "\n}"
    
    with open("src/main/java/org/example/learning/StrategicKnowledgeLearningInitializer.java", "w", encoding="utf-8") as f:
        f.write(content)
