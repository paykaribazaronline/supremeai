package com.supremeai.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.security.UnifiedSecretsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VertexAIEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(VertexAIEmbeddingService.class);

    private static final int MAX_BATCH_SIZE = 250;
    private static final int EMBEDDING_DIMENSION = 768;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String projectId;
    private final String location;
    private final UnifiedSecretsService secretsService;

    public VertexAIEmbeddingService(
            UnifiedSecretsService secretsService,
            @Value("${gcp.project.id:}") String projectId,
            @Value("${gcp.location:us-central1}") String location) {
        this.secretsService = secretsService;
        this.projectId = projectId;
        this.location = location;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Double> generateEmbedding(String text) {
        List<List<Double>> batch = generateBatchEmbeddings(List.of(text));
        return batch.isEmpty() ? new ArrayList<>() : batch.get(0);
    }

    @Override
    public List<List<Double>> generateBatchEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        List<List<Double>> allEmbeddings = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, end);

            try {
                List<List<Double>> batchResult = callVertexAIApi(batch);
                allEmbeddings.addAll(batchResult);
            } catch (Exception e) {
                log.error("Error generating embeddings for batch {}-{}: {}", i, end, e.getMessage());
                for (int j = 0; j < batch.size(); j++) {
                    allEmbeddings.add(defaultEmbedding());
                }
            }
        }

        return allEmbeddings;
    }

    private List<List<Double>> callVertexAIApi(List<String> texts) throws IOException {
        String url = String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/textembedding-gecko@003:predict",
                location, projectId, location);

        StringBuilder instancesJson = new StringBuilder("[");
        for (int i = 0; i < texts.size(); i++) {
            if (i > 0) instancesJson.append(",");
            instancesJson.append(String.format("{\"content\":\"%s\"}",
                    escapeJson(texts.get(i))));
        }
        instancesJson.append("]");

        String jsonBody = String.format("{\"instances\":%s}", instancesJson);

        String accessToken = getAccessToken();
        RequestBody body = RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body().string());
                JsonNode predictions = root.path("predictions");

                List<List<Double>> embeddings = new ArrayList<>();
                if (predictions.isArray()) {
                    for (JsonNode prediction : predictions) {
                        JsonNode values = prediction.path("embeddings").path("values");
                        if (values.isArray()) {
                            List<Double> embedding = new ArrayList<>();
                            for (JsonNode v : values) {
                                embedding.add(v.asDouble());
                            }
                            embeddings.add(embedding);
                        } else {
                            embeddings.add(defaultEmbedding());
                        }
                    }
                }
                return embeddings;
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No body";
                log.error("Vertex AI embedding API error: {} - {}", response.code(), errorBody);
                return texts.stream().map(t -> defaultEmbedding()).collect(Collectors.toList());
            }
        }
    }

    private String getAccessToken() {
        try {
            String key = secretsService.getSecret("GCP_ACCESS_TOKEN").block();
            return key != null ? key : System.getenv("GCP_ACCESS_TOKEN");
        } catch (Exception e) {
            return System.getenv("GCP_ACCESS_TOKEN");
        }
    }

    private List<Double> defaultEmbedding() {
        List<Double> embedding = new ArrayList<>();
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding.add(0.0);
        }
        return embedding;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
