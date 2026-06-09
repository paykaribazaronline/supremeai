package com.supremeai.service.knowledge;

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
public class UnslothTrainingService {
    private static final Logger logger = LoggerFactory.getLogger(UnslothTrainingService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${unsloth.api.url:}")
    private String unslothApiUrl;
    @Value("${unsloth.api.key:}")
    private String unslothApiKey;

    public UnslothTrainingService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(120))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String submitFineTuning(String modelId, String datasetRef, Map<String, Object> hparams) throws IOException {
        if (unslothApiKey == null || unslothApiKey.isBlank()) {
            throw new IllegalStateException("Unsloth API key not configured");
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("model_id", modelId);
        body.put("dataset_ref", datasetRef);
        body.put("hyperparameters", hparams);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(unslothApiUrl + "/v1/fine-tuning/jobs")
                .addHeader("Authorization", "Bearer " + unslothApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unsloth HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "{}";
        }
    }

    public boolean isConfigured() {
        return unslothApiKey != null && !unslothApiKey.isBlank();
    }
}
