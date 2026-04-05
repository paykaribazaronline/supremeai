package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final String GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");
    private static final String GITHUB_REPO = System.getenv("GITHUB_REPO") != null 
        ? System.getenv("GITHUB_REPO")
        : "supremeai/supremeai";
    
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
            if (GITHUB_TOKEN == null || GITHUB_TOKEN.trim().isEmpty()) {
                logger.error("❌ GitHub token not configured. Set GITHUB_TOKEN environment variable.");
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
    
    // ============ PRIVATE HELPER METHODS ============
    
    /**
     * Make GET request to GitHub API
     */
    private String makeGitHubRequest(String endpoint) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "token " + GITHUB_TOKEN);
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
        if (GITHUB_TOKEN == null || GITHUB_TOKEN.trim().isEmpty()) {
            throw new IOException("GitHub token not configured");
        }
        
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "token " + GITHUB_TOKEN);
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
}
