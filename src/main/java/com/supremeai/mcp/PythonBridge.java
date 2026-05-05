package com.supremeai.mcp;

import org.springframework.stereotype.Component;
import java.io.*;
import java.util.*;

/**
 * Python Bridge - Plan 23 + 24 Integration
 * Calls Python reverse_engineer from Java MCP server
 */
@Component
public class PythonBridge {
    
    /**
     * Call Python reverse_engineer script
     */
    public Map<String, Object> callReverseEngineer(String url, Map<String, Object> credentials) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Build command: python3 main.py <url> [--creds email:pass]
            List<String> command = new ArrayList<>();
            command.add("python3");
            command.add("/home/nazifarabbu/OneDrive/supremeai/reverse_engineer/main.py");
            command.add(url);
            
            if (credentials != null && !credentials.isEmpty()) {
                command.add("--creds");
                String email = (String) credentials.get("email");
                String password = (String) credentials.get("password");
                if (email != null && password != null) {
                    command.add(email + ":" + password);
                }
            }
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File("/home/nazifarabbu/OneDrive/supremeai/reverse_engineer"));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Read output
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                result.put("status", "success");
                result.put("output", output.toString());
                
                // Try to read generated connector file
                String connectorFile = url.replace("https://", "").replace("http://", "").replace("/", "_") + "_connector.py";
                File file = new File("/home/nazifarabbu/OneDrive/supremeai/reverse_engineer/" + connectorFile);
                if (file.exists()) {
                    result.put("connector_file", connectorFile);
                }
            } else {
                result.put("status", "error");
                result.put("exit_code", exitCode);
                result.put("output", output.toString());
            }
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }
}
