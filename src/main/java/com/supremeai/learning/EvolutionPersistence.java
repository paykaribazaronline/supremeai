package com.supremeai.learning;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Service
public class EvolutionPersistence {

    private static final String SAVE_FILE = "data/best_agent_config.properties";

    public EvolutionPersistence() {
        try {
            Path path = Paths.get("data");
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            System.err.println("Failed to create data directory: " + e.getMessage());
        }
    }

    public void saveBestConfig(AgentConfig config, int generation) {
        Properties props = new Properties();
        props.setProperty("generation", String.valueOf(generation));
        props.setProperty("decisionWeight", String.valueOf(config.decisionWeight));
        props.setProperty("confidenceThreshold", String.valueOf(config.confidenceThreshold));
        props.setProperty("learningRate", String.valueOf(config.learningRate));

        try (FileOutputStream out = new FileOutputStream(SAVE_FILE)) {
            props.store(out, "Saved automatically by Self-Improvement Engine (Phase 10)");
            System.out.println("Evolution data saved successfully. Generation: " + generation);
        } catch (IOException e) {
            System.err.println("Failed to save evolution data: " + e.getMessage());
        }
    }

    public AgentConfig loadBestConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(SAVE_FILE)) {
            props.load(in);
            
            double dw = Double.parseDouble(props.getProperty("decisionWeight", "0.5"));
            double ct = Double.parseDouble(props.getProperty("confidenceThreshold", "0.5"));
            double lr = Double.parseDouble(props.getProperty("learningRate", "0.1"));
            
            System.out.println("Loaded previous evolution data from generation: " + props.getProperty("generation", "0"));
            return new AgentConfig(dw, ct, lr);
            
        } catch (IOException e) {
            System.out.println("No previous evolution data found. Starting fresh.");
            return null; // Will start fresh
        }
    }
}