package org.example.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class OllamaProvider {

    @Value("${ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    private static final ObjectMapper mapper = new ObjectMapper();

    public String generateCode(String prompt) {
        try {
            URL url = new URL(ollamaUrl + "/api/generate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            var root = mapper.createObjectNode();
            root.put("model", "llama3.2:70b");
            root.put("prompt", prompt);
            root.put("stream", false);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = mapper.writeValueAsBytes(root);
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() != 200) {
                return null;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                JsonNode jsonResponse = mapper.readTree(response.toString());
                return jsonResponse.get("response").asText();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isHealthy() {
        try {
            URL url = new URL(ollamaUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
