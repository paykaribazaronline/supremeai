package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LocalJsonStoreService {
    private static final Logger logger = LoggerFactory.getLogger(LocalJsonStoreService.class);
    private final ObjectMapper mapper;
    private final Path baseDirectory = Paths.get("data", "supremeai");

    public LocalJsonStoreService() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    void ensureBaseDirectory() throws IOException {
        Files.createDirectories(baseDirectory);
    }

    public synchronized <T> T read(String relativePath, TypeReference<T> type, T fallbackValue) {
        Path target = baseDirectory.resolve(relativePath);
        if (!Files.exists(target)) {
            return fallbackValue;
        }
        try {
            return mapper.readValue(target.toFile(), type);
        } catch (IOException exception) {
            return fallbackValue;
        }
    }

    public synchronized void write(String relativePath, Object value) {
        Path target = baseDirectory.resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), value);
        } catch (IOException exception) {
            // Log instead of throwing — a disk I/O failure should not crash the caller
            // (e.g. QuotaService.persistQuotas or ProviderRegistryService).
            logger.error("❌ Failed to write local store: {} — {}", target, exception.getMessage());
        }
    }
}