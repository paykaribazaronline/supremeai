package com.supremeai.learning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Persistence service for evolution data across generations.
 * Saves and loads agent configuration for self-improvement continuity.
 */
@Service
public class EvolutionPersistence {

    private static final Logger log = LoggerFactory.getLogger(EvolutionPersistence.class);
    private static final String SAVE_FILE = "data/best_agent_config.properties";
    private final String saveFilePath;

    public EvolutionPersistence() {
        // Use /tmp in cloud environments (read-only filesystem), otherwise use relative path
        String cloudMode = System.getenv("SPRING_PROFILES_ACTIVE");
        if (cloudMode != null && cloudMode.contains("cloud")) {
            this.saveFilePath = "/tmp/best_agent_config.properties";
        } else {
            this.saveFilePath = SAVE_FILE;
            try {
                Path path = Paths.get("data");
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }
            } catch (IOException e) {
                log.warn("Failed to create data directory, will use fallback", e);
            }
        }
    }

    /**
     * Save the best agent configuration for the current generation.
     */
    public void saveBestConfig(AgentConfig config, int generation) {
        Properties props = new Properties();
        props.setProperty("generation", String.valueOf(generation));
        props.setProperty("decisionWeight", String.valueOf(config.decisionWeight));
        props.setProperty("confidenceThreshold", String.valueOf(config.confidenceThreshold));
        props.setProperty("learningRate", String.valueOf(config.learningRate));

        try (FileOutputStream out = new FileOutputStream(saveFilePath)) {
            props.store(out, "Saved automatically by Self-Improvement Engine");
            log.info("Evolution data saved successfully to {}. Generation: {}", saveFilePath, generation);
        } catch (IOException e) {
            log.error("Failed to save evolution data to " + saveFilePath, e);
        }
    }

    /**
     * Load the best agent configuration from previous generation.
     * @return AgentConfig or null if no previous data exists
     */
    public AgentConfig loadBestConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(saveFilePath)) {
            props.load(in);

            double dw = Double.parseDouble(props.getProperty("decisionWeight", "0.5"));
            double ct = Double.parseDouble(props.getProperty("confidenceThreshold", "0.5"));
            double lr = Double.parseDouble(props.getProperty("learningRate", "0.1"));

            log.info("Loaded previous evolution data from generation: {}", props.getProperty("generation", "0"));
            return new AgentConfig(dw, ct, lr);

        } catch (IOException e) {
            log.info("No previous evolution data found at {}. Starting fresh.", saveFilePath);
            return null; // Will start fresh
        }
    }
}
