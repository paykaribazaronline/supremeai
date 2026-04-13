package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Built-in Analysis Service
 * SupremeAI's native analysis engine - ALWAYS available as 3rd participant
 * 
 * Used when:
 * - 0 external AIs: Solo mode
 * - 2+ AIs: System votes as tiebreaker (dynamic consensus)
 * 
 * Does NOT require external AI calls - pure pattern matching + rules
 */
@Service
public class BuiltInAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(BuiltInAnalysisService.class);
    
    public String analyze(String query) {
        // Pattern 1: Database questions
        if (isAbout(query, "database|sql|index|query|redis|mongo")) {
            return analyzeDatabaseQuery(query);
        }
        
        // Pattern 2: Architecture questions
        if (isAbout(query, "architecture|design|pattern|microservice|monolith")) {
            return analyzeArchitectureQuery(query);
        }
        
        // Pattern 3: Performance questions
        if (isAbout(query, "performance|optimize|slow|cache|latency|throughput")) {
            return analyzePerformanceQuery(query);
        }
        
        // Pattern 4: Security questions
        if (isAbout(query, "security|authenticate|authorize|jwt|token|password|encrypt")) {
            return analyzeSecurityQuery(query);
        }
        
        // Pattern 5: Error/Bug analysis
        if (isAbout(query, "error|bug|exception|crash|fail|null pointer")) {
            return analyzeErrorQuery(query);
        }
        
        // Pattern 6: Testing questions
        if (isAbout(query, "test|unit|integration|mock|stub|tdd")) {
            return analyzeTestingQuery(query);
        }
        
        // Pattern 7: Deployment/DevOps
        if (isAbout(query, "deploy|docker|kubernetes|ci/cd|jenkins|github|cloud")) {
            return analyzeDeploymentQuery(query);
        }
        
        // Fallback: Generic query handling
        return analyzeGenericQuery(query);
    }
    
    private boolean isAbout(String query, String keywords) {
        String lowerQuery = query.toLowerCase();
        String[] words = keywords.split("\\|");
        for (String word : words) {
            if (lowerQuery.contains(word)) {
                return true;
            }
        }
        return false;
    }
    
    private String analyzeDatabaseQuery(String query) {
        StringBuilder response = new StringBuilder();
        response.append("**Database Analysis:**\n\n");
        
        if (query.toLowerCase().contains("slow") || query.toLowerCase().contains("optimize")) {
            response.append("🔍 **Performance Issues** — Check these first:\n");
            response.append("1. **Missing Indexes** → Add index on frequently filtered columns\n");
            response.append("2. **N+1 Query Problem** → Use JOINs instead of loops\n");
            response.append("3. **Full Table Scans** → Verify index usage with EXPLAIN PLAN\n");
            response.append("4. **Connection Pool** → Ensure pool is properly configured\n");
            response.append("5. **Data Volume** → Archive old data, use pagination for large result sets\n\n");
        }
        
        if (query.toLowerCase().contains("index")) {
            response.append("📊 **Index Strategy:**\n");
            response.append("- Single column: Often used in WHERE clauses\n");
            response.append("- Composite: (dept, salary, name) for queries filtering by dept+salary\n");
            response.append("- Avoid: Too many indexes slow down INSERTs/UPDATEs\n");
            response.append("- Monitor: Use EXPLAIN to verify indexes are used\n\n");
        }
        
        if (query.toLowerCase().contains("transaction") || query.toLowerCase().contains("acid")) {
            response.append("🔒 **ACID Compliance:**\n");
            response.append("- **Atomicity:** All or nothing - use transactions\n");
            response.append("- **Consistency:** Data validity - check constraints\n");
            response.append("- **Isolation:** SET TRANSACTION ISOLATION LEVEL\n");
            response.append("- **Durability:** Persistent storage (WAL logs)\n");
        }
        
        if (response.length() == "**Database Analysis:**\n\n".length()) {
            response.append("General DB best practices:\n");
            response.append("✓ Normalize schema (3NF typical)\n");
            response.append("✓ Add indexes on foreign keys\n");
            response.append("✓ Monitor query performance regularly\n");
            response.append("✓ Use connection pooling (HikariCP)\n");
        }
        
        return response.toString();
    }
    
    private String analyzeArchitectureQuery(String query) {
        StringBuilder response = new StringBuilder();
        response.append("🏗️ **Architecture Analysis:**\n\n");
        
        if (query.toLowerCase().contains("microservice")) {
            response.append("📦 **Microservices Pattern:**\n");
            response.append("**Pros:** Independent scaling, team autonomy, tech flexibility\n");
            response.append("**Cons:** Distributed complexity, network latency, data consistency\n");
            response.append("**When to use:** 10+ teams, high scaling needs, independent services\n");
            response.append("**Watch out:** Service discovery, circuit breakers, shared data\n\n");
        }
        
        if (query.toLowerCase().contains("monolith")) {
            response.append("🏢 **Monolithic Pattern:**\n");
            response.append("**Pros:** Simple deployment, shared DB consistency, debugging\n");
            response.append("**Cons:** Harder to scale, single point of failure\n");
            response.append("**When to use:** Startups, simple domains, < 5 teams\n");
            response.append("**Exit strategy:** Break into services only when needed\n\n");
        }
        
        if (query.toLowerCase().contains("pattern") || query.toLowerCase().contains("design")) {
            response.append("🔧 **Common Patterns:**\n");
            response.append("- **MVC/MVP/MVVM:** UI separation\n");
            response.append("- **Repository:** Data access abstraction\n");
            response.append("- **Factory:** Object creation\n");
            response.append("- **Observer:** Event-driven systems\n");
            response.append("- **Strategy:** Algorithm switching\n");
        }
        
        if (response.length() == "🏗️ **Architecture Analysis:**\n\n".length()) {
            response.append("General architecture checklist:\n");
            response.append("✓ Separation of concerns (layers)\n");
            response.append("✓ Single responsibility principle\n");
            response.append("✓ Dependency injection\n");
            response.append("✓ API contracts defined\n");
        }
        
        return response.toString();
    }
    
    private String analyzePerformanceQuery(String query) {
        StringBuilder response = new StringBuilder();
        response.append("⚡ **Performance Analysis:**\n\n");
        
        response.append("**Optimization Priority (by impact):**\n");
        response.append("1. **Database queries** (70% of slowness)\n");
        response.append("   - Add indexes\n");
        response.append("   - Fix N+1 queries\n");
        response.append("   - Use query caching (Redis)\n\n");
        
        response.append("2. **Caching layer** (40% improvement)\n");
        response.append("   - L1: In-memory cache (LRU)\n");
        response.append("   - L2: Distributed cache (Redis)\n");
        response.append("   - L3: CDN (static assets)\n\n");
        
        response.append("3. **Algorithm optimization** (20% typical)\n");
        response.append("   - Profile first (find actual bottleneck)\n");
        response.append("   - O(n) vs O(n²) matters at scale\n");
        response.append("   - JVM: Use JProfiler, YourKit\n\n");
        
        response.append("4. **Async/parallel execution** (3-5x improvement)\n");
        response.append("   - Thread pools\n");
        response.append("   - Reactive (Project Reactor, RxJava)\n");
        response.append("   - Event-driven processing\n\n");
        
        response.append("**Measurement:**\n");
        response.append("- Set baseline (before optimization)\n");
        response.append("- Use APM tools (New Relic, DataDog)\n");
        response.append("- Test with production-like load\n");
        
        return response.toString();
    }
    
    private String analyzeSecurityQuery(String query) {
        StringBuilder response = new StringBuilder();
        response.append("🔐 **Security Analysis:**\n\n");
        
        response.append("**Top 10 OWASP Risks:**\n");
        response.append("1. **Injection:** Parameterized queries\n");
        response.append("2. **Broken Auth:** Multi-factor, session management\n");
        response.append("3. **Sensitive Data Exposure:** Encryption at rest & transit\n");
        response.append("4. **XML Entities:** Disable XXE parsing\n");
        response.append("5. **Access Control:** RBAC, principle of least privilege\n");
        response.append("6. **Security Config:** Default credentials, error messaging\n");
        response.append("7. **XSS:** Input validation, output encoding\n");
        response.append("8. **Insecure Deserialization:** Validate serialized objects\n");
        response.append("9. **Using Components with Known Vulns:** Keep dependencies updated\n");
        response.append("10. **Insufficient Logging:** Audit trails for compliance\n\n");
        
        if (query.toLowerCase().contains("jwt") || query.toLowerCase().contains("token")) {
            response.append("**JWT Best Practices:**\n");
            response.append("- Sign with strong algorithm (HS256+ or RS256)\n");
            response.append("- Short expiry (5-15 min), refresh tokens for renewal\n");
            response.append("- Never put secrets in JWT payload\n");
            response.append("- Validate signature SERVER-SIDE\n");
        }
        
        return response.toString();
    }
    
    private String analyzeErrorQuery(String query) {
        StringBuilder response = new StringBuilder();
        response.append("🐛 **Error Analysis:**\n\n");
        
        response.append("**Debugging Approach:**\n");
        response.append("1. **Reproduce:** Create minimal test case\n");
        response.append("2. **Isolate:** Narrow scope (unit, integration, system)\n");
        response.append("3. **Hypothesis:** What could cause this?\n");
        response.append("4. **Test:** Add debug logs, breakpoints\n");
        response.append("5. **Verify:** Fix works in all scenarios\n");
        response.append("6. **Prevent:** Add test case for regression\n\n");
        
        if (query.toLowerCase().contains("null")) {
            response.append("**NullPointerException Solutions:**\n");
            response.append("- Add null checks before use\n");
            response.append("- Use Optional<T> (Java 8+)\n");
            response.append("- Enable `@NonNull` annotations\n");
            response.append("- Use static analysis tools\n");
        }
        
        return response.toString();
    }
    
    private String analyzeTestingQuery(String query) {
        StringBuilder response = new StringBuilder();
        response.append("✅ **Testing Strategy:**\n\n");
        
        response.append("**Test Pyramid:**\n");
        response.append("```\n");
        response.append("        /\\\n");
        response.append("       /  \\\n");
        response.append("    E2E Tests (10%) - Selenium, Cypress\n");
        response.append("      /      \\\n");
        response.append("   /          \\\n");
        response.append("Integration (30%) - TestContainers, H2\n");
        response.append("    /              \\\n");
        response.append("  /                  \\\n");
        response.append("Unit Tests (60%) - JUnit, Mockito\n");
        response.append("```\n\n");
        
        response.append("**Best Practices:**\n");
        response.append("- Arrange-Act-Assert (AAA) pattern\n");
        response.append("- Mock external dependencies\n");
        response.append("- Test edge cases (null, empty, negative)\n");
        response.append("- Aim for 80%+ coverage, not 100%\n");
        response.append("- CI/CD must run tests on every commit\n");
        
        return response.toString();
    }
    
    private String analyzeDeploymentQuery(String query) {
        StringBuilder response = new StringBuilder();
        response.append("🚀 **Deployment Analysis:**\n\n");
        
        response.append("**CI/CD Pipeline:**\n");
        response.append("1. **Code commit** → GitHub/GitLab\n");
        response.append("2. **Automated tests** → Unit, integration, E2E\n");
        response.append("3. **Build artifact** → Docker image, JAR\n");
        response.append("4. **Deploy to staging** → Validation environment\n");
        response.append("5. **Smoke tests** → Verify deployment\n");
        response.append("6. **Deploy to production** → Canary or blue-green\n");
        response.append("7. **Monitor** → APM, logs, alerts\n\n");
        
        if (query.toLowerCase().contains("docker")) {
            response.append("**Docker Best Practices:**\n");
            response.append("- Use specific base image tags (not 'latest')\n");
            response.append("- Multi-stage builds (smaller images)\n");
            response.append("- Security scanning (Trivy, Snyk)\n");
            response.append("- Run as non-root user\n");
        }
        
        if (query.toLowerCase().contains("kubernetes")) {
            response.append("**Kubernetes Best Practices:**\n");
            response.append("- Resource requests/limits\n");
            response.append("- Liveness & readiness probes\n");
            response.append("- ConfigMaps for config, Secrets for sensitive data\n");
            response.append("- Use namespaces for isolation\n");
        }
        
        return response.toString();
    }
    
    private String analyzeGenericQuery(String query) {
        return "**SupremeAI Built-in Analysis:**\n\n" +
            "Query analyzed for patterns in architecture, performance, security, and testing.\n\n" +
            "**Built-in knowledge includes:**\n" +
            "- Design patterns (MVC, Repository, Factory, Observer, Strategy)\n" +
            "- Database optimization (indexing, caching, query tuning)\n" +
            "- Performance debugging (profiling, bottleneck identification)\n" +
            "- Security best practices (OWASP, authentication, encryption)\n" +
            "- Testing strategies (unit, integration, E2E, TDD)\n" +
            "- Deployment patterns (CI/CD, Docker, Kubernetes)\n\n" +
            "**For specialized AI responses**, external AI providers offer domain expertise.";
    }
}
