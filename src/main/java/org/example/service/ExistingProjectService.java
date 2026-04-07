package org.example.service;

import org.example.model.ConsensusVote;
import org.example.model.ExistingProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ExistingProjectService — Continuous improvement of admin-registered external GitHub repos.
 *
 * Workflow per project:
 *  1. Admin registers a repo + goal via the admin dashboard.
 *  2. Admin chats with SupremeAI to refine the plan (discuss endpoint).
 *  3. On each improvement cycle:
 *       a) Clone (first time) or pull latest changes.
 *       b) Ask MultiAI to analyse the repo structure & goal and produce a concrete list of changes.
 *       c) Commit & push changes back to the branch.
 *  4. If continuousImprovement=true, cycle repeats automatically during idle time.
 */
@Service
public class ExistingProjectService {
    private static final Logger logger = LoggerFactory.getLogger(ExistingProjectService.class);

    @Autowired
    private MultiAIConsensusService consensusService;

    @Autowired
    private IdleResearchService idleResearchService;

    /** In-memory store: projectId → ExistingProject */
    private final Map<String, ExistingProject> projects = new ConcurrentHashMap<>();

    /** Dedicated executor for async improvement cycles (avoids starving the common ForkJoinPool). */
    private final ExecutorService improvementExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "project-improvement");
        t.setDaemon(true);
        return t;
    });

    /**
     * Root directory where external repos are checked out.
     * Falls back to /tmp/supremeai-external-projects when the env var is absent.
     */
    private static final String WORKSPACE = Optional.ofNullable(System.getenv("EXTERNAL_PROJECTS_WORKSPACE"))
            .filter(s -> !s.isBlank())
            .orElse(System.getProperty("java.io.tmpdir") + File.separator + "supremeai-external-projects");

    /** Minimum interval (30 minutes in milliseconds) between automatic improvement cycles for the same project. */
    private static final long MIN_CYCLE_INTERVAL_MS = 30 * 60 * 1000;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(WORKSPACE));
            logger.info("✅ ExistingProjectService ready — workspace: {}", WORKSPACE);
        } catch (Exception e) {
            logger.warn("⚠️ Could not create workspace dir {}: {}", WORKSPACE, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Register a new external project for continuous improvement.
     */
    public ExistingProject registerProject(String name, String repoUrl, String branch,
                                           String repoToken, String improvementGoal) {
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new IllegalArgumentException("repoUrl is required");
        }
        if (name == null || name.isBlank()) {
            name = extractRepoName(repoUrl);
        }
        ExistingProject project = new ExistingProject();
        project.setName(name);
        project.setRepoUrl(repoUrl);
        project.setBranch(branch != null && !branch.isBlank() ? branch : "main");
        project.setRepoToken(repoToken);
        project.setImprovementGoal(improvementGoal);
        project.setStatus("REGISTERED");

        // Welcome message
        String welcomeMsg = String.format(
            "Hello! I've registered the project '%s' (%s, branch: %s). " +
            "Your goal is: \"%s\". " +
            "I'll start by cloning the repository and analysing the code. " +
            "Feel free to add more details or constraints to the plan via this conversation.",
            project.getName(), repoUrl, project.getBranch(),
            improvementGoal != null ? improvementGoal : "(not specified yet)");
        project.addConversationMessage("ai", welcomeMsg);

        projects.put(project.getId(), project);
        logger.info("📝 Registered existing project: {} ({})", project.getName(), project.getId());
        return project;
    }

    /**
     * Return all registered projects (summary map, no token).
     */
    public List<Map<String, Object>> listProjects() {
        return projects.values().stream()
                .sorted(Comparator.comparingLong(ExistingProject::getCreatedAt).reversed())
                .map(ExistingProject::toSummaryMap)
                .collect(Collectors.toList());
    }

    /**
     * Return full project details.
     */
    public ExistingProject getProject(String id) {
        return projects.get(id);
    }

    /**
     * Remove a project from tracking (does NOT delete the cloned repo from disk).
     */
    public boolean removeProject(String id) {
        return projects.remove(id) != null;
    }

    /**
     * Enable or disable continuous improvement for a project.
     */
    public ExistingProject toggleContinuous(String id, boolean enabled) {
        ExistingProject project = requireProject(id);
        project.setContinuousImprovement(enabled);
        logger.info("🔄 Continuous improvement {} for project {}", enabled ? "ENABLED" : "DISABLED", project.getName());
        return project;
    }

    /**
     * Update the improvement goal and/or add a new admin message to the conversation.
     * The AI replies with an updated plan.
     */
    public Map<String, Object> discuss(String id, String adminMessage) {
        ExistingProject project = requireProject(id);

        if (adminMessage != null && !adminMessage.isBlank()) {
            project.addConversationMessage("admin", adminMessage);

            // If the message looks like a new goal, update the goal field too
            if (adminMessage.length() > 20) {
                project.setImprovementGoal(adminMessage);
            }
        }

        // Build context prompt from full conversation
        String contextPrompt = buildDiscussionPrompt(project, adminMessage);

        String aiReply;
        try {
            ConsensusVote vote = consensusService.askAllAISystemLevel(contextPrompt);
            String raw = (vote != null && vote.getWinningResponse() != null)
                    ? vote.getWinningResponse()
                    : null;
            // Filter out fallback/quota markers so the admin sees a useful message
            aiReply = isAIUnavailableResponse(raw)
                    ? "I've noted your input and will apply it during the next improvement cycle."
                    : raw;
        } catch (Exception e) {
            logger.warn("⚠️ AI reply failed for project {}: {}", project.getId(), e.getMessage());
            aiReply = "I've recorded your update. The improvement will be applied on the next cycle.";
        }

        project.addConversationMessage("ai", aiReply);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", id);
        result.put("aiReply", aiReply);
        result.put("conversationLength", project.getConversation().size());
        return result;
    }

    /**
     * Trigger an immediate improvement cycle for a project.
     * Returns a report of what was done.
     */
    public Map<String, Object> triggerImprovement(String id) {
        ExistingProject project = requireProject(id);
        logger.info("⚡ Admin triggered improvement for project: {}", project.getName());
        return runImprovementCycle(project);
    }

    /**
     * Trigger an improvement cycle asynchronously (fire-and-forget).
     * Used by the REST controller so the HTTP request returns immediately.
     */
    public void triggerImprovementAsync(String id) {
        ExistingProject project = requireProject(id);
        logger.info("⚡ Admin triggered async improvement for project: {}", project.getName());
        CompletableFuture.runAsync(() -> {
            try {
                runImprovementCycle(project);
            } catch (Exception e) {
                logger.error("❌ Async improvement failed for {}: {}", project.getName(), e.getMessage());
                project.setStatus("ERROR");
            }
        }, improvementExecutor);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SCHEDULED — runs every 5 minutes, processes projects with continuous=true
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 3 * 60 * 1000)
    public void runScheduledImprovements() {
        for (ExistingProject project : projects.values()) {
            if (!project.isContinuousImprovement()) continue;
            if ("IMPROVING".equals(project.getStatus()) || "ANALYSING".equals(project.getStatus())) continue;

            // Respect minimum interval between cycles
            long elapsed = System.currentTimeMillis() - project.getLastImprovedAt();
            if (project.getLastImprovedAt() > 0 && elapsed < MIN_CYCLE_INTERVAL_MS) continue;

            try {
                logger.info("🔄 Scheduled improvement cycle for: {}", project.getName());
                runImprovementCycle(project);
            } catch (Exception e) {
                logger.warn("⚠️ Scheduled improvement failed for {}: {}", project.getName(), e.getMessage());
                project.setStatus("ERROR");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORE — improvement cycle
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> runImprovementCycle(ExistingProject project) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("projectId", project.getId());
        report.put("projectName", project.getName());
        report.put("startedAt", System.currentTimeMillis());

        // Notify idle-research so it pauses while we run
        idleResearchService.notifyProjectActivity();

        try {
            // Step 1: Clone or pull
            project.setStatus("ANALYSING");
            boolean repoReady = prepareRepository(project);
            if (!repoReady) {
                project.setStatus("ERROR");
                report.put("error", "Failed to clone/pull repository. Check repoUrl and token.");
                project.addImprovementRecord("Clone/pull failed", List.of(), false);
                return report;
            }
            project.setLastAnalysedAt(System.currentTimeMillis());

            // Step 2: Collect repo structure summary
            String repoSnapshot = collectRepoSnapshot(project);

            // Step 3: Ask AI for specific improvements
            project.setStatus("IMPROVING");
            String improvementPrompt = buildImprovementPrompt(project, repoSnapshot);
            ConsensusVote vote = consensusService.askAllAISystemLevel(improvementPrompt);

            String aiSuggestion = (vote != null && vote.getWinningResponse() != null)
                    ? vote.getWinningResponse()
                    : null;

            if (isAIUnavailableResponse(aiSuggestion)) {
                project.setStatus("IDLE");
                report.put("skipped", "AI providers unavailable or quota exceeded");
                return report;
            }

            project.setLatestAnalysis(truncate(aiSuggestion, 2000));

            // Step 4: Commit the AI analysis as an improvement note file so the repo has a record
            List<String> filesChanged = writeImprovementNote(project, aiSuggestion);

            // Step 5: Commit & push
            boolean pushed = commitAndPush(project, "SupremeAI improvement: " + truncate(project.getImprovementGoal(), 60));
            project.addImprovementRecord(truncate(aiSuggestion, 500), filesChanged, pushed);

            // Add conversation message
            String summary = String.format(
                "✅ Improvement cycle complete. %s. AI analysed the repo and %s.",
                pushed ? "Changes committed & pushed" : "Changes noted (push skipped)",
                "proposed: " + truncate(aiSuggestion, 200));
            project.addConversationMessage("ai", summary);

            project.setStatus("IDLE");

            report.put("status", "completed");
            report.put("aiSuggestion", truncate(aiSuggestion, 1000));
            report.put("filesChanged", filesChanged);
            report.put("pushed", pushed);
            report.put("finishedAt", System.currentTimeMillis());

            logger.info("✅ Improvement cycle complete for: {} (pushed={})", project.getName(), pushed);

        } catch (Exception e) {
            logger.error("❌ Improvement cycle failed for {}: {}", project.getName(), e.getMessage());
            project.setStatus("ERROR");
            project.addImprovementRecord("Error: " + e.getMessage(), List.of(), false);
            report.put("error", e.getMessage());
        }

        return report;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS — git operations
    // ─────────────────────────────────────────────────────────────────────────

    private boolean prepareRepository(ExistingProject project) {
        Path projectDir = repoDir(project);
        try {
            if (Files.exists(projectDir.resolve(".git"))) {
                // Pull latest
                logger.info("📥 Pulling latest for {} ({})", project.getName(), project.getBranch());
                int exit = runProcess(projectDir.toFile(), "git", "pull", "origin", project.getBranch());
                return exit == 0;
            } else {
                // Clone
                Files.createDirectories(projectDir);
                String cloneUrl = buildCloneUrl(project);
                logger.info("🔄 Cloning {} → {}", project.getRepoUrl(), projectDir);
                int exit = runProcess(Paths.get(WORKSPACE).toFile(),
                        "git", "clone", "--branch", project.getBranch(), "--depth", "1",
                        cloneUrl, projectDir.toString());
                return exit == 0;
            }
        } catch (Exception e) {
            logger.error("❌ prepareRepository failed: {}", e.getMessage());
            return false;
        }
    }

    /** Injects the PAT into the HTTPS URL when a token is provided. */
    private String buildCloneUrl(ExistingProject project) {
        String url = project.getRepoUrl();
        String token = project.getRepoToken();
        if (token != null && !token.isBlank() && url.startsWith("https://")) {
            // https://TOKEN@github.com/user/repo
            return url.replace("https://", "https://" + token + "@");
        }
        return url;
    }

    private List<String> writeImprovementNote(ExistingProject project, String content) {
        List<String> changed = new ArrayList<>();
        try {
            Path notesDir = repoDir(project).resolve(".supremeai");
            Files.createDirectories(notesDir);
            String filename = "improvement-" + System.currentTimeMillis() + ".md";
            Path noteFile = notesDir.resolve(filename);
            String noteContent = "# SupremeAI Improvement — " + new Date() + "\n\n"
                    + "**Goal:** " + project.getImprovementGoal() + "\n\n"
                    + "## AI Analysis\n\n" + content + "\n";
            Files.writeString(noteFile, noteContent);
            changed.add(".supremeai/" + filename);
        } catch (Exception e) {
            logger.warn("Could not write improvement note: {}", e.getMessage());
        }
        return changed;
    }

    private boolean commitAndPush(ExistingProject project, String commitMessage) {
        try {
            Path dir = repoDir(project);
            runProcess(dir.toFile(), "git", "add", "-A");
            int commitExit = runProcess(dir.toFile(),
                    "git", "commit", "-m", commitMessage,
                    "--author=SupremeAI <supremeai@noreply>");
            if (commitExit != 0) {
                logger.info("ℹ️ Nothing to commit for {}", project.getName());
                return false;
            }

            // Ensure the remote URL contains the token for push authentication.
            // Shallow clones may lose the embedded-token remote, and git-pull
            // using the default origin may strip it.
            String pushUrl = buildCloneUrl(project);
            runProcess(dir.toFile(), "git", "remote", "set-url", "origin", pushUrl);

            int pushExit = runProcess(dir.toFile(), "git", "push", "origin", project.getBranch());
            return pushExit == 0;
        } catch (Exception e) {
            logger.warn("⚠️ commitAndPush failed: {}", e.getMessage());
            return false;
        }
    }

    private int runProcess(File workDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        // Drain stdout/stderr to avoid blocking
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            br.lines().forEach(line -> logger.debug("[git] {}", line));
        }
        return proc.waitFor();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS — repo snapshot & prompts
    // ─────────────────────────────────────────────────────────────────────────

    private String collectRepoSnapshot(ExistingProject project) {
        Path dir = repoDir(project);
        StringBuilder sb = new StringBuilder();
        sb.append("Repository: ").append(project.getRepoUrl()).append("\n");
        sb.append("Branch: ").append(project.getBranch()).append("\n");
        sb.append("Files (top-level):\n");
        try {
            Files.list(dir)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .limit(30)
                    .forEach(p -> sb.append("  ").append(p.getFileName()).append("\n"));
        } catch (Exception e) {
            sb.append("  (could not list files)\n");
        }
        return sb.toString();
    }

    private String buildDiscussionPrompt(ExistingProject project, String adminMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are SupremeAI, an expert software improvement assistant.\n");
        sb.append("The admin has shared an existing project with you:\n\n");
        sb.append("Project: ").append(project.getName()).append("\n");
        sb.append("Repo: ").append(project.getRepoUrl()).append("\n");
        sb.append("Branch: ").append(project.getBranch()).append("\n");
        sb.append("Improvement Goal: ").append(project.getImprovementGoal()).append("\n\n");
        sb.append("Previous conversation:\n");
        for (Map<String, Object> msg : project.getConversation()) {
            sb.append(msg.get("role")).append(": ").append(msg.get("message")).append("\n");
        }
        sb.append("\nNew message from admin: ").append(adminMessage).append("\n\n");
        sb.append("Please respond with a clear, concise improvement plan or answer. ");
        sb.append("Be specific about what files or areas should change and why.");
        return sb.toString();
    }

    private String buildImprovementPrompt(ExistingProject project, String repoSnapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are SupremeAI, an expert software improvement assistant.\n\n");
        sb.append("Analyse the following existing project and produce a concrete list of improvements.\n\n");
        sb.append("=== PROJECT ===\n").append(repoSnapshot).append("\n");
        sb.append("=== ADMIN'S GOAL ===\n").append(project.getImprovementGoal()).append("\n\n");
        if (!project.getConversation().isEmpty()) {
            sb.append("=== DISCUSSION CONTEXT ===\n");
            project.getConversation().stream().limit(10).forEach(msg ->
                sb.append(msg.get("role")).append(": ").append(
                    truncate(String.valueOf(msg.get("message")), 300)).append("\n"));
            sb.append("\n");
        }
        sb.append("Respond with:\n");
        sb.append("1. A 2-3 sentence summary of what you found.\n");
        sb.append("2. A numbered list of specific, actionable improvements (max 5).\n");
        sb.append("3. Which files / areas each improvement targets.\n");
        sb.append("Keep the answer under 600 words.");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILS
    // ─────────────────────────────────────────────────────────────────────────

    private Path repoDir(ExistingProject project) {
        // Sanitize id to only alphanumeric + dashes
        String safeName = project.getId().replaceAll("[^a-zA-Z0-9\\-]", "");
        return Paths.get(WORKSPACE, safeName);
    }

    private String extractRepoName(String repoUrl) {
        String[] parts = repoUrl.replaceAll("\\.git$", "").split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "unnamed";
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    /**
     * Returns true when the AI response indicates that no real suggestion was produced
     * (quota exhausted, no providers configured, or local fallback).
     */
    private static boolean isAIUnavailableResponse(String response) {
        return response == null
                || response.contains("[QUOTA_EXCEEDED]")
                || response.contains("[NO_PROVIDERS_CONFIGURED]")
                || response.contains("[LOCAL_FALLBACK]");
    }

    private ExistingProject requireProject(String id) {
        ExistingProject p = projects.get(id);
        if (p == null) throw new NoSuchElementException("Project not found: " + id);
        return p;
    }
}
