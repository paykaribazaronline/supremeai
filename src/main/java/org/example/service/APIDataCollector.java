package org.example.service;

import java.util.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * API Data Collector - Primary data source
 * 
 * Uses FREE APIs:
 * - GitHub API (5000 req/hour) - repo info, commits, branches
 * - Vercel API (100 req/day) - deployment status
 * - Firebase API (50K reads/day) - project data
 */
public class APIDataCollector {
    
    private final QuotaTracker quotaTracker;
    private final FirebaseService firebaseService;
    
    // API Keys (from environment)
    private final String githubToken;
    private final String vercelToken;
    private final String firebaseToken;
    
    public APIDataCollector(QuotaTracker quotaTracker, FirebaseService firebase) {
        this.quotaTracker = quotaTracker;
        this.firebaseService = firebase;
        
        // Load from environment
        this.githubToken = System.getenv("GITHUB_TOKEN") != null ?
            System.getenv("GITHUB_TOKEN") : "YOUR_GITHUB_TOKEN";
        this.vercelToken = System.getenv("VERCEL_TOKEN") != null ?
            System.getenv("VERCEL_TOKEN") : "YOUR_VERCEL_TOKEN";
        this.firebaseToken = System.getenv("FIREBASE_TOKEN") != null ?
            System.getenv("FIREBASE_TOKEN") : "YOUR_FIREBASE_TOKEN";
    }
    
    // ========== GITHUB API ==========
    
    /**
     * Get repository info from GitHub
     * Cost: 1-2 API calls
     */
    public GitHubRepoData getGitHubRepoInfo(String owner, String repo) throws Exception {
        if (!quotaTracker.canUseService("github")) {
            throw new Exception("GitHub API quota exceeded");
        }
        
        System.out.println("🔍 Fetching GitHub repo: " + owner + "/" + repo);
        
        GitHubRepoData data = new GitHubRepoData();
        
        try {
            // Get repo info
            String repoUrl = "https://api.github.com/repos/" + owner + "/" + repo;
            String repoJson = makeGitHubRequest(repoUrl);
            
            // Parse JSON (simple parsing, can use Jackson in real code)
            // For now: Extract key fields manually
            data.name = repo;
            data.owner = owner;
            data.stars = extractJsonInt(repoJson, "\"stargazers_count\"");
            data.forks = extractJsonInt(repoJson, "\"forks_count\"");
            data.watchers = extractJsonInt(repoJson, "\"watchers_count\"");
            data.openIssues = extractJsonInt(repoJson, "\"open_issues_count\"");
            data.description = extractJsonString(repoJson, "\"description\"");
            data.language = extractJsonString(repoJson, "\"language\"");
            data.isPrivate = repoJson.contains("\"private\":true");
            
            // Get latest commit info
            String commitsUrl = repoUrl + "/commits";
            String commitsJson = makeGitHubRequest(commitsUrl);
            data.latestCommit = extractJsonString(commitsJson, "\"message\"");
            data.lastPushed = extractJsonString(commitsJson, "\"committed_at\"");
            
            quotaTracker.recordUsage("github", 2);
            
            System.out.println("✅ GitHub data collected: " + data.stars + " ⭐");
            return data;
            
        } catch (Exception e) {
            System.err.println("❌ GitHub API error: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Make GitHub API request
     */
    private String makeGitHubRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "token " + githubToken);
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new Exception("GitHub API error: " + status);
        }
        
        // Read response
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
        );
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return response.toString();
    }
    
    // ========== VERCEL API ==========
    
    /**
     * Get deployment status from Vercel
     * Cost: 1 API call
     */
    public VercelDeploymentData getVercelDeploymentStatus(String projectId) throws Exception {
        if (!quotaTracker.canUseService("vercel")) {
            throw new Exception("Vercel API quota exceeded");
        }
        
        System.out.println("🔍 Fetching Vercel deployment: " + projectId);
        
        VercelDeploymentData data = new VercelDeploymentData();
        
        try {
            String url = "https://api.vercel.com/v6/projects/" + projectId + "/deployments";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + vercelToken);
            
            // Read response and parse
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Extract data (simplified)
            data.projectId = projectId;
            data.status = extractJsonString(response.toString(), "\"state\"");
            data.url = extractJsonString(response.toString(), "\"url\"");
            data.createdAt = extractJsonString(response.toString(), "\"createdAt\"");
            
            quotaTracker.recordUsage("vercel", 1);
            
            System.out.println("✅ Vercel data collected: status=" + data.status);
            return data;
            
        } catch (Exception e) {
            System.err.println("❌ Vercel API error: " + e.getMessage());
            throw e;
        }
    }
    
    // ========== FIREBASE API ==========
    
    /**
     * Get project data from Firebase
     * Cost: 1-2 API calls
     */
    public FirebaseProjectData getFirebaseProjectStatus() throws Exception {
        if (!quotaTracker.canUseService("firebase")) {
            throw new Exception("Firebase API quota exceeded");
        }
        
        System.out.println("🔍 Fetching Firebase project status");
        
        FirebaseProjectData data = new FirebaseProjectData();
        
        try {
            // TODO: Implement Firebase REST API calls
            // For now: Mock data
            data.projectName = "SupremeAI";
            data.region = "asia-southeast1";
            data.hasHosting = true;
            data.hasFirestore = true;
            data.hasAuthentication = true;
            data.hasFunctions = true;
            
            quotaTracker.recordUsage("firebase", 1);
            
            System.out.println("✅ Firebase data collected");
            return data;
            
        } catch (Exception e) {
            System.err.println("❌ Firebase API error: " + e.getMessage());
            throw e;
        }
    }
    
    // ========== HELPER METHODS ==========
    
    private int extractJsonInt(String json, String key) {
        try {
            int index = json.indexOf(key);
            if (index == -1) return 0;
            int colonIndex = json.indexOf(":", index);
            int commaIndex = json.indexOf(",", colonIndex);
            if (commaIndex == -1) commaIndex = json.indexOf("}", colonIndex);
            
            String value = json.substring(colonIndex + 1, commaIndex).trim();
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private String extractJsonString(String json, String key) {
        try {
            int index = json.indexOf(key);
            if (index == -1) return "";
            int colonIndex = json.indexOf(":", index);
            int quoteStart = json.indexOf("\"", colonIndex);
            int quoteEnd = json.indexOf("\"", quoteStart + 1);
            
            return json.substring(quoteStart + 1, quoteEnd);
        } catch (Exception e) {
            return "";
        }
    }
    
    // ========== DATA CLASSES ==========
    
    public static class GitHubRepoData {
        public String name;
        public String owner;
        public String description;
        public String language;
        public int stars;
        public int forks;
        public int watchers;
        public int openIssues;
        public boolean isPrivate;
        public String latestCommit;
        public String lastPushed;
        
        @Override
        public String toString() {
            return String.format("GitHub(%s): %d⭐ %d🍴 %d👀", name, stars, forks, watchers);
        }
    }
    
    public static class VercelDeploymentData {
        public String projectId;
        public String status;
        public String url;
        public String createdAt;
        
        @Override
        public String toString() {
            return String.format("Vercel(%s): %s", projectId, status);
        }
    }
    
    public static class FirebaseProjectData {
        public String projectName;
        public String region;
        public boolean hasHosting;
        public boolean hasFirestore;
        public boolean hasAuthentication;
        public boolean hasFunctions;
        
        @Override
        public String toString() {
            return String.format("Firebase(%s): Hosting=%s Firestore=%s", 
                projectName, hasHosting, hasFirestore);
        }
    }
}
