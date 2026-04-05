# ⚠️ SupremeAI Common Mistakes & Documented Fixes

**Last Updated:** April 2, 2026
**Purpose:** Every real error encountered in SupremeAI, its root cause, and its verified fix
**Learning Source:** Accumulated from CI failures, security audits, and production incidents

> **How to use:** Search (Ctrl+F) for your error message or category. Each entry includes the symptom, root cause, fix, and prevention rule.

---

## 🔐 SECURITY MISTAKES

### Mistake 1: Using Open `/api/auth/init` Endpoint

**Symptom:**

```
Any unauthenticated user can create an admin account
Admin credentials compromised after deployment
```

**Root Cause:**
The `/api/auth/init` endpoint had no token protection, allowing anyone to POST credentials and create the system admin.

**Wrong Code:**

```java
// ❌ NEVER DO THIS
@PostMapping("/api/auth/init")
public ResponseEntity<?> initAdmin(@RequestBody AdminRequest req) {
    return adminService.createAdmin(req.getUsername(), req.getPassword());
}
```

**Fixed Code:**

```java
// ✅ CORRECT — requires setup token
@PostMapping("/api/auth/setup")
public ResponseEntity<?> setupAdmin(
    @RequestHeader("Authorization") String authHeader,
    @RequestBody AdminRequest req) {
    String token = authHeader.replace("Bearer ", "");
    if (!token.equals(System.getenv("SUPREMEAI_SETUP_TOKEN"))) {
        return ResponseEntity.status(403).body("Forbidden");
    }
    return adminService.createAdmin(req.getUsername(), req.getPassword());
}
```

**Rule:** `SUPREMEAI_SETUP_TOKEN` env var must be set before first deploy. See [ENVIRONMENT_CONFIGURATION.md](ENVIRONMENT_CONFIGURATION.md).

---

### Mistake 2: Command Injection via Branch Name

**Symptom:**

```
Git operations execute unexpected shell commands
Branch name like "main; rm -rf /" causes system damage
```

**Root Cause:**
Branch names passed directly to `Runtime.exec(String)` instead of array-based `ProcessBuilder`.

**Wrong Code:**

```java
// ❌ NEVER DO THIS
Runtime.getRuntime().exec("git push origin " + branchName);
```

**Fixed Code:**

```java
// ✅ CORRECT — array args prevent injection
private static final Pattern SAFE_BRANCH = Pattern.compile("^[a-zA-Z0-9/_.-]{1,100}$");

public void pushToRemote(String branch) {
    if (!SAFE_BRANCH.matcher(branch).matches()) {
        throw new IllegalArgumentException("Invalid branch name: " + branch);
    }
    ProcessBuilder pb = new ProcessBuilder("git", "push", "origin", branch);
    pb.redirectErrorStream(false); // keep stderr separate
    // ...
}
```

**Rule:** Always use `ProcessBuilder` with array arguments. Always validate branch names against `^[a-zA-Z0-9/_.-]{1,100}$`.

---

### Mistake 3: Missing Null Check on Environment Variables

**Symptom:**

```
NullPointerException at startup
System silently uses null token, all auth bypassed
```

**Root Cause:**
`System.getenv("GITHUB_TOKEN")` returns `null` when the variable is not set. Code that uses the result without null-checking silently fails or bypasses security.

**Wrong Code:**

```java
// ❌ NEVER DO THIS
String token = System.getenv("GITHUB_TOKEN");
headers.setBearerAuth(token); // NullPointerException or null auth
```

**Fixed Code:**

```java
// ✅ CORRECT — validate at startup
@PostConstruct
public void validateEnv() {
    String githubToken = System.getenv("GITHUB_TOKEN");
    String setupToken = System.getenv("SUPREMEAI_SETUP_TOKEN");
    if (githubToken == null || githubToken.isBlank()) {
        throw new IllegalStateException("GITHUB_TOKEN environment variable is required");
    }
    if (setupToken == null || setupToken.isBlank()) {
        throw new IllegalStateException("SUPREMEAI_SETUP_TOKEN environment variable is required");
    }
}
```

**Rule:** Validate all required env vars in a `@PostConstruct` method. Fail fast at startup, not at runtime.

---

## 🔧 IMPLEMENTATION MISTAKES

### Mistake 4: Parsing the Request Instead of the Response

**Symptom:**

```
API call "succeeds" but the result is always empty
Logged data shows original request fields, not the AI response
```

**Root Cause:**
Code was reading from the `HttpEntity` request body instead of capturing the `RestTemplate` response body.

**Wrong Code:**

```java
// ❌ NEVER DO THIS
HttpEntity<String> request = new HttpEntity<>(body, headers);
String result = request.getBody(); // this is the REQUEST, not the response!
```

**Fixed Code:**

```java
// ✅ CORRECT — capture the response
HttpEntity<String> request = new HttpEntity<>(body, headers);
ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
String result = response.getBody(); // this is the RESPONSE
```

**Rule:** Always store `ResponseEntity<T>` from RestTemplate calls. Log both request and response bodies at DEBUG level.

---

### Mistake 5: Merging stderr with stdout

**Symptom:**

```
Can't tell if a Git operation succeeded or failed
Error messages mixed into output, causing false positive parsing
```

**Root Cause:**
`ProcessBuilder.redirectErrorStream(true)` merges error output into standard output, making it impossible to distinguish success from failure.

**Wrong Code:**

```java
// ❌ NEVER DO THIS
ProcessBuilder pb = new ProcessBuilder("git", "push", "origin", branch);
pb.redirectErrorStream(true); // merges stderr into stdout — hard to debug
```

**Fixed Code:**

```java
// ✅ CORRECT — capture stderr separately
ProcessBuilder pb = new ProcessBuilder("git", "push", "origin", branch);
pb.redirectErrorStream(false);
Process process = pb.start();

String stdout = new String(process.getInputStream().readAllBytes());
String stderr = new String(process.getErrorStream().readAllBytes());
int exitCode = process.waitFor();

if (exitCode != 0 || stderr.contains("error") || stderr.contains("fatal")) {
    log.error("Git push failed. stderr: {}", stderr);
    throw new GitOperationException("Push failed: " + stderr);
}
```

**Rule:** Always capture stdout and stderr separately. Check both `exitCode` and stderr content for errors.

---

### Mistake 6: Assuming Success Without Verifying Output

**Symptom:**

```
Git commit shows exit code 0 but no commit was created
Push "succeeds" but remote is not updated
```

**Root Cause:**
Exit code 0 is not sufficient. Git outputs `nothing to commit, working tree clean` with exit code 0 even when nothing happened.

**Wrong Code:**

```java
// ❌ NEVER DO THIS
if (process.waitFor() == 0) {
    return "success"; // might be "nothing to commit"!
}
```

**Fixed Code:**

```java
// ✅ CORRECT — check the actual output content
String output = new String(process.getInputStream().readAllBytes());
int exitCode = process.waitFor();

if (exitCode != 0) {
    throw new GitOperationException("Command failed with exit code " + exitCode);
}
if (output.contains("nothing to commit")) {
    return GitResult.NO_CHANGES; // distinct result, not an error
}
if (output.contains("error") || output.contains("fatal")) {
    throw new GitOperationException("Git error in output: " + output);
}
return GitResult.SUCCESS;
```

**Rule:** Parse Git output text, not just exit codes. Distinguish "no changes", "success", and "error" as separate states.

---

### Mistake 7: Single Format Parsing (Commit Hash Extraction)

**Symptom:**

```
Commit hash extraction works in dev, breaks in CI/CD
Different Git versions produce different output formats
```

**Root Cause:**
Code assumed Git output format is always `[main abc1234] message`, which varies between Git versions and configurations.

**Wrong Code:**

```java
// ❌ NEVER DO THIS
String hash = output.split("\\[")[1].split(" ")[1]; // breaks if format differs
```

**Fixed Code:**

```java
// ✅ CORRECT — use git log as authoritative source
private String extractCommitHash(String commitOutput, String branchName) {
    // Try format: [main abc1234] ...
    Pattern bracketPattern = Pattern.compile("\\[\\S+\\s+([a-f0-9]+)\\]");
    Matcher m = bracketPattern.matcher(commitOutput);
    if (m.find()) return m.group(1);

    // Fallback: query git directly
    try {
        Process p = new ProcessBuilder("git", "rev-parse", "HEAD").start();
        return new String(p.getInputStream().readAllBytes()).trim();
    } catch (Exception e) {
        log.warn("Could not extract commit hash", e);
        return "unknown";
    }
}
```

**Rule:** Use `git rev-parse HEAD` as the authoritative source for commit hash after any commit operation.

---

### Mistake 8: Missing Input Validation

**Symptom:**

```
Commit messages with quotes break Git command execution
Branch names with spaces cause ProcessBuilder to split arguments incorrectly
```

**Root Cause:**
No validation on user-supplied commit messages, branch names, or requirement text before processing.

**Wrong Code:**

```java
// ❌ NEVER DO THIS
public void commitChanges(String message, String author) {
    ProcessBuilder pb = new ProcessBuilder("git", "commit", "-m", message);
    // message could contain quotes, newlines, or injection sequences
}
```

**Fixed Code:**

```java
// ✅ CORRECT — validate and sanitize inputs
public void commitChanges(String message, String author) {
    if (message == null || message.isBlank()) {
        throw new IllegalArgumentException("Commit message cannot be blank");
    }
    if (message.length() > 500) {
        throw new IllegalArgumentException("Commit message too long (max 500 chars)");
    }
    // ProcessBuilder handles quoting automatically — no manual escaping needed
    ProcessBuilder pb = new ProcessBuilder("git", "commit",
        "--author", author + " <" + author + "@supremeai.ai>",
        "-m", message);
}
```

**Rule:** Validate length, nullness, and character set. With `ProcessBuilder` array args, no shell escaping is needed — but you still need null/length checks.

---

## 📦 BUILD & DEPLOYMENT MISTAKES

### Mistake 9: Argument List Too Long in CI

**Symptom:**

```
/bin/sh: 1: markdownlint: Argument list too long
Workflow fails at docs-lint step
```

**Root Cause:**
Passing all 80+ markdown files as a single argument list to `markdownlint` in one command.

**Wrong Command:**

```bash
# ❌ NEVER DO THIS
markdownlint $(find . -name "*.md" | tr '\n' ' ')
```

**Fixed Command:**

```bash
# ✅ CORRECT — batch processing
find . -name "*.md" | head -20 | xargs markdownlint --config .markdownlint.json
find . -name "*.md" | tail -n +21 | head -20 | xargs markdownlint --config .markdownlint.json
# ... etc. (or use a batch script)
```

**Full fix:** See [ROOT_CAUSE_AUTO_FIX_ANALYSIS.md](ROOT_CAUSE_AUTO_FIX_ANALYSIS.md) and [CI_LINT_PRIORITY_EXECUTION.md](CI_LINT_PRIORITY_EXECUTION.md).

**Rule:** Never pass an unbounded file list to a CLI tool. Always batch in groups of ≤20 files.

---

### Mistake 10: Markdownlint Config Without `"default": true`

**Symptom:**

```
markdownlint --fix runs without errors but files are not fixed
Auto-fixable rules (MD022, MD031, MD032) silently skip
```

**Root Cause:**
`.markdownlint.json` only disabled specific rules but never explicitly enabled all rules first. Markdownlint's default behavior is inconsistent without `"default": true`.

**Wrong Config:**

```json
{
  "MD013": false,
  "MD024": false,
  "MD031": { "list_items": false }
}
```

**Fixed Config:**

```json
{
  "default": true,
  "MD013": false,
  "MD024": false,
  "MD031": { "list_items": false },
  "MD033": false,
  "MD041": false
}
```

**Rule:** Always start `.markdownlint.json` with `"default": true`. Then disable only the rules you want to skip.

---

## 🧠 LEARNING SYSTEM MISTAKES

### Mistake 11: Not Recording Errors in the Learning System

**Symptom:**

```
Same error occurs multiple times
No auto-fix applied on second occurrence
Learning stats show 0 entries
```

**Root Cause:**
Error was caught and logged but `SystemLearningService.recordError()` was never called.

**Wrong Code:**

```java
// ❌ NEVER DO THIS
try {
    doOperation();
} catch (Exception e) {
    log.error("Operation failed", e);
}
```

**Fixed Code:**

```java
// ✅ CORRECT — teach the system from every error
@Autowired
private SystemLearningService learningService;

try {
    doOperation();
} catch (Exception e) {
    log.error("Operation failed", e);
    learningService.recordError(
        e.getClass().getSimpleName(),
        e.getMessage(),
        "git-operations",
        Severity.HIGH
    );
}
```

**Rule:** Every `catch` block in a Service class must include `learningService.recordError()`. The learning system is only as good as the data it receives.

---

### Mistake 12: Not Checking Admin Mode Before Operations

**Symptom:**

```
Operations execute even when FORCE_STOP mode is active
Admin loses control during emergency scenarios
```

**Root Cause:**
Service methods performing Git or generation operations didn't check the admin control mode before proceeding.

**Wrong Code:**

```java
// ❌ NEVER DO THIS
public void generateApp(String requirement) {
    // starts immediately, ignores admin mode
    codeGenerator.generate(requirement);
}
```

**Fixed Code:**

```java
// ✅ CORRECT — always check admin mode first
@Autowired
private AdminControlService adminControlService;

public void generateApp(String requirement) {
    AdminMode mode = adminControlService.getCurrentMode();
    if (mode == AdminMode.FORCE_STOP) {
        throw new OperationBlockedException("System is in FORCE_STOP mode");
    }
    if (mode == AdminMode.WAIT) {
        return adminControlService.queueForApproval(requirement);
    }
    // AUTO mode — proceed immediately
    codeGenerator.generate(requirement);
}
```

**Rule:** Every Service method that modifies state must check `adminControlService.getCurrentMode()` as its first operation.

---

## 📋 QUICK REFERENCE: MISTAKE PREVENTION CHECKLIST

Use this before every PR review:

- [ ] No open `/api/auth/init` or unprotected setup endpoints
- [ ] All `ProcessBuilder` calls use array arguments (not string concatenation)
- [ ] All env vars validated in `@PostConstruct` at startup
- [ ] All external API calls capture `ResponseEntity` (not request)
- [ ] All `ProcessBuilder` calls have `redirectErrorStream(false)`
- [ ] All Git operations check output text, not just exit code
- [ ] Commit hash extraction uses regex + `git rev-parse HEAD` fallback
- [ ] All user inputs validated for null, length, and character set
- [ ] Markdownlint CI uses batched file processing (≤20 files per call)
- [ ] `.markdownlint.json` starts with `"default": true`
- [ ] All `catch` blocks call `learningService.recordError()`
- [ ] All state-modifying service methods check admin mode first

---

*Every entry in this document represents a real error that occurred in SupremeAI.*
*When a new error is resolved, it should be added here. See [DOCUMENTATION_MAINTENANCE_STRATEGY.md](DOCUMENTATION_MAINTENANCE_STRATEGY.md).*
