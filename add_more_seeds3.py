import re

with open("src/main/java/org/example/learning/StrategicKnowledgeLearningInitializer.java", "r", encoding="utf-8") as f:
    content = f.read()

additional_seeds_part3 = """

    private void seedAutonomousEngineeringMastery() {
        // --- AI ORCHESTRATION ---
        systemLearningService.recordTechnique(
            "AI_ORCHESTRATION",
            "Context Window Compression Strategy",
            "When logs or source code exceed LLM context limits, summarize older historical context rather than truncating the end (which usually contains the actual error).",
            Arrays.asList(
                "Detect when payload tokens exceed 80% of model limit.",
                "Extract the top 100 lines (setup) and bottom 200 lines (stacktrace/error).",
                "Summarize the middle section into a brief structural description.",
                "Feed the compressed payload to the model."
            ),
            0.96,
            Map.of("evidence_type", "ai_engineering", "risk_if_ignored", "Model hallucinates fixes because the actual error message was truncated.")
        );

        systemLearningService.recordTechnique(
            "AI_ORCHESTRATION",
            "Multi-Agent Consensus Degradation",
            "If multiple AI models cannot reach consensus on a critical architecture decision, safely degrade to the highest-confidence solo model rather than blocking progress.",
            Arrays.asList(
                "Trigger MultiAIConsensusService with a timeout.",
                "If votes are tied or confidence is low, identify the model with the highest historical success rate for this category.",
                "Log the consensus failure and proceed with the solo model's choice."
            ),
            0.94,
            Map.of("evidence_type", "autonomous_systems", "risk_if_ignored", "System enters indefinite WAIT mode due to minor disagreements between models.")
        );

        // --- CODE GENERATION ---
        systemLearningService.recordTechnique(
            "CODE_GENERATION",
            "Iterative Compilation Feedback Loop",
            "Compile the codebase after every major component is generated, rather than waiting for the entire project to be generated.",
            Arrays.asList(
                "Generate Domain Models -> Compile.",
                "Generate Repositories -> Compile.",
                "Generate Services -> Compile.",
                "Feed compilation errors immediately back to the CodeGenerator before moving to the next layer."
            ),
            0.98,
            Map.of("evidence_type", "compiler_theory", "risk_if_ignored", "Accumulating 50 compilation errors across 10 files makes it impossible for the AI to fix them all at once.")
        );

        systemLearningService.recordTechnique(
            "CODE_GENERATION",
            "AST-Based Code Modification",
            "Parse code into an Abstract Syntax Tree (AST) to make targeted structural changes rather than relying on brittle regex or line-number string replacements.",
            Arrays.asList(
                "Use a language-specific parser (e.g., JavaParser, Babel).",
                "Locate the target MethodDeclaration or ClassDeclaration.",
                "Apply the modification to the AST node.",
                "Write the AST back to source code."
            ),
            0.93,
            Map.of("evidence_type", "static_analysis", "risk_if_ignored", "Regex replaces the wrong variable or corrupts syntax, causing cascading build failures.")
        );

        // --- SECURITY ---
        systemLearningService.recordTechnique(
            "SECURITY",
            "Ephemeral Build Sandboxing",
            "Always execute AI-generated code, tests, and builds in strictly isolated, ephemeral environments with zero access to internal metadata services.",
            Arrays.asList(
                "Spin up a temporary Docker container for the build.",
                "Disable network access to 169.254.169.254 (Cloud Metadata).",
                "Execute the build/test command.",
                "Destroy the container immediately after, regardless of success or failure."
            ),
            0.99,
            Map.of("evidence_type", "devsecops_practice", "risk_if_ignored", "Maliciously generated code extracts cloud credentials and exfiltrates them.")
        );

        // --- ARCHITECTURE ---
        systemLearningService.recordTechnique(
            "ARCHITECTURE",
            "Saga Pattern for Distributed Transactions",
            "Use compensating transactions to undo previous steps if a multi-service workflow fails, rather than relying on distributed locking.",
            Arrays.asList(
                "Define a sequence of local transactions.",
                "For each step, define a compensating action (e.g., 'Reserve Inventory' -> 'Release Inventory').",
                "If step N fails, trigger compensating actions for steps N-1 down to 1.",
                "Record the final state in a Saga execution log."
            ),
            0.95,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Distributed deadlocks or partial data inconsistencies across microservices.")
        );

        systemLearningService.recordTechnique(
            "ARCHITECTURE",
            "Strangler Fig Pattern",
            "Safely replace legacy monolithic code by intercepting calls at the edge and incrementally routing specific features to new microservices.",
            Arrays.asList(
                "Place an API Gateway in front of the legacy system.",
                "Build the new service alongside the legacy one.",
                "Configure the gateway to route traffic for specific endpoints to the new service.",
                "Gradually increase the routed endpoints until the legacy system can be decommissioned."
            ),
            0.97,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Big-bang rewrites fail because they attempt to replace too much complexity at once.")
        );

        // --- DEPLOYMENT ---
        systemLearningService.recordTechnique(
            "DEPLOYMENT",
            "Backward Compatible Schema Migrations",
            "Never drop a database column or table in the same deployment where the application code stops using it. Separate into two phases.",
            Arrays.asList(
                "Phase 1: Deploy code that ignores the deprecated column.",
                "Ensure all active instances of the app are running the new code.",
                "Phase 2: In a subsequent deployment, safely DROP the column."
            ),
            0.98,
            Map.of("evidence_type", "database_management", "risk_if_ignored", "Rolling deployments cause application crashes because old instances query a dropped column.")
        );

        // --- DATABASE ---
        systemLearningService.recordTechnique(
            "DATABASE_MANAGEMENT",
            "Connection Leak Active Detection",
            "Do not rely solely on application logic to close connections. Configure the pool to detect and kill abandoned connections.",
            Arrays.asList(
                "Configure connection pool (e.g., HikariCP) with leakDetectionThreshold.",
                "Set threshold to a reasonable maximum query time (e.g., 30000ms).",
                "Log the stack trace of the thread that originally checked out the leaked connection.",
                "Forcibly close the connection to prevent pool exhaustion."
            ),
            0.96,
            Map.of("evidence_type", "reliability_engineering", "risk_if_ignored", "Application slowly grinds to a halt as connections are checked out and lost due to unhandled exceptions.")
        );

        systemLearningService.recordTechnique(
            "DATABASE_MANAGEMENT",
            "Read Replica Offloading",
            "Protect the primary database from heavy analytical or non-critical read queries by routing them to asynchronously replicated read-only instances.",
            Arrays.asList(
                "Configure a Primary-Replica database topology.",
                "Use a read-write data source for POST/PUT/PATCH/DELETE operations.",
                "Use a read-only data source for heavy GET operations (e.g., reporting, dashboard stats).",
                "Accept eventual consistency for these read operations."
            ),
            0.92,
            Map.of("evidence_type", "scaling_pattern", "risk_if_ignored", "Heavy analytical queries lock tables or consume CPU, preventing users from saving critical transactions.")
        );
    }
"""

idx = content.find("        seedUnlimitedEngineeringWisdom();")
if idx != -1:
    insert_pos = idx + len("        seedUnlimitedEngineeringWisdom();")
    content = content[:insert_pos] + "\n        seedAutonomousEngineeringMastery();" + content[insert_pos:]
    
    class_end = content.rfind("}")
    content = content[:class_end] + additional_seeds_part3 + "\n}"
    
    with open("src/main/java/org/example/learning/StrategicKnowledgeLearningInitializer.java", "w", encoding="utf-8") as f:
        f.write(content)
