package com.supremeai.service.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Service
public class ManifestService {
    private static final Logger logger = LoggerFactory.getLogger(ManifestService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${manifest.api.url:}")
    private String manifestUrl;

    public ManifestService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String migrateSchema(String manifestContent) throws IOException {
        if (manifestUrl == null || manifestUrl.isBlank()) {
            return "skipped-unconfigured";
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("manifest", manifestContent);
        body.put("action", "migrate");
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(manifestUrl + "/v1/migrate")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Manifest HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "ok";
        }
    }

    public boolean isConfigured() {
        return manifestUrl != null && !manifestUrl.isBlank();
    }
}
