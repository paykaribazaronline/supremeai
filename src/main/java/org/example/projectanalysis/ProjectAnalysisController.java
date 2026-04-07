package org.example.projectanalysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/project-analysis")
@CrossOrigin(origins = "*")
public class ProjectAnalysisController {
    
    @Autowired
    private ProjectAnalyzerService analyzerService;
    
    @Autowired
    private ProjectAnalysisFirebaseService firebaseService;
    
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeProject(@RequestBody Map<String, String> request) {
        String projectPath = request.get("projectPath");
        if (projectPath == null || projectPath.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "projectPath required"));
        }
        
        try {
            ProjectAnalysis analysis = analyzerService.analyzeProject(projectPath);
            
            // Save to Firebase
            firebaseService.saveAnalysis(analysis);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("analysisId", analysis.getId());
            response.put("projectName", analysis.getProjectName());
            response.put("projectType", analysis.getProjectType());
            response.put("overallHealth", analysis.getOverallHealth());
            response.put("healthScore", analysis.getHealthScore());
            response.put("totalFiles", analysis.getTotalFiles());
            response.put("totalLinesOfCode", analysis.getTotalLinesOfCode());
            response.put("issuesCount", analysis.getIssues().size());
            response.put("suggestionsCount", analysis.getSuggestions().size());
            response.put("summary", analysis.getSummary());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @PostMapping("/health")
    public ResponseEntity<Map<String, Object>> quickHealthCheck(@RequestBody Map<String, String> request) {
        String projectPath = request.get("projectPath");
        if (projectPath == null || projectPath.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "projectPath required"));
        }
        
        try {
            ProjectAnalysis analysis = analyzerService.analyzeProject(projectPath);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "projectName", analysis.getProjectName(),
                "healthScore", analysis.getHealthScore(),
                "overallHealth", analysis.getOverallHealth(),
                "issuesCount", analysis.getIssues().size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
