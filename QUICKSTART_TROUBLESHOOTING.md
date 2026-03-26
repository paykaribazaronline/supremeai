# 🚀 QUICK START & TROUBLESHOOTING GUIDE

**AI Multi-Agent App Generator - Getting Started & Problem Solving**

---

## 1. QUICK START (5 Minutes)

### Step 1: Verify Java Installation
```powershell
java -version
# Should show: java version "17" or higher
```

### Step 2: Build the Project
```powershell
cd c:\Users\Nazifa\supremeai
.\gradlew build
```

**Expected Output:**
```
BUILD SUCCESSFUL in 15s
```

### Step 3: Check Configuration
```powershell
cat src/main/resources/application.properties
```

### Step 4: Run Unit Tests
```powershell
.\gradlew test
```

**Expected Output:**
```
BUILD SUCCESSFUL in 20s
2 tests passed
```

---

## 2. COMMON ISSUES & SOLUTIONS

### Issue 1: Gradle Build Fails

**Error:**
```
Error: Could not find or load main class
```

**Solution:**
```powershell
# Clean and rebuild
.\gradlew clean build --refresh-dependencies

# If problem persists, check Java version
java -version  # Must be 17+

# Verify gradle wrapper
.\gradlew --version
```

---

### Issue 2: Firebase Not Connected

**Error:**
```
Failed to initialize FirebaseApp
```

**Solution 1: Check Google Cloud Credentials**
```powershell
# Windows: Check environment variable
$env:GOOGLE_APPLICATION_CREDENTIALS
# Should point to your service account key file

# Set it if missing
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\path\to\service-account-key.json"
```

**Solution 2: Use Firebase Emulator (Local Development)**
```powershell
# Install Firebase CLI
npm install -g firebase-tools

# Start emulator
firebase emulators:start

# Update application.properties
# firebase.database.default=false
# firestore.emulator.host=localhost:8080
```

---

### Issue 3: API Keys Not Working

**Error:**
```
Unauthorized: Invalid API key
```

**Solution:**
```powershell
# 1. Verify API key is in Google Cloud Secret Manager
gcloud secrets list --project=supremeai

# 2. Check SecretManager can access it
# Run: SecretManager.main() for diagnostic

# 3. For local development, use environment variables
$env:DEEPSEEK_KEY = "your-key-here"
$env:GROQ_KEY = "your-key-here"
```

---

### Issue 4: Rate Limiting Errors

**Error:**
```
429 Too Many Requests
```

**Solution:**
```powershell
# Check configuration
cat src/main/resources/application.properties | Select-String "ratelimit"

# For testing: increase limits temporarily
# ratelimit.user.tokens.per_minute=1000

# Check rate limiter status
# Add debug logging: RateLimitingService.logStats()
```

---

### Issue 5: Tests Fail

**Error:**
```
org.junit.ComparisonFailure: expected:<success> but was:<error>
```

**Solution 1: Check Dependencies**
```powershell
.\gradlew dependencies | grep -i junit
# Should show: junit:junit-bom:5.9.3 or higher
```

**Solution 2: Run in Isolation**
```powershell
# Run single test
.\gradlew test --tests APIErrorHandlerTest

# Run with verbose output
.\gradlew test --info
```

**Solution 3: Clean Test Cache**
```powershell
.\gradlew cleanTest test
```

---

### Issue 6: Logback Not Logging

**Error:**
```
No output from AuditLogger
```

**Solution:**
```powershell
# 1. Verify logback.xml exists
Test-Path src/main/resources/logback.xml

# 2. Check SLF4J is bound to Logback
.\gradlew dependencies | Select-String "logback"

# 3. Enable debug logging
# In logback.xml: <root level="DEBUG">

# 4. Test logging directly
# Create Main.java test:
# Logger logger = LoggerFactory.getLogger(Main.class);
# logger.info("Test message");  // Should appear
```

---

### Issue 7: Metrics Not Collecting

**Error:**
```
MetricsService returns empty map
```

**Solution:**
```powershell
# 1. Verify Micrometer is in build.gradle.kts
.\gradlew dependencies | grep -i micrometer

# 2. Check MetricsService initialization
# Make sure it's created before services:
# MetricsService metrics = new MetricsService();

# 3. Record metrics explicitly
# metrics.recordSuccess("orchestration");
# System.out.println(metrics.getMetricsSummary());
```

---

## 3. DEVELOPMENT WORKFLOW

### Daily Development

```powershell
# 1. Start of day: pull latest
git pull origin main

# 2. Build and test
.\gradlew clean test

# 3. Make changes
# Edit Main.java or service classes

# 4. Verify changes
.\gradlew test

# 5. Check code quality
.\gradlew check

# 6. Commit changes
git add .
git commit -m "Feature: description"
git push origin feature-branch
```

---

### Running the Application

**Option 1: Using Gradle**
```powershell
.\gradlew run
```

**Option 2: Direct Java Execution**
```powershell
.\gradlew build
java -cp "build\classes\java\main;build\resources\main" org.example.Main
```

**Option 3: IDE Execution**
- Open in IntelliJ IDEA
- Right-click Main.java
- Click "Run Main.main()"

---

### Adding New Feature

```powershell
# 1. Create feature branch
git checkout -b feature/my-feature

# 2. Create service class
New-Item src/main/java/org/example/service/MyService.java

# 3. Add JUnit test
New-Item src/test/java/org/example/service/MyServiceTest.java

# 4. Write implementation
# Implement MyService with business logic

# 5. Write test first (TDD approach)
# @Test public void testFeature() { ... }

# 6. Test it
.\gradlew test

# 7. Commit
git add .
git commit -m "Feature: My new feature"
```

---

### Debugging Tips

**Enable Debug Logging:**
```properties
# In application.properties
logging.level.org.example=DEBUG
logging.level.com.google.firebase=DEBUG
```

**Breakpoint Debugging (IDEA):**
```
1. Left-click line number to set breakpoint
2. Right-click Main.java → Debug 'Main'
3. Use F10 (Step Over), F11 (Step Into)
4. Inspect variables in Debug panel
```

**Print Debugging:**
```java
// Good for quick debugging
System.out.println("DEBUG: agentScore = " + agentScore);
logger.debug("Agent selected: {}", agentId);

// Better: Use SLF4J
logger.info("Processing requirement: {}", requirementId);
AuditLogger.logEvent("REQUIREMENT_CLASSIFIED", requirementId, "size=" + size);
```

---

## 4. PERFORMANCE TUNING

### Check Performance Baseline

```powershell
# Run with metrics
.\gradlew build
java -cp "build\classes\java\main" org.example.Main 2>&1 | Select-String "duration"

# Example output:
# orchestration.duration = 150ms
# api.call.duration = 45ms
# requirement.processing.duration = 30ms
```

### Optimize if Needed

**Slow API Calls (> 5 seconds):**
```java
// Check timeout in application.properties
api.timeout.seconds=30  // May be too high for some APIs

// Consider reducing to 15-20 seconds
// Set per-provider timeouts

// Add caching
String cachedResult = cache.get(apiCall);
if (cachedResult != null) return cachedResult;
```

**Slow Consensus (> 10 seconds):**
```java
// Check agent pool size
consensus.thread.pool.size=10  // May be too small

// Increase for concurrent consensus
consensus.thread.pool.size=20

// Check timeout
consensus.timeout.seconds=30
```

**Slow Requirement Classification (> 3 seconds):**
```java
// Use caching for patterns
cache.put("description_pattern_1", "SMALL");

// Check database queries
// Use indexes on Firestore collections
```

---

## 5. MONITORING & HEALTH CHECKS

### Health Check Endpoint

```powershell
# Add to Main.java
if (args.length > 0 && args[0].equals("health")) {
    System.out.println("status: healthy");
    System.out.println("timestamp: " + System.currentTimeMillis());
    System.out.println("services: all running");
    System.exit(0);
}

# Run health check
java -cp "build\classes\java\main" org.example.Main health
# Output: status: healthy
```

### Metrics Endpoint

```powershell
# Add to Main.java
if (args.length > 0 && args[0].equals("metrics")) {
    MetricsService metrics = MetricsService.getInstance();
    System.out.println(metrics.getMetricsSummary());
    System.exit(0);
}

# Get metrics
java -cp "build\classes\java\main" org.example.Main metrics
# Output: JSON with all metrics
```

---

## 6. UPDATING DEPENDENCIES

### Check for Updates

```powershell
# See available updates
.\gradlew dependencyUpdates

# Example output:
# com.google.firebase:firebase-admin [9.2.0 -> 9.2.2]
# io.github.resilience4j:resilience4j-core [2.0.0 -> 2.1.0]
```

### Apply Updates Safely

```powershell
# 1. Update build.gradle.kts
# Change version numbers

# 2. Run tests
.\gradlew clean test

# 3. Check compatibility
# Some updates may break code

# 4. Commit
git add build.gradle.kts
git commit -m "Chore: Update dependencies"
```

---

## 7. DEPLOYMENT CHECKLIST

Before deploying to production:

```powershell
# 1. Run full test suite
.\gradlew test

# 2. Run security checks
.\gradlew dependencyCheckAggregate

# 3. Build release version
.\gradlew build -Drelease=true

# 4. Verify configuration
cat src/main/resources/application.properties | Select-String "secret|key|token"
# Should show NO actual keys (only placeholders)

# 5. Check Git history
git log --oneline -10
# Verify all commits are reviewed

# 6. Tag release
git tag -a v1.0.0 -m "Phase 1 release"

# 7. Verify artifact
Test-Path build/libs/supremeai.jar
```

---

## 8. EMERGENCY PROCEDURES

### System Not Responding

```powershell
# 1. Check logs
tail -f logs/application.log
tail -f logs/audit.log

# 2. Check metrics
java -cp "build\classes\java\main" org.example.Main metrics

# 3. Check error rate
# Look for spikes in error counters

# 4. Restart service
# Kill existing process
# Start new instance

# 5. Check if circuit breaker is open
# Look for: circuitBreaker.state = OPEN
# Wait for half-open period (60 seconds)
```

### High Error Rate

```powershell
# 1. Check API quotas
# DeepSeek quota: ?
# Groq quota: ?
# Verify in respective dashboards

# 2. Check rate limiting
# Is rate limiter blocking requests?
# Check application logs for 429 errors

# 3. Check database
# Is Firestore responsive?
# Check GCP dashboard

# 4. Temporary mitigation
# Reduce load or fallback to another API

# 5. Investigate root cause
# Check detailed logs
# Correlate with deployments
```

### Memory Issues

```powershell
# Increase heap size
java -Xmx2g -Xms2g -cp "build\classes\java\main" org.example.Main

# Check memory usage
Get-Process java | Select-Object ProcessName, @{Name="WorkingSetMB"; Expression={[math]::Round($_.WorkingSet/1MB)}}

# Enable garbage collection logs
java -Xloggc:gc.log -cp "build\classes\java\main" org.example.Main
```

---

## 9. GETTING HELP

### Debug Information to Gather

When reporting issues, include:
```
1. Full error message and stack trace
2. OS and Java version: java -version
3. Last 20 lines of logs: tail -20 logs/application.log
4. Configuration: cat src/main/resources/application.properties
5. Steps to reproduce the issue
6. Expected vs actual behavior
7. When it started happening
```

### Support Resources

```
Documentation: See *.md files in workspace root
  - README.md: Overview
  - PHASE1_SETUP.md: Step-by-step setup
  - PHASE1_ARCHITECTURE.md: Design details
  - SECURITY_GUIDE.md: Security implementation
  - PRODUCTION_READINESS.md: Production checklist

Code Examples:
  - src/main/java/org/example/Main.java: Entry point
  - src/main/java/org/example/service/: All services

Tests:
  - src/test/java/org/example/: Unit test examples
```

---

## 10. FEEDBACK & IMPROVEMENTS

### Report Bugs

```
Create GitHub issue with:
1. Title: [BUG] Brief description
2. Description: Steps to reproduce
3. Actual behavior
4. Expected behavior
5. Environment (OS, Java version, etc.)
6. Logs and stack traces
```

### Suggest Features

```
Create GitHub issue with:
1. Title: [FEATURE] What this feature does
2. Description: Use case and benefits
3. Example usage
4. Related to Phase 1/2/3?
5. Priority: Critical/High/Medium/Low
```

### Contribute Code

```
1. Fork repository
2. Create feature branch
3. Add unit tests
4. Submit pull request with description
5. Address review feedback
6. Merge to main
```

---

## 11. COMMON COMMANDS REFERENCE

```powershell
# Build
.\gradlew build                    # Full build

# Testing
.\gradlew test                     # Run all tests
.\gradlew test --tests MyTest      # Run specific test
.\gradlew cleanTest test           # Clean and test

# Cleaning
.\gradlew clean                    # Remove build artifacts
.\gradlew cleanBuild               # Remove and rebuild

# Dependency Management
.\gradlew dependencies             # Show all dependencies
.\gradlew dependencyUpdates        # Show available updates
.\gradlew dependencyCheckAggregate # Check for vulnerabilities

# Running
.\gradlew run                      # Run application
java -cp build\classes\java\main org.example.Main  # Direct execution

# Gradle Information
.\gradlew --version                # Gradle version
.\gradlew tasks                    # List available tasks
.\gradlew help                     # Show Gradle help
```

---

**Last Updated:** March 26, 2026  
**Status:** Ready for Team Distribution

**Quick Links:**
- [Full Documentation](README.md)
- [Architecture Guide](PHASE1_ARCHITECTURE.md)
- [Security Guide](SECURITY_GUIDE.md)
- [Production Readiness](PRODUCTION_READINESS.md)
