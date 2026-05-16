package com.supremeai.service.analysis;

import com.supremeai.model.analysis.AnalysisFinding;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

/**
 * Main agent interface for all analysis agents.
 * All agents must implement this contract.
 */
public interface AnalysisAgentInterface {
    String getCategory();
    Flux<AnalysisFinding> scanFile(File file, String relativePath);
    List<String> getSupportedExtensions();
    boolean isEnabled();
}
