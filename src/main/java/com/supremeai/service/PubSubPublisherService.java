package com.supremeai.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Google Cloud Pub/Sub publisher using low-level client.
 * Publishes reverse engineering jobs to a Pub/Sub topic.
 */
@Service
public class PubSubPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(PubSubPublisherService.class);

    @Value("${spring.cloud.gcp.project-id:supremeai-a}")
    private String projectId;

    private final Map<String, Publisher> publisherCache = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public void publish(String topicName, Map<String, Object> payload) {
        String topicPath = ProjectTopicName.format(projectId, topicName).toString();
        Publisher publisher = publisherCache.computeIfAbsent(topicPath, this::createPublisher);
        
        try {
            String json = gson.toJson(payload);
            PubsubMessage message = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(json))
                .putAttributes("contentType", "application/json")
                .build();
            
            ApiFuture<String> future = publisher.publish(message);
            String messageId = future.get(5, TimeUnit.SECONDS);
            logger.info("Published message {} to topic {}: jobId={}", messageId, topicName, payload.get("jobId"));
        } catch (Exception e) {
            logger.error("Failed to publish to Pub/Sub topic {}: {}", topicName, e.getMessage(), e);
            throw new RuntimeException("Pub/Sub publish failed", e);
        }
    }

    private Publisher createPublisher(String topicPath) {
        try {
            Publisher.Builder builder = Publisher.newBuilder(ProjectTopicName.parse(topicPath));
            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Pub/Sub publisher for " + topicPath, e);
        }
    }

    /**
     * Graceful shutdown hook to close publishers.
     */
    public void shutdown() {
        publisherCache.forEach((topic, publisher) -> {
            try {
                publisher.shutdown();
                publisher.awaitTermination(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("Error shutting down publisher for {}: {}", topic, e.getMessage());
            }
        });
    }
}
