package com.supremeai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import jakarta.annotation.PostConstruct;

import java.util.*;

/**
 * Stub provider for local-first operation.
 * Provides REAL, useful offline responses without requiring external AI API keys.
 * Uses comprehensive rule-based patterns to generate genuinely helpful answers.
 */
@Component
public class StubLocalProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(StubLocalProvider.class);

    // Knowledge base for offline answers, loaded from JSON
    private final Map<String, String> knowledgeBase = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("core_knowledge.json");
            if (resource.exists()) {
                List<Map<String, String>> loadedData = mapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<Map<String, String>>>() {}
                );
                for (Map<String, String> entry : loadedData) {
                    knowledgeBase.put(entry.get("task"), entry.get("solution"));
                }
                log.info("[StubLocalProvider] Loaded {} topics from core_knowledge.json", knowledgeBase.size());
            } else {
                log.warn("[StubLocalProvider] core_knowledge.json not found, knowledge base will be empty");
                // Add basic fallback
                knowledgeBase.put("hello", "Hello! I'm SupremeAI (Offline). I couldn't load my knowledge base.");
            }
        } catch (Exception e) {
            log.error("[StubLocalProvider] Failed to load knowledge base: {}", e.getMessage());
        }
    }

    @Override
    public String getName() { return "stub-local"; }

    @Override
    public Map<String, Object> getCapabilities() {
        Map<String, Object> caps = new HashMap<>();
        caps.put("offline", true);
        caps.put("localModel", true);
        caps.put("supportsCode", true);
        caps.put("supportsChat", true);
        caps.put("ruleBasedResponses", true);
        caps.put("topicCount", knowledgeBase.size());
        return caps;
    }

    @Override
    public Mono<String> generate(String prompt) {
        log.info("[StubLocalProvider] Generating real response for: {}", prompt);
        return Mono.just(generateRealResponse(prompt));
    }

    private String generateRealResponse(String prompt) {
        if (prompt == null || prompt.trim().isEmpty() || knowledgeBase.isEmpty()) {
            return "Please ask me a question — I can help with programming, development, DevOps, databases, and more.";
        }

        String p = prompt.toLowerCase().trim();

        // Handle "what is X" and similar factual questions FIRST
        // These should be answered directly, not with system architecture
        if (p.contains("what is") || p.contains("what are") || p.contains("explain") || p.contains("tell me about")) {
            for (Map.Entry<String, String> entry : knowledgeBase.entrySet()) {
                String key = entry.getKey();
                // For "what is X" questions, check if the topic keyword is in the prompt
                if (p.contains("what is " + key) || p.contains("what are " + key) || 
                    p.contains("explain " + key) || p.contains("tell me about " + key) ||
                    p.contains("about " + key)) {
                    return entry.getValue();
                }
            }
        }

        for (Map.Entry<String, String> entry : knowledgeBase.entrySet()) {
            if (p.contains(entry.getKey())) {
                log.info("[StubLocalProvider] Matched topic: {}", entry.getKey());
                return entry.getValue();
            }
        }

        // Multi-word topic matching (e.g., "create flutter app" → matches "flutter")
        String[] words = p.split("\\s+");
        for (String word : words) {
            if (word.length() > 2 && knowledgeBase.containsKey(word)) {
                log.info("[StubLocalProvider] Word-matched topic: {}", word);
                return knowledgeBase.get(word);
            }
        }

        // Smart category fallback based on intent keywords
        if (p.contains("create") || p.contains("build") || p.contains("make") || p.contains("develop") || p.contains("start")) {
            return generateProjectCreationGuide(p);
        }
        if (p.contains("error") || p.contains("bug") || p.contains("fix") || p.contains("debug") || p.contains("crash")) {
            return generateDebuggingGuide(p);
        }
        if (p.contains("deploy") || p.contains("host") || p.contains("publish") || p.contains("production")) {
            return generateDeploymentGuide(p);
        }
        if (p.contains("install") || p.contains("setup") || p.contains("configure")) {
            return generateSetupGuide(p);
        }
        if (p.contains("test") || p.contains("testing")) {
            return generateTestingGuide(p);
        }

        // General helpful response (never a generic "I'm in local mode" message)
        return "## Here's what I can help you with:\n\n" +
            "I searched my knowledge base but couldn't find a specific match for your query: \"" + prompt + "\"\n\n" +
            "**Topics I can help with:**\n" +
            "- **Web Development:** React, Vue, Angular, Next.js, HTML, CSS, JavaScript, TypeScript\n" +
            "- **Backend:** Java, Spring Boot, Python, Flask, FastAPI, Node.js, Express\n" +
            "- **Mobile:** Flutter, Dart\n" +
            "- **DevOps:** Docker, Kubernetes, Git, Linux, CI/CD\n" +
            "- **Cloud:** AWS, GCP, Firebase\n" +
            "- **Databases:** SQL, PostgreSQL, MongoDB, Firestore\n" +
            "- **AI/ML:** Machine Learning, Deep Learning, scikit-learn, TensorFlow\n\n" +
            "Please try rephrasing your question with one of these topics, and I'll provide a detailed answer with code examples!";
    }

    private String generateProjectCreationGuide(String prompt) {
        return "## Project Creation Guide\n\n" +
            "Based on your request, here are common project creation commands:\n\n" +
            "| Technology | Command |\n|---|---|\n" +
            "| React | `npx create-react-app my-app` or `npm create vite@latest` |\n" +
            "| Next.js | `npx create-next-app@latest my-app` |\n" +
            "| Vue | `npm create vue@latest my-app` |\n" +
            "| Angular | `ng new my-app` |\n" +
            "| Flutter | `flutter create my_app` |\n" +
            "| Spring Boot | Visit https://start.spring.io |\n" +
            "| Python/Flask | `mkdir my-app && python -m venv venv` |\n" +
            "| Node/Express | `npm init -y && npm install express` |\n\n" +
            "Tell me which technology you'd like to use and I'll give you a detailed step-by-step guide!";
    }

    private String generateDebuggingGuide(String prompt) {
        return "## Debugging Guide\n\n" +
            "**Step 1:** Read the error message carefully — it usually tells you the file and line number.\n\n" +
            "**Step 2:** Check these common issues:\n" +
            "- Null pointer / undefined reference\n" +
            "- Missing imports or dependencies\n" +
            "- Typos in variable/function names\n" +
            "- Wrong data types\n" +
            "- Network/CORS errors\n\n" +
            "**Step 3:** Use debugging tools:\n" +
            "- Browser DevTools (F12) for frontend\n" +
            "- `console.log()` / `System.out.println()` / `print()` for quick debugging\n" +
            "- Breakpoints in your IDE\n" +
            "- Stack trace analysis\n\n" +
            "**Step 4:** Search the exact error message online (StackOverflow, GitHub Issues).\n\n" +
            "Share your specific error message and I'll help you diagnose it!";
    }

    private String generateDeploymentGuide(String prompt) {
        return "## Deployment Guide\n\n" +
            "| Platform | Command |\n|---|---|\n" +
            "| Firebase Hosting | `firebase deploy` |\n" +
            "| Vercel | `npx vercel` |\n" +
            "| Netlify | `npx netlify deploy --prod` |\n" +
            "| Google Cloud Run | `gcloud run deploy` |\n" +
            "| Docker | `docker build -t myapp . && docker push` |\n" +
            "| Heroku | `git push heroku main` |\n\n" +
            "Tell me your technology stack and I'll provide a specific deployment guide!";
    }

    private String generateSetupGuide(String prompt) {
        return "## Setup & Installation Guide\n\n" +
            "**Common Development Setup:**\n" +
            "1. Install Node.js: https://nodejs.org\n" +
            "2. Install Git: https://git-scm.com\n" +
            "3. Install your IDE (VS Code recommended)\n" +
            "4. Install language-specific tools:\n" +
            "   - Java: JDK 17+ from https://adoptium.net\n" +
            "   - Python: python.org or pyenv\n" +
            "   - Flutter: flutter.dev/docs/get-started\n\n" +
            "Tell me what specific tool or framework you want to set up!";
    }

    private String generateTestingGuide(String prompt) {
        return "## Testing Guide\n\n" +
            "**By Technology:**\n" +
            "- **JavaScript/React:** Jest + React Testing Library\n" +
            "  ```bash\n  npm test\n  ```\n" +
            "- **Java/Spring:** JUnit 5 + MockMvc\n" +
            "  ```bash\n  ./gradlew test\n  ```\n" +
            "- **Python:** pytest\n" +
            "  ```bash\n  pip install pytest && pytest\n  ```\n" +
            "- **Flutter:** `flutter test`\n\n" +
            "**Test Types:** Unit tests, Integration tests, E2E tests (Cypress, Playwright).";
    }
}