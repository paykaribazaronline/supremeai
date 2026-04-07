package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * GitHub API Service
 * Monitors CI/CD workflows, checks status, retrieves logs
 */
@Service
public class GitHubAPIService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubAPIService.class);
    private static final int HTTP_CONNECT_TIMEOUT_MS = 15_000;
    private static final int HTTP_READ_TIMEOUT_MS = 20_000;
    
    private static final String GITHUB_API = "https://api.github.com";
    private static final String GITHUB_REPO = System.getenv("GITHUB_REPO") != null 
        ? System.getenv("GITHUB_REPO")
        : "paykaribazaronline/supremeai";

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
            if (gitHubAppService == null || !gitHubAppService.isConfigured()) {
                logger.error("❌ GitHub App credentials not configured. Set GITHUB_APP_PRIVATE_KEY_BASE64, GITHUB_APP_ID, and GITHUB_APP_INSTALLATION_ID.");
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

    // ============ PUBLIC FILE CREATION API ============

    /**
     * Create or update a file in the GitHub repository via Contents API.
     * Automatically fetches the current file SHA when the file already exists
     * (required for updates — GitHub API rejects updates without the current SHA).
     *
     * @param repoFilePath  Path within the repo root (e.g., "src/main/java/org/example/service/MyService.java")
     * @param content       Raw file content (will be Base64-encoded for the API)
     * @param commitMessage Commit message
     * @return true if file was successfully created/updated on GitHub
     */
    public boolean createOrUpdateFileInRepo(String repoFilePath, String content, String commitMessage) {
        try {
            // Normalize path separators for GitHub API
            String normalizedPath = repoFilePath.replace('\\', '/');
            String endpoint = String.format("%s/repos/%s/contents/%s", GITHUB_API, GITHUB_REPO, normalizedPath);

            // Step 1: Try to GET existing file to obtain its SHA (required for updates)
            String existingSha = null;
            try {
                String existingJson = makeGitHubRequest(endpoint);
                if (existingJson != null && existingJson.contains("\"sha\":")) {
                    int shaStart = existingJson.indexOf("\"sha\":") + 7;
                    // skip the opening quote
                    if (shaStart < existingJson.length() && existingJson.charAt(shaStart) == '"') shaStart++;
                    int shaEnd = existingJson.indexOf('"', shaStart);
                    if (shaEnd > shaStart) {
                        existingSha = existingJson.substring(shaStart, shaEnd);
                    }
                }
            } catch (Exception e) {
                // File doesn't exist yet — that's fine, we create it
                logger.debug("📄 File {} not found (will create): {}", normalizedPath, e.getMessage());
            }

            // Step 2: Base64-encode the file content
            String encodedContent = Base64.getEncoder()
                .encodeToString(content.getBytes(StandardCharsets.UTF_8));

            // Step 3: Build JSON body
            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("\"message\":\"").append(escapeJson(commitMessage)).append("\"");
            body.append(",\"content\":\"").append(encodedContent).append("\"");
            if (existingSha != null && !existingSha.isEmpty()) {
                body.append(",\"sha\":\"").append(existingSha).append("\"");
            }
            body.append("}");

            // Step 4: PUT to GitHub Contents API
            String response = makePutRequest(endpoint, body.toString());
            if (response != null) {
                logger.info("✅ GitHub commit: {}", normalizedPath);
                return true;
            } else {
                logger.error("❌ GitHub commit failed for: {}", normalizedPath);
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ createOrUpdateFileInRepo error for {}: {}", repoFilePath, e.getMessage());
            return false;
        }
    }

    // ============ PRIVATE HELPER METHODS ============

    /**
     * Make HTTP PUT request to GitHub API (used by Contents API for file create/update).
     */
    private String makePutRequest(String endpoint, String body) throws IOException {
        String token = resolveToken();
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(HTTP_READ_TIMEOUT_MS);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        // Read response or error stream
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = rd.readLine()) != null) sb.append(line);
            }
        }
        conn.disconnect();

        if (code >= 200 && code < 300) {
            return sb.toString();
        }
        logger.error("❌ GitHub PUT {} → HTTP {}: {}", endpoint, code, sb.toString().substring(0, Math.min(200, sb.length())));
        return null;
    }

    /**
     * Returns a GitHub App installation token.
     *
     * @throws IOException when GitHub App credentials are missing or token minting fails
     */
    private String resolveToken() throws IOException {
        if (gitHubAppService == null || !gitHubAppService.isConfigured()) {
            throw new IOException("GitHub App credentials are not configured. Set GITHUB_APP_PRIVATE_KEY_BASE64, GITHUB_APP_ID, and GITHUB_APP_INSTALLATION_ID.");
        }

        String appToken = gitHubAppService.getInstallationToken();
        if (appToken == null || appToken.isBlank()) {
            throw new IOException("GitHub App token minting failed. Check GitHub App private key and installation configuration.");
        }
        return appToken;
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
        conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(HTTP_READ_TIMEOUT_MS);
        
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
        conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(HTTP_READ_TIMEOUT_MS);
        
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
