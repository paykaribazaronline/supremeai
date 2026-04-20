package com.supremeai.api;

import com.supremeai.provider.AIProviderFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;

/**
 * Streaming Chat Controller - SSE real-time responses
 * Eliminates all waiting spinner frustration
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class StreamingChatController {

    private final AIProviderFactory providerFactory;

    public StreamingChatController(AIProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody Map<String, String> request) {
        String prompt = request.get("message");

        return Flux.create(sink -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URI("http://localhost:11434/api/generate").toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String body = "{\"model\": \"llama3:70b\", \"prompt\": \"" + prompt.replace("\"", "\\\"") + "\", \"stream\": true}";
                conn.getOutputStream().write(body.getBytes());

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;

                while ((line = reader.readLine()) != null && !sink.isCancelled()) {
                    if (line.trim().isEmpty()) continue;
                    sink.next("data: " + line + "\n\n");
                }

                sink.next("data: [DONE]\n\n");
                sink.complete();

            } catch (Exception e) {
                sink.error(e);
            }
        });
    }
}
