package org.example.service;

import org.example.service.RequirementAnalyzer.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Code Generator
 * Creates Java code (Services, Controllers, Models) from requirements
 */
@Service
public class CodeGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CodeGenerator.class);
    
    /**
     * Generate Java Service from requirement
     */
    public String generateService(Requirement req) {
        StringBuilder code = new StringBuilder();
        
        code.append("package org.example.service;\n\n");
        code.append("import org.slf4j.Logger;\n");
        code.append("import org.slf4j.LoggerFactory;\n");
        code.append("import org.springframework.stereotype.Service;\n");
        code.append("import java.util.*;\n\n");
        
        // Class
        code.append("/**\n");
        code.append(" * ").append(req.name).append("Service\n");
        code.append(" * Generated: ").append(LocalDateTime.now()).append("\n");
        code.append(" */\n");
        code.append("@Service\n");
        code.append("public class ").append(req.name).append("Service {\n");
        code.append("    private static final Logger logger = LoggerFactory.getLogger(").append(req.name).append("Service.class);\n\n");
        
        // Methods
        for (Map.Entry<String, String> method : req.methods.entrySet()) {
            code.append("    /**\n");
            code.append("     * ").append(method.getKey()).append("\n");
            code.append("     */\n");
            code.append("    public ").append(method.getValue()).append(" {\n");
            code.append("        try {\n");
            code.append("            logger.info(\"🔧 Executing ").append(method.getKey()).append("\");\n");
            code.append("            // Implementation here\n");
            code.append("            return true;\n");
            code.append("        } catch (Exception e) {\n");
            code.append("            logger.error(\"❌ Failed: {}\", e.getMessage());\n");
            code.append("            return false;\n");
            code.append("        }\n");
            code.append("    }\n\n");
        }
        
        code.append("}\n");
        
        logger.info("✅ Generated Service: {}", req.name);
        return code.toString();
    }
    
    /**
     * Generate REST Controller from requirement
     */
    public String generateController(Requirement req) {
        StringBuilder code = new StringBuilder();
        
        code.append("package org.example.controller;\n\n");
        code.append("import org.slf4j.Logger;\n");
        code.append("import org.slf4j.LoggerFactory;\n");
        code.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        code.append("import org.springframework.http.ResponseEntity;\n");
        code.append("import org.springframework.web.bind.annotation.*;\n");
        code.append("import java.util.*;\n\n");
        
        String serviceName = req.name + "Service";
        String serviceVar = req.name.substring(0, 1).toLowerCase() + req.name.substring(1) + "Service";
        
        code.append("@RestController\n");
        code.append("@RequestMapping(\"/api/").append(serviceVar).append("\")\n");
        code.append("public class ").append(req.name).append("Controller {\n");
        code.append("    private static final Logger logger = LoggerFactory.getLogger(").append(req.name).append("Controller.class);\n\n");
        code.append("    @Autowired\n");
        code.append("    private ").append(serviceName).append(" ").append(serviceVar).append(";\n\n");
        
        // Generate endpoints for each method
        int count = 0;
        for (String methodName : req.methods.keySet()) {
            String endpoint = methodName.replaceAll("([A-Z])", "-$1").toLowerCase();
            
            code.append("    @PostMapping(\"").append(endpoint).append("\")\n");
            code.append("    public ResponseEntity<?> ").append(methodName).append("() {\n");
            code.append("        try {\n");
            code.append("            ").append(serviceVar).append(".").append(methodName).append("();\n");
            code.append("            return ResponseEntity.ok(Map.of(\"status\", \"success\"));\n");
            code.append("        } catch (Exception e) {\n");
            code.append("            logger.error(\"❌ Error: {}\", e.getMessage());\n");
            code.append("            return ResponseEntity.status(500)\n");
            code.append("                .body(Map.of(\"status\", \"error\", \"message\", e.getMessage()));\n");
            code.append("        }\n");
            code.append("    }\n\n");
            
            count++;
        }
        
        code.append("}\n");
        
        logger.info("✅ Generated Controller: {} with {} endpoints", req.name, count);
        return code.toString();
    }
    
    /**
     * Generate Model from requirement
     */
    public String generateModel(Requirement req) {
        StringBuilder code = new StringBuilder();
        
        code.append("package org.example.model;\n\n");
        code.append("import java.io.Serializable;\n");
        code.append("import java.util.*;\n\n");
        
        code.append("/**\n");
        code.append(" * ").append(req.name).append(" Model\n");
        code.append(" * Generated: ").append(LocalDateTime.now()).append("\n");
        code.append(" */\n");
        code.append("public class ").append(req.name).append(" implements Serializable {\n");
        code.append("    private String id;\n");
        code.append("    private String name;\n");
        code.append("    private Map<String, Object> data = new HashMap<>();\n");
        code.append("    private long timestamp = System.currentTimeMillis();\n\n");
        
        // Getters
        code.append("    public String getId() { return id; }\n");
        code.append("    public void setId(String id) { this.id = id; }\n\n");
        
        code.append("    public String getName() { return name; }\n");
        code.append("    public void setName(String name) { this.name = name; }\n\n");
        
        code.append("    public Map<String, Object> getData() { return data; }\n");
        code.append("    public void setData(Map<String, Object> data) { this.data = data; }\n\n");
        
        code.append("    public long getTimestamp() { return timestamp; }\n\n");
        
        code.append("}\n");
        
        logger.info("✅ Generated Model: {}", req.name);
        return code.toString();
    }
    
    /**
     * Generate all code for a requirement
     */
    public Map<String, String> generateComplete(Requirement req) {
        Map<String, String> generated = new HashMap<>();
        
        generated.put("Model", generateModel(req));
        generated.put("Service", generateService(req));
        
        if ("CONTROLLER".equals(req.type) || "SERVICE".equals(req.type)) {
            generated.put("Controller", generateController(req));
        }
        
        logger.info("✅ Complete code generation for: {} - {} files", req.name, generated.size());
        return generated;
    }
}
