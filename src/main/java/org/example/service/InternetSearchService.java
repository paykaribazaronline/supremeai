package org.example.service;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.*;

/**
 * 🌐 Internet Search Service (Tavily / Serper / Google)
 * Enables SupremeAI to fetch real-time documentation and code examples from the web.
 */
public class InternetSearchService {
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private String tavilyApiKey;

    public InternetSearchService(String apiKey) {
        this.tavilyApiKey = apiKey;
    }

    /**
     * Search the web for up-to-date documentation or bug fixes.
     */
    public List<SearchResult> search(String query) {
        if (tavilyApiKey == null || tavilyApiKey.isEmpty()) {
            System.err.println("⚠️ Search API Key missing. Skipping internet search.");
            return Collections.emptyList();
        }

        System.out.println("🌐 Searching the web for: " + query);
        
        // Using Tavily API for search (optimized for AI agents)
        String jsonBody = mapper.createObjectNode()
                .put("api_key", tavilyApiKey)
                .put("query", query)
                .put("search_depth", "advanced")
                .put("include_answer", true)
                .put("max_results", 5)
                .toString();

        Request request = new Request.Builder()
                .url("https://api.tavily.com/search")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode root = mapper.readTree(response.body().string());
                List<SearchResult> results = new ArrayList<>();
                
                if (root.has("results")) {
                    for (JsonNode node : root.get("results")) {
                        results.add(new SearchResult(
                            node.path("title").asText(),
                            node.path("url").asText(),
                            node.path("content").asText()
                        ));
                    }
                }
                return results;
            }
        } catch (IOException e) {
            System.err.println("❌ Internet search failed: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public static class SearchResult {
        public String title;
        public String url;
        public String snippet;

        public SearchResult(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
        }
    }
}
