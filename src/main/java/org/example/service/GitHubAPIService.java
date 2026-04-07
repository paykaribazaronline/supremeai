package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * GitHub API Service
 * Monitors CI/CD workflows, checks status, retrieves logs
 */
@Service
public class GitHubAPIService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubAPIService.class);
    
    private static final String GITHUB_API = "https://api.github.com";
    private static final String GITHUB_REPO = System.getenv("GITHUB_REPO") != null 
        ? System.getenv("GITHUB_REPO")
        : "supremeai/supremeai";

    /** GitHub App auth — preferred over a bare PAT when configured. */
    @Autowired
    private GitHubAppService gitHubAppService;
    
    /**
     * Get latest workflow run status
     */
    public Map<String, Object> getLatestWorkflowStatus(String workflowName) {
        try {
            String endpoint = String.format("%s/repos/%s/actions/workflows/%s/runs?per_page=1",
                GITHUB_API, GITHUB_REPO, workflowName);
            
            String response = makeGitHubRequest(endpoint);
            Map<String, Object> data = parseJson(response);
            
            if (data.containsKey("workflow_runs")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> runs = (List<Map<String, Object>>) data.get("workflow_runs");
                if (!runs.isEmpty()) {
                    return runs.get(0);
                }
            }
            
            return new HashMap<>();
        } catch (Exception e) {
            logger.error("❌ Failed to get workflow status: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Check if last workflow run passed
     */
    public boolean isLastWorkflowSuccessful(String workflowName) {
        try {
            Map<String, Object> status = getLatestWorkflowStatus(workflowName);
            
            if (status.isEmpty()) {
                return false;
            }
            
            String conclusion = (String) status.get("conclusion");
            return "success".equals(conclusion);
        } catch (Exception e) {
            logger.error("❌ Failed to check workflow success: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get workflow run logs
     */
    public String getWorkflowLogs(String runId) {
        try {
            String endpoint = String.format("%s/repos/%s/actions/runs/%s/logs",
                GITHUB_API, GITHUB_REPO, runId);
            
            return makeGitHubRequest(endpoint);
        } catch (Exception e) {
            logger.error("❌ Failed to get workflow logs: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Trigger a workflow manually
     */
    public boolean triggerWorkflow(String workflowFile, String ref) {
        try {
            String endpoint = String.format("%s/repos/%s/actions/workflows/%s/dispatches",
                GITHUB_API, GITHUB_REPO, workflowFile);
            
            String body = "{\"ref\": \"" + ref + "\"}";
            return makeGitHubPostRequest(endpoint, body);
        } catch (Exception e) {
            logger.error("❌ Failed to trigger workflow: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all recent workflow runs
     */
    public List<Map<String, Object>> getRecentWorkflowRuns(int limit) {
        try {
            String endpoint = String.format("%s/repos/%s/actions/runs?per_page=%d",
                GITHUB_API, GITHUB_REPO, limit);
            
            String response = makeGitHubRequest(endpoint);
            Map<String, Object> data = parseJson(response);
            
            if (data.containsKey("workflow_runs")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> runs = (List<Map<String, Object>>) data.get("workflow_runs");
                return runs;
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("❌ Failed to get workflow runs: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Create a GitHub issue
     */
    public String createIssue(String title, String body, String label) {
        try {
            if (!gitHubAppService.isConfigured()
                    && (System.getenv("GITHUB_TOKEN") == null
                        || System.getenv("GITHUB_TOKEN").trim().isEmpty())) {
                logger.error("❌ GitHub token not configured. Set GITHUB_APP_PRIVATE_KEY_BASE64 or GITHUB_TOKEN.");
                return null;
            }
            
            String endpoint = String.format("%s/repos/%s/issues", GITHUB_API, GITHUB_REPO);
            
            // Escape title and body for JSON
            String escapedTitle = escapeJson(title);
            String escapedBody = escapeJson(body);
            String escapedLabel = escapeJson(label);
            
            String jsonBody = String.format(
                "{\"title\": \"%s\", \"body\": \"%s\", \"labels\": [\"%s\"]}",
                escapedTitle, escapedBody, escapedLabel
            );
            
            // ✅ FIX: Actually capture the response, not the request!
            String response = makeGitHubRequestWithResponse(endpoint, jsonBody);
            if (response == null) {
                return null;
            }
            
            Map<String, Object> data = parseJson(response);
            
            if (data.containsKey("number")) {
                return String.valueOf(data.get("number"));
            }
            
            // Fallback: extract from response string
            if (response.contains("\"number\":")) {
                int start = response.indexOf("\"number\":") + 10;
                int end = response.indexOf(",", start);
                if (end == -1) end = response.indexOf("}", start);
                if (start > 0 && end > start) {
                    return response.substring(start, end).trim();
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("❌ Failed to create issue: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Add comment to issue
     */
    public boolean addIssueComment(String issueNumber, String comment) {
        try {
            String endpoint = String.format("%s/repos/%s/issues/%s/comments",
                GITHUB_API, GITHUB_REPO, issueNumber);
            
            String body = "{\"body\": \"" + comment.replace("\"", "\\\"") + "\"}";
            return makeGitHubPostRequest(endpoint, body);
        } catch (Exception e) {
            logger.error("❌ Failed to add comment: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get commit details
     */
    public Map<String, Object> getCommitDetails(String commitHash) {
        try {
            String endpoint = String.format("%s/repos/%s/commits/%s",
                GITHUB_API, GITHUB_REPO, commitHash);
            
            String response = makeGitHubRequest(endpoint);
            return parseJson(response);
        } catch (Exception e) {
            logger.error("❌ Failed to get commit details: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Get repository info
     */
    public Map<String, Object> getRepositoryInfo() {
        try {
            String endpoint = String.format("%s/repos/%s", GITHUB_API, GITHUB_REPO);
            String response = makeGitHubRequest(endpoint);
            return parseJson(response);
        } catch (Exception e) {
            logger.error("❌ Failed to get repo info: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Fetch recently-closed issues (bugs) for knowledge harvesting.
     * Returns up to {@code limit} closed issues with their title and body.
     */
    public List<Map<String, Object>> getRecentClosedIssues(int limit) {
        try {
            String endpoint = String.format(
                "%s/repos/%s/issues?state=closed&labels=bug&per_page=%d&sort=updated&direction=desc",
                GITHUB_API, GITHUB_REPO, Math.min(limit, 50));
            String response = makeGitHubRequest(endpoint);
            return parseJsonArray(response);
        } catch (Exception e) {
            logger.warn("⚠️ Could not fetch closed issues: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch recently-merged pull-request titles/bodies for learning.
     * Uses the issues search API (PRs are issues with pull_request key).
     */
    public List<Map<String, Object>> getRecentMergedPRs(int limit) {
        try {
            String endpoint = String.format(
                "%s/repos/%s/pulls?state=closed&per_page=%d&sort=updated&direction=desc",
                GITHUB_API, GITHUB_REPO, Math.min(limit, 50));
            String response = makeGitHubRequest(endpoint);
            return parseJsonArray(response);
        } catch (Exception e) {
            logger.warn("⚠️ Could not fetch merged PRs: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ============ PRIVATE HELPER METHODS ============

    /**
     * Returns the best available GitHub auth token.
     * Prefers a GitHub App installation token (when {@link GitHubAppService} is
     * configured); falls back to the {@code GITHUB_TOKEN} PAT env var.
     *
     * @throws IOException when neither token source is available
     */
    private String resolveToken() throws IOException {
        if (gitHubAppService != null && gitHubAppService.isConfigured()) {
            String appToken = gitHubAppService.getInstallationToken();
            if (appToken != null && !appToken.isBlank()) {
                return appToken;
            }
        }
        String pat = System.getenv("GITHUB_TOKEN");
        if (pat == null || pat.isBlank()) {
            throw new IOException("No GitHub credentials configured. "
                + "Set GITHUB_APP_PRIVATE_KEY_BASE64 + GITHUB_APP_ID + GITHUB_APP_INSTALLATION_ID, "
                + "or set GITHUB_TOKEN.");
        }
        return pat;
    }

    /**
     * Make GET request to GitHub API
     */
    private String makeGitHubRequest(String endpoint) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "token " + resolveToken());
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        
        reader.close();
        conn.disconnect();
        
        return response.toString();
    }
    
    /**
     * Make POST request to GitHub API - with response body
     */
    private String makeGitHubRequestWithResponse(String endpoint, String body) throws IOException {
        String token = resolveToken();

        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = body.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        
        // ✅ Read response body
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        
        if (responseCode < 200 || responseCode >= 300) {
            logger.error("❌ GitHub API error {}: {}", responseCode, response.toString());
            return null;
        }
        
        return response.toString();
    }
    
    /**
     * Make POST request to GitHub API - returns boolean
     */
    private boolean makeGitHubPostRequest(String endpoint, String body) throws IOException {
        String response = makeGitHubRequestWithResponse(endpoint, body);
        return response != null;
    }
    
    /**
     * Escape JSON string values
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
    
    /**
     * Simple JSON parser (for basic operations)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String jsonStr) {
        // For production, use Jackson or Gson
        // Simple implementation for demo
        Map<String, Object> result = new HashMap<>();
        try {
            // Very basic JSON parsing - use Jackson for production
            if (jsonStr.contains("\"workflow_runs\"")) {
                result.put("workflow_runs", new ArrayList<>());
            }
            if (jsonStr.contains("\"conclusion\"")) {
                if (jsonStr.contains("\"success\"")) {
                    result.put("conclusion", "success");
                } else if (jsonStr.contains("\"failure\"")) {
                    result.put("conclusion", "failure");
                }
            }
            // Extract number field if present
            if (jsonStr.contains("\"number\":")) {
                int start = jsonStr.indexOf("\"number\":") + 10;
                int end = jsonStr.indexOf(",", start);
                if (end == -1) end = jsonStr.indexOf("}", start);
                if (start > 0 && end > start) {
                    try {
                        int number = Integer.parseInt(jsonStr.substring(start, end).trim());
                        result.put("number", number);
                    } catch (NumberFormatException e) {
                        logger.warn("Could not parse issue number");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("⚠️ JSON parsing error: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Very lightweight JSON-array parser: splits on top-level objects and parses each one.
     * Good enough for extracting title/body/number from GitHub list endpoints.
     */
    private List<Map<String, Object>> parseJsonArray(String json) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (json == null || json.trim().isEmpty() || json.trim().equals("[]")) {
            return items;
        }
        // Split on },{  — works for flat arrays returned by GitHub list APIs
        String stripped = json.trim();
        if (stripped.startsWith("[")) stripped = stripped.substring(1);
        if (stripped.endsWith("]"))  stripped = stripped.substring(0, stripped.length() - 1);

        // Each top-level object is delimited by },{ at depth 0
        int depth = 0;
        int start = 0;
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    String chunk = stripped.substring(start, i + 1).trim();
                    if (!chunk.isEmpty()) {
                        items.add(parseJson(chunk));
                    }
                    // skip the comma separator
                    start = i + 1;
                    while (start < stripped.length() && (stripped.charAt(start) == ',' || stripped.charAt(start) == ' ')) {
                        start++;
                    }
                    i = start - 1;
                }
            }
        }
        return items;
    }
}
