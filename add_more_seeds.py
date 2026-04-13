import re

with open("src/main/java/org/example/learning/StrategicKnowledgeLearningInitializer.java", "r", encoding="utf-8") as f:
    content = f.read()

additional_seeds = """

    private void seedExtendedEngineeringPatterns() {
        // --- RESILIENCE & HEALING ---
        systemLearningService.recordTechnique(
            "RESILIENCE",
            "Auto-Healing Pod Restarts",
            "When memory leaks or unrecoverable thread deadlocks occur, relying on orchestration-level restarts is safer than application-level recovery attempts.",
            Arrays.asList(
                "Configure readiness and liveness probes correctly.",
                "Ensure application is stateless and gracefully shuts down on SIGTERM.",
                "Allow Kubernetes/Cloud Run to kill and replace the container."
            ),
            0.98,
            Map.of("evidence_type", "cloud_native_patterns", "risk_if_ignored", "Zombie containers consume resources without serving traffic.")
        );

        systemLearningService.recordTechnique(
            "RESILIENCE",
            "Fallback Cache Utilization",
            "When primary database is unreachable, serve stale data from cache to maintain read-only operations.",
            Arrays.asList(
                "Wrap database reads in try-catch.",
                "On exception, query distributed cache (e.g., Redis).",
                "Return cached data with a header indicating it is stale."
            ),
            0.92,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Complete read outage during brief database hiccups.")
        );

        // --- SECURITY ---
        systemLearningService.recordTechnique(
            "SECURITY",
            "Zero Trust Network Boundaries",
            "Do not assume internal network traffic is safe. Authenticate and authorize every microservice-to-microservice call.",
            Arrays.asList(
                "Use mTLS for all internal service communication.",
                "Enforce strict network policies dropping unapproved traffic.",
                "Require JWT or service tokens for all internal APIs."
            ),
            0.99,
            Map.of("evidence_type", "security_standard", "risk_if_ignored", "Lateral movement of attackers if one microservice is compromised.")
        );

        systemLearningService.recordTechnique(
            "SECURITY",
            "Automated Dependency Vulnerability Scanning",
            "Integrate tools like Dependabot or Snyk to block builds containing known CVEs.",
            Arrays.asList(
                "Add vulnerability scanning step to CI pipeline.",
                "Fail the build if Critical or High vulnerabilities are found.",
                "Automate PR creation for dependency updates."
            ),
            0.97,
            Map.of("evidence_type", "devsecops_practice", "risk_if_ignored", "Shipping code with known exploits.")
        );

        // --- PERFORMANCE ---
        systemLearningService.recordTechnique(
            "PERFORMANCE",
            "Asynchronous Log Appending",
            "Synchronous logging blocks application threads. Use async loggers to prevent logging from becoming a bottleneck under high load.",
            Arrays.asList(
                "Configure logback/log4j to use AsyncAppender.",
                "Set appropriate queue size and discarding threshold for INFO/DEBUG logs.",
                "Never discard ERROR or FATAL logs."
            ),
            0.95,
            Map.of("evidence_type", "benchmark", "risk_if_ignored", "Application throughput collapses when logging volume increases.")
        );

        systemLearningService.recordTechnique(
            "PERFORMANCE",
            "Database Index Optimization",
            "Identify slow queries and add composite indexes matching the WHERE and ORDER BY clauses.",
            Arrays.asList(
                "Enable slow query logging in the database.",
                "Analyze query execution plans (EXPLAIN).",
                "Add indexes covering the exact fields used in filtering and sorting."
            ),
            0.96,
            Map.of("evidence_type", "database_optimization", "risk_if_ignored", "Full table scans locking the database under load.")
        );

        // --- CI/CD & DEPLOYMENT ---
        systemLearningService.recordTechnique(
            "CI_CD",
            "Canary Release Strategy",
            "Deploy new code to a small subset of users (e.g., 5%) before full rollout to catch unpredicted production errors.",
            Arrays.asList(
                "Configure load balancer to route 5% of traffic to new version.",
                "Monitor error rates and latency for 15 minutes.",
                "If metrics degrade, automatically route traffic back to stable.",
                "If stable, gradually increase traffic to 100%."
            ),
            0.94,
            Map.of("evidence_type", "release_engineering", "risk_if_ignored", "Deploying a fatal bug to 100% of users simultaneously.")
        );

        systemLearningService.recordTechnique(
            "CI_CD",
            "Infrastructure as Code (IaC) Validation",
            "Treat infrastructure changes like application code. Validate Terraform/Pulumi scripts before applying.",
            Arrays.asList(
                "Run 'terraform plan' in CI to show changes.",
                "Use static analysis (e.g., tfsec) to check for misconfigurations (e.g., public S3 buckets).",
                "Require manual approval for destructive changes."
            ),
            0.98,
            Map.of("evidence_type", "infrastructure_practice", "risk_if_ignored", "Accidental deletion of production databases or exposure of private networks.")
        );

        // --- DATA INTEGRITY ---
        systemLearningService.recordTechnique(
            "DATA_INTEGRITY",
            "Soft Deletion Pattern",
            "Never hard-delete records. Mark them as deleted to allow for easy recovery and auditability.",
            Arrays.asList(
                "Add a 'deleted_at' timestamp column to critical tables.",
                "Update application queries to filter out records where 'deleted_at' is not null.",
                "Create a background job to permanently archive soft-deleted records after 90 days."
            ),
            0.95,
            Map.of("evidence_type", "data_management", "risk_if_ignored", "Permanent loss of user data due to bugs or accidental admin actions.")
        );

        systemLearningService.recordTechnique(
            "DATA_INTEGRITY",
            "Optimistic Concurrency Control",
            "Prevent lost updates in concurrent environments by using version numbers on database records.",
            Arrays.asList(
                "Add a 'version' integer column to the table.",
                "Include the version in the WHERE clause of UPDATE statements.",
                "Increment the version by 1 on update.",
                "If the update affects 0 rows, throw a ConcurrentModificationException."
            ),
            0.97,
            Map.of("evidence_type", "database_pattern", "risk_if_ignored", "Two users editing the same record overwrite each other's changes silently.")
        );
    }
"""

idx = content.find("        seedAdvancedResearchFindings();")
if idx != -1:
    insert_pos = idx + len("        seedAdvancedResearchFindings();")
    content = content[:insert_pos] + "\n        seedExtendedEngineeringPatterns();" + content[insert_pos:]
    
    class_end = content.rfind("}")
    content = content[:class_end] + additional_seeds + "\n}"
    
    with open("src/main/java/org/example/learning/StrategicKnowledgeLearningInitializer.java", "w", encoding="utf-8") as f:
        f.write(content)
