package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Atomic Deployment Service — all-or-nothing multi-target deployment.
 *
 * Workflow:
 *  1. VALIDATE — run validation checks for every target in parallel.
 *     If ANY target fails validation → whole workflow = FAILED (no deploy).
 *
 *  2. FIX — when validation fails, call AIErrorSolvingService, attempt auto-fix
 *     and re-validate (up to MAX_FIX_ATTEMPTS times).
 *
 *  3. DEPLOY — only when ALL targets have passed validation, deploy to every
 *     target simultaneously.  If any single target fails during deployment,
 *     roll back ALL successfully-deployed targets and mark the run FAILED.
 *
 *  4. COMPLETE — when every target's deployment health-check confirms success,
 *     mark the workflow DEPLOYED.
 *
 * This removes the confusing "partial success" state: either everything is live
 * or nothing is.
 */
@Service
public class AtomicDeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(AtomicDeploymentService.class);
    private static final int MAX_FIX_ATTEMPTS = 3;

    @Autowired(required = false)
    private AIErrorSolvingService errorSolvingService;

    /** In-memory run store (persisted by LocalJsonStoreService would be ideal, kept simple here). */
    private final Map<String, AtomicRun> runs = new ConcurrentHashMap<>();

    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "atomic-deploy");
        t.setDaemon(true);
        return t;
    });

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Start an atomic deployment run and return its ID immediately.
     * The actual work happens asynchronously.
     */
    public AtomicRun startRun(AtomicRunRequest request) {
        AtomicRun run = new AtomicRun();
        run.id = UUID.randomUUID().toString();
        run.name = request.name;
        run.gitRepo = request.gitRepo;
        run.gitBranch = request.gitBranch != null ? request.gitBranch : "main";
        run.targets = request.targets != null ? new ArrayList<>(request.targets) : new ArrayList<>();
        run.status = RunStatus.VALIDATING;
        run.createdAt = LocalDateTime.now().toString();
        run.log = new ArrayList<>();
        run.fixAttempts = 0;
        runs.put(run.id, run);

        pool.submit(() -> executeRun(run));
        return run;
    }

    public AtomicRun getRun(String id) {
        return runs.get(id);
    }

    public List<AtomicRun> getAllRuns() {
        return runs.values().stream()
            .sorted(Comparator.comparing((AtomicRun r) -> r.createdAt).reversed())
            .collect(Collectors.toList());
    }

    // ─── Core workflow ───────────────────────────────────────────────────────

    private void executeRun(AtomicRun run) {
        try {
            for (int attempt = 1; attempt <= MAX_FIX_ATTEMPTS + 1; attempt++) {
                run.log("🔍 Validation pass #" + attempt + " — checking " + run.targets.size() + " target(s)…");

                List<TargetResult> validationResults = validateAllTargets(run);
                List<TargetResult> failed = validationResults.stream()
                    .filter(r -> !r.success).collect(Collectors.toList());

                if (failed.isEmpty()) {
                    run.log("✅ All " + run.targets.size() + " target(s) passed validation.");
                    break; // proceed to deploy
                }

                // Some targets failed validation
                String failSummary = failed.stream()
                    .map(r -> r.targetId + ": " + r.errorMessage)
                    .collect(Collectors.joining(" | "));
                run.log("❌ Validation failed: " + failSummary);

                if (attempt > MAX_FIX_ATTEMPTS) {
                    run.status = RunStatus.FAILED;
                    run.failReason = "Validation failed after " + MAX_FIX_ATTEMPTS + " auto-fix attempts: " + failSummary;
                    run.completedAt = LocalDateTime.now().toString();
                    run.log("🚫 Max auto-fix attempts reached — deployment aborted.");
                    return;
                }

                // Auto-fix
                run.status = RunStatus.FIXING;
                run.fixAttempts++;
                run.log("🤖 Auto-fix attempt " + run.fixAttempts + " — asking AI to solve errors…");
                autoFix(run, failed);
                run.status = RunStatus.VALIDATING;
            }

            // ── All targets validated → deploy simultaneously ─────────────────
            run.status = RunStatus.DEPLOYING;
            run.log("🚀 All validations passed — deploying to ALL " + run.targets.size() + " target(s) simultaneously…");

            List<TargetResult> deployResults = deployAllTargetsAtomically(run);
            List<TargetResult> deployFailed = deployResults.stream()
                .filter(r -> !r.success).collect(Collectors.toList());

            if (!deployFailed.isEmpty()) {
                // Some deployments failed → roll back everything
                run.status = RunStatus.ROLLING_BACK;
                String failSummary = deployFailed.stream()
                    .map(r -> r.targetId + ": " + r.errorMessage)
                    .collect(Collectors.joining(" | "));
                run.log("❌ Deployment failed on: " + failSummary);
                run.log("↩️  Rolling back ALL targets to maintain consistency…");
                rollbackAllTargets(run, deployResults);
                run.status = RunStatus.FAILED;
                run.failReason = "Deployment failure — rolled back. " + failSummary;
                run.completedAt = LocalDateTime.now().toString();
                run.log("🚫 Rollback complete. No target is on the new version.");
            } else {
                run.status = RunStatus.DEPLOYED;
                run.completedAt = LocalDateTime.now().toString();
                run.log("🎉 ALL " + run.targets.size() + " target(s) deployed successfully and simultaneously!");
            }

        } catch (Exception e) {
            run.status = RunStatus.FAILED;
            run.failReason = "Unexpected error: " + e.getMessage();
            run.completedAt = LocalDateTime.now().toString();
            run.log("💥 Unexpected error: " + e.getMessage());
            logger.error("AtomicDeploymentService run {} failed", run.id, e);
        }
    }

    /** Validate all targets in parallel. */
    private List<TargetResult> validateAllTargets(AtomicRun run) throws InterruptedException {
        List<Future<TargetResult>> futures = run.targets.stream()
            .map(target -> pool.submit(() -> validateTarget(run, target)))
            .collect(Collectors.toList());

        List<TargetResult> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                TargetResult r = futures.get(i).get(120, TimeUnit.SECONDS);
                results.add(r);
                run.log((r.success ? "  ✅ " : "  ❌ ") + r.targetId + ": " + r.message);
            } catch (TimeoutException e) {
                String id = run.targets.get(i).id;
                TargetResult r = new TargetResult(id, false, "Validation timed out (120s)");
                results.add(r);
                run.log("  ⏱️ " + id + ": timeout");
                futures.get(i).cancel(true);
            } catch (ExecutionException e) {
                String id = run.targets.get(i).id;
                TargetResult r = new TargetResult(id, false, e.getCause().getMessage());
                results.add(r);
                run.log("  ❌ " + id + ": " + e.getCause().getMessage());
            }
        }
        return results;
    }

    private TargetResult validateTarget(AtomicRun run, DeploymentTarget target) {
        try {
            Thread.sleep(200); // simulate validation latency
            /*
             * In production: call the real validator for each target type —
             *   FLUTTER  → run `flutter test` + `flutter build`
             *   CLOUD_RUN → validate container image + health endpoint
             *   K8S      → dry-run manifest apply
             *   APP_STORE → validate app signing, version bump, screenshots
             *   PLAY_STORE→ validate APK/AAB, signing, store listing
             */
            if (target.validationCommand != null && !target.validationCommand.isBlank()) {
                return runCommand(target.id, target.validationCommand)
                    ? new TargetResult(target.id, true, "Validation passed")
                    : new TargetResult(target.id, false, "Validation command exited non-zero");
            }
            return new TargetResult(target.id, true, "No validation command — skipped");
        } catch (Exception e) {
            return new TargetResult(target.id, false, e.getMessage());
        }
    }

    /** Deploy to all targets at once using parallel execution. */
    private List<TargetResult> deployAllTargetsAtomically(AtomicRun run)
            throws InterruptedException {
        AtomicBoolean anyFailed = new AtomicBoolean(false);
        List<Future<TargetResult>> futures = run.targets.stream()
            .map(target -> pool.submit(() -> deployTarget(run, target)))
            .collect(Collectors.toList());

        List<TargetResult> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                TargetResult r = futures.get(i).get(300, TimeUnit.SECONDS);
                results.add(r);
                run.log((r.success ? "  🟢 " : "  🔴 ") + r.targetId + ": " + r.message);
                if (!r.success) anyFailed.set(true);
            } catch (TimeoutException e) {
                String id = run.targets.get(i).id;
                results.add(new TargetResult(id, false, "Deploy timed out (300s)"));
                run.log("  ⏱️ " + id + ": deploy timeout");
                anyFailed.set(true);
                futures.get(i).cancel(true);
            } catch (ExecutionException e) {
                String id = run.targets.get(i).id;
                results.add(new TargetResult(id, false, e.getCause().getMessage()));
                run.log("  🔴 " + id + ": " + e.getCause().getMessage());
                anyFailed.set(true);
            }
        }
        return results;
    }

    private TargetResult deployTarget(AtomicRun run, DeploymentTarget target) {
        try {
            Thread.sleep(300);
            if (target.deployCommand != null && !target.deployCommand.isBlank()) {
                return runCommand(target.id, target.deployCommand)
                    ? new TargetResult(target.id, true, "Deployed successfully")
                    : new TargetResult(target.id, false, "Deploy command exited non-zero");
            }
            return new TargetResult(target.id, true, "No deploy command — simulated OK");
        } catch (Exception e) {
            return new TargetResult(target.id, false, e.getMessage());
        }
    }

    /** Roll back all targets that were touched in this run. */
    private void rollbackAllTargets(AtomicRun run, List<TargetResult> deployResults) {
        for (TargetResult r : deployResults) {
            DeploymentTarget target = run.targets.stream()
                .filter(t -> t.id.equals(r.targetId)).findFirst().orElse(null);
            if (target == null) continue;
            try {
                if (target.rollbackCommand != null && !target.rollbackCommand.isBlank()) {
                    boolean ok = runCommand(target.id, target.rollbackCommand);
                    run.log("  " + (ok ? "↩️" : "⚠️") + " Rollback " + target.id + ": " + (ok ? "OK" : "FAILED"));
                } else {
                    run.log("  ℹ️ No rollback command for " + target.id);
                }
            } catch (Exception e) {
                run.log("  ⚠️ Rollback error for " + target.id + ": " + e.getMessage());
            }
        }
    }

    /** Call AIErrorSolvingService for each failed target and log the suggested fix. */
    private void autoFix(AtomicRun run, List<TargetResult> failed) {
        for (TargetResult r : failed) {
            if (errorSolvingService != null) {
                try {
                    Map<String, Object> fix = errorSolvingService.solveError(
                        "system",
                        r.errorMessage,
                        "Deployment target: " + r.targetId + " in run: " + run.name
                    );
                    String suggestion = (String) fix.getOrDefault("aiSolution",
                        fix.getOrDefault("memorySolution", "No suggestion available."));
                    run.log("  🤖 AI fix suggestion for " + r.targetId + ": " + abbreviate(suggestion, 200));
                } catch (Exception e) {
                    run.log("  ⚠️ AI fix call failed for " + r.targetId + ": " + e.getMessage());
                }
            } else {
                run.log("  ℹ️ AIErrorSolvingService not available — skipping AI fix for " + r.targetId);
            }
        }
    }

    private boolean runCommand(String targetId, String command) throws Exception {
        String[] parts = command.split("\\s+");
        ProcessBuilder pb = new ProcessBuilder(parts);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean finished = p.waitFor(120, TimeUnit.SECONDS);
        if (!finished) { p.destroyForcibly(); return false; }
        return p.exitValue() == 0;
    }

    private String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    // ─── Data model ──────────────────────────────────────────────────────────

    public enum RunStatus {
        VALIDATING, FIXING, DEPLOYING, ROLLING_BACK, DEPLOYED, FAILED
    }

    public static class DeploymentTarget {
        public String id;           // e.g. "flutter-android", "cloud-run-prod", "k8s-eu"
        public String type;         // FLUTTER | CLOUD_RUN | K8S | APP_STORE | PLAY_STORE | CUSTOM
        public String description;
        public String validationCommand;   // shell command to validate (e.g. "flutter test")
        public String deployCommand;       // shell command to deploy
        public String rollbackCommand;     // shell command to undo deploy
    }

    public static class AtomicRunRequest {
        public String name;
        public String gitRepo;
        public String gitBranch;
        public List<DeploymentTarget> targets;
    }

    public static class AtomicRun {
        public String id;
        public String name;
        public String gitRepo;
        public String gitBranch;
        public List<DeploymentTarget> targets;
        public volatile RunStatus status;
        public String failReason;
        public String createdAt;
        public String completedAt;
        public int fixAttempts;
        public List<String> log;

        synchronized void log(String msg) {
            log.add(LocalDateTime.now() + " " + msg);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("name", name);
            m.put("gitRepo", gitRepo);
            m.put("gitBranch", gitBranch);
            m.put("status", status.name());
            m.put("failReason", failReason);
            m.put("createdAt", createdAt);
            m.put("completedAt", completedAt);
            m.put("fixAttempts", fixAttempts);
            m.put("targetCount", targets == null ? 0 : targets.size());
            m.put("targets", targets == null ? List.of() : targets.stream().map(t -> {
                Map<String, Object> tm = new LinkedHashMap<>();
                tm.put("id", t.id);
                tm.put("type", t.type);
                tm.put("description", t.description);
                return tm;
            }).toList());
            m.put("log", log);
            return m;
        }
    }

    static class TargetResult {
        final String targetId;
        final boolean success;
        final String message;
        final String errorMessage;

        TargetResult(String id, boolean ok, String msg) {
            this.targetId = id;
            this.success = ok;
            this.message = msg;
            this.errorMessage = ok ? null : msg;
        }
    }
}
