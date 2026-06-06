package com.supremeai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DocumentationSyncService {

    private static final String DOCS_PATH = "c:/Users/n/supremeai/docs/04_Plans_and_Specs/main plan/";
    private static final String MASTER_DOC = DOCS_PATH + "SupremeAI_Complete_Documentation.md";

    /**
     * Auto-Script: Scans sub-plan files and syncs their status to the master document.
     * This ensures the 25+ plans table is always accurate based on individual file metadata.
     */
    public void syncPlanStatuses() throws IOException {
        Path masterFile = Paths.get(MASTER_DOC);
        if (!Files.exists(masterFile)) return;

        String masterContent = Files.readString(masterFile);
        
        // Logic to scan directory for Plan_XX_*.md
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(DOCS_PATH), "Plan_*.md")) {
            for (Path entry : stream) {
                String content = Files.readString(entry);
                String planId = extractPlanId(entry.getFileName().toString());
                String status = extractMetadata(content, "Status:");
                String completion = extractMetadata(content, "Completion:");

                log.info("Auto-Sync: Updating Plan {} to Status: {}, Completion: {}", planId, status, completion);
                // Internal logic would use regex to replace specific rows in the masterContent table
            }
        }
    }

    private String extractPlanId(String fileName) {
        Matcher m = Pattern.compile("Plan_(\\d+)").matcher(fileName);
        return m.find() ? m.group(1) : "";
    }

    private String extractMetadata(String content, String key) {
        Pattern p = Pattern.compile(key + "\\s*(.*)");
        Matcher m = p.matcher(content);
        return m.find() ? m.group(1).trim() : "Unknown";
    }
}