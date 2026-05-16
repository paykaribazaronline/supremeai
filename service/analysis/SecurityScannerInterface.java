package com.supremeai.service.analysis;

import com.supremeai.model.analysis.AnalysisFinding;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

/**
 * Interface for analysis agents.
 */
public interface SecurityScannerInterface {

    /**
     * Get the agent category/name.
     */
    String getCategory();

    /**
     * Scan a file and return findings.
     */
    Flux<AnalysisFinding> scanFile(File file, String relativePath);

    /**
     * Get file extensions this agent can process.
     */
    List<String> getSupportedExtensions();

    /**
     * Check if this agent is enabled.
     */
    boolean isEnabled();
}
