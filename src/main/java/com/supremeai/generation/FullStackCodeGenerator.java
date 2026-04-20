package com.supremeai.generation;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FullStackCodeGenerator {

    public Map<String, String> generateApp(String requirements, String platform) {
        Map<String, String> output = new HashMap<>();
        
        output.put("status", "success");
        output.put("platform", platform);
        output.put("backend", generateSpringBootBackend(requirements));
        output.put("frontend", generateReactFrontend(requirements));
        
        return output;
    }
    
    private String generateSpringBootBackend(String requirements) {
        return """
            // Generated Spring Boot Backend
            package com.generated.app;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            
            @SpringBootApplication
            public class Application {
                public static void main(String[] args) {
                    SpringApplication.run(Application.class, args);
                }
            }
            """;
    }
    
    private String generateReactFrontend(String requirements) {
        return """
            // Generated React Frontend
            import React from 'react';
            import ReactDOM from 'react-dom/client';
            
            function App() {
              return (
                <div className="App">
                  <h1>Generated Application</h1>
                  <p>Built by SupremeAI</p>
                </div>
              );
            }
            
            const root = ReactDOM.createRoot(document.getElementById('root'));
            root.render(<App />);
            """;
    }
}
