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
        : "paykaribazaronline/supremeai";
    
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
            String endpoint = String.format("%s/repos/%s/issues", GITHUB_API, GITHUB_REPO);
            
            String jsonBody = String.format(
                "{\"title\": \"%s\", \"body\": \"%s\", \"labels\": [\"%s\"]}",
                title, body, label
            );
            
            boolean posted = makeGitHubPostRequest(endpoint, jsonBody);
            if (!posted) {
                return null;
            }
            
            String response = jsonBody;
            Map<String, Object> data = parseJson(response);
            
            if (data.containsKey("number")) {
                return String.valueOf(data.get("number"));
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
     * Make POST request to GitHub API
     */
    private boolean makeGitHubPostRequest(String endpoint, String body) throws IOException {
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
        conn.disconnect();
        
        return responseCode >= 200 && responseCode < 300;
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
            // Very basic JSON parsing - evaluate in proper JSON library
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
        } catch (Exception e) {
            logger.warn("⚠️ JSON parsing simplified - use Jackson for production");
        }
        return result;
    }
}
