package com.supremeai.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * CD-05: One-Click Deploy Simulates deploying a generated project to Cloud Run / Firebase Hosting
 * directly from the admin dashboard.
 */
@Service
public class OneClickDeployService {

  private static final Logger log = LoggerFactory.getLogger(OneClickDeployService.class);

  public Mono<Map<String, Object>> deployProject(String projectId, String targetEnvironment) {
    log.info("Initiating One-Click Deploy for Project {} to {}", projectId, targetEnvironment);

    // Simulated deployment process
    return Mono.delay(Duration.ofSeconds(5)) // Simulate build and deploy time
        .map(
            v -> {
              Map<String, Object> result = new HashMap<>();
              result.put("projectId", projectId);
              result.put("targetEnvironment", targetEnvironment);
              result.put("status", "SUCCESS");

              if ("CloudRun".equalsIgnoreCase(targetEnvironment)) {
                result.put("url", "https://" + projectId + "-deploy.run.app");
                result.put("serviceId", "cr-" + UUID.randomUUID().toString().substring(0, 8));
              } else if ("FirebaseHosting".equalsIgnoreCase(targetEnvironment)) {
                result.put("url", "https://" + projectId + ".web.app");
                result.put("siteId", "fh-" + UUID.randomUUID().toString().substring(0, 8));
              } else {
                result.put("url", "http://localhost:8080");
              }

              result.put("deployedAt", java.time.LocalDateTime.now().toString());
              log.info("Project {} successfully deployed to: {}", projectId, result.get("url"));
              return result;
            });
  }
}
