package com.supremeai.service.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Service
public class LangChainOrchestrationService {
    private static final Logger logger = LoggerFactory.getLogger(LangChainOrchestrationService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${langchain.api.url:}")
    private String langchainApiUrl;
    @Value("${langchain.api.key:}")
    private String langchainApiKey;

    public LangChainOrchestrationService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String runChain(String chainName, String input) throws IOException {
        if (langchainApiKey == null || langchainApiKey.isBlank()) {
            throw new IllegalStateException("LangChain API key not configured");
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("chain_name", chainName);
        body.put("input", input);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(langchainApiUrl + "/v1/chain/run")
                .addHeader("Authorization", "Bearer " + langchainApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("LangChain HTTP " + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "{}";
            return respBody;
        }
    }

    public boolean isConfigured() {
        return langchainApiKey != null && !langchainApiKey.isBlank();
    }
}
