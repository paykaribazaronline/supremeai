package org.example.service;

import java.io.*;
import java.util.*;

/**
 * 🕵️ Self Git Analyzer
 * Allows SupremeAI to analyze its own development history, 
 * identify patterns, and learn from its own past commits.
 */
public class SelfGitAnalyzer {
    
    private final String repoRoot;
    
    public SelfGitAnalyzer(String repoRoot) {
        this.repoRoot = repoRoot;
    }
    
    /**
     * Analyze recent commit history to understand evolution.
     */
    public List<Map<String, String>> analyzeSelfHistory(int limit) {
        List<Map<String, String>> history = new ArrayList<>();
        try {
            System.out.println("🕵️ Analyzing my own Git history...");
            
            // git log --pretty=format:"%h|%an|%ar|%s" -n 10
            ProcessBuilder pb = new ProcessBuilder("git", "log", "--pretty=format:%h|%an|%ar|%s", "-n", String.valueOf(limit));
            pb.directory(new File(repoRoot));
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    Map<String, String> commit = new HashMap<>();
                    commit.put("hash", parts[0]);
                    commit.put("author", parts[1]);
                    commit.put("time", parts[2]);
                    commit.put("message", parts[3]);
                    history.add(commit);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("❌ Failed to analyze self history");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Self-analysis error: " + e.getMessage());
        }
        return history;
    }
    
    /**
     * Check current branch health and uncommitted changes.
     */
    public Map<String, String> checkSelfStatus() {
        Map<String, String> statusMap = new HashMap<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "status", "-s");
            pb.directory(new File(repoRoot));
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            
            statusMap.put("uncommitted_changes", sb.toString());
            statusMap.put("is_dirty", String.valueOf(!sb.toString().isEmpty()));
            
            // Get current branch
            ProcessBuilder pbBranch = new ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD");
            pbBranch.directory(new File(repoRoot));
            try (BufferedReader brBranch = new BufferedReader(new InputStreamReader(pbBranch.start().getInputStream()))) {
                String branch = brBranch.readLine();
                statusMap.put("branch", branch != null ? branch : "unknown");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Status check error: " + e.getMessage());
        }
        return statusMap;
    }

    /**
     * Identify "Feature Hotspots" (files changed most frequently).
     */
    public Map<String, Integer> identifyHotspots() {
        Map<String, Integer> hotspots = new HashMap<>();
        try {
            // git log --pretty=format: --name-only | sort | uniq -c | sort -rg
            ProcessBuilder pb = new ProcessBuilder("git", "log", "--pretty=format:", "--name-only");
            pb.directory(new File(repoRoot));
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                hotspots.put(line, hotspots.getOrDefault(line, 0) + 1);
            }
        } catch (Exception e) {
            System.err.println("❌ Hotspot analysis error: " + e.getMessage());
        }
        return hotspots;
    }
}
