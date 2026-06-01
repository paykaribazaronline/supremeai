package com.supremeai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Stub provider for local-first operation.
 * Provides REAL, useful offline responses without requiring external AI API keys.
 * Uses comprehensive rule-based patterns to generate genuinely helpful answers.
 */
@Component
public class StubLocalProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(StubLocalProvider.class);

    // Comprehensive knowledge base for offline answers
    private static final Map<String, String> KNOWLEDGE_BASE = new LinkedHashMap<>();

    static {
        // Flutter
        KNOWLEDGE_BASE.put("flutter", 
            "## Flutter App Development Guide\n\n" +
            "**Step 1: Install Flutter SDK**\n" +
            "```bash\ngit clone https://github.com/flutter/flutter.git\nexport PATH=\"$PATH:`pwd`/flutter/bin\"\nflutter doctor\n```\n\n" +
            "**Step 2: Create a New Project**\n" +
            "```bash\nflutter create my_app\ncd my_app\nflutter run\n```\n\n" +
            "**Step 3: Project Structure**\n" +
            "- `lib/main.dart` — Entry point\n" +
            "- `pubspec.yaml` — Dependencies\n" +
            "- `android/` & `ios/` — Platform-specific code\n\n" +
            "**Step 4: Basic Widget Example**\n" +
            "```dart\nimport 'package:flutter/material.dart';\n\nvoid main() => runApp(MaterialApp(\n  home: Scaffold(\n    appBar: AppBar(title: Text('My App')),\n    body: Center(child: Text('Hello Flutter!')),\n  ),\n));\n```\n\n" +
            "**Step 5: Add Dependencies** in `pubspec.yaml`:\n" +
            "```yaml\ndependencies:\n  http: ^1.1.0\n  provider: ^6.0.0\n```\n\n" +
            "**Step 6: Build for Production**\n" +
            "```bash\nflutter build apk     # Android\nflutter build ios     # iOS\nflutter build web     # Web\n```");

        // React
        KNOWLEDGE_BASE.put("react",
            "## React App Development Guide\n\n" +
            "**Create Project:**\n```bash\nnpx create-react-app my-app\n# or with Vite (recommended):\nnpm create vite@latest my-app -- --template react\ncd my-app && npm run dev\n```\n\n" +
            "**Functional Component Example:**\n```jsx\nimport { useState, useEffect } from 'react';\n\nexport default function App() {\n  const [data, setData] = useState([]);\n  \n  useEffect(() => {\n    fetch('/api/data').then(r => r.json()).then(setData);\n  }, []);\n  \n  return (\n    <div>\n      <h1>My React App</h1>\n      {data.map(item => <p key={item.id}>{item.name}</p>)}\n    </div>\n  );\n}\n```\n\n" +
            "**Key Concepts:** useState, useEffect, useContext, React Router, Redux/Zustand for state management.");

        // JavaScript
        KNOWLEDGE_BASE.put("javascript",
            "## JavaScript Guide\n\n" +
            "**Modern ES6+ Features:**\n" +
            "- Arrow functions: `const fn = (x) => x * 2;`\n" +
            "- Destructuring: `const {name, age} = person;`\n" +
            "- Template literals: `` `Hello ${name}` ``\n" +
            "- Async/Await: `const data = await fetch(url);`\n" +
            "- Spread operator: `const arr2 = [...arr1, 4, 5];`\n" +
            "- Optional chaining: `user?.address?.city`\n\n" +
            "**Promises:**\n```js\nfetch('https://api.example.com/data')\n  .then(res => res.json())\n  .then(data => console.log(data))\n  .catch(err => console.error(err));\n```");

        // Java / Spring Boot
        KNOWLEDGE_BASE.put("spring boot",
            "## Spring Boot Application Guide\n\n" +
            "**Create Project:** Visit https://start.spring.io or:\n```bash\ncurl https://start.spring.io/starter.zip -d dependencies=web,data-jpa,postgresql -o demo.zip\nunzip demo.zip && cd demo\n./mvnw spring-boot:run\n```\n\n" +
            "**REST Controller:**\n```java\n@RestController\n@RequestMapping(\"/api\")\npublic class UserController {\n    @GetMapping(\"/users\")\n    public List<User> getAll() { return userService.findAll(); }\n    \n    @PostMapping(\"/users\")\n    public User create(@Valid @RequestBody User user) {\n        return userService.save(user);\n    }\n}\n```\n\n" +
            "**Key Dependencies:** Spring Web, Spring Data JPA, Spring Security, Lombok.");

        KNOWLEDGE_BASE.put("java",
            "## Java Programming Guide\n\n" +
            "**Hello World:**\n```java\npublic class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}\n```\n\n" +
            "**Key Concepts:** OOP (classes, inheritance, polymorphism), Collections (List, Map, Set), Streams API, Lambda expressions, Exception handling, Generics, Multithreading.\n\n" +
            "**Build Tools:** Maven (`mvn clean install`) or Gradle (`./gradlew build`).");

        // Python
        KNOWLEDGE_BASE.put("python",
            "## Python Development Guide\n\n" +
            "**Setup:**\n```bash\npython -m venv venv\nsource venv/bin/activate  # Linux/Mac\npip install flask requests\n```\n\n" +
            "**Flask API Example:**\n```python\nfrom flask import Flask, jsonify, request\n\napp = Flask(__name__)\n\n@app.route('/api/hello', methods=['GET'])\ndef hello():\n    name = request.args.get('name', 'World')\n    return jsonify({'message': f'Hello {name}!'})\n\nif __name__ == '__main__':\n    app.run(debug=True)\n```\n\n" +
            "**FastAPI (Modern Alternative):**\n```python\nfrom fastapi import FastAPI\napp = FastAPI()\n\n@app.get('/api/hello/{name}')\nasync def hello(name: str):\n    return {'message': f'Hello {name}!'}\n```");

        // Docker
        KNOWLEDGE_BASE.put("docker",
            "## Docker Guide\n\n" +
            "**Dockerfile Example:**\n```dockerfile\nFROM node:18-alpine\nWORKDIR /app\nCOPY package*.json ./\nRUN npm install\nCOPY . .\nEXPOSE 3000\nCMD [\"npm\", \"start\"]\n```\n\n" +
            "**Commands:**\n```bash\ndocker build -t myapp .\ndocker run -p 3000:3000 myapp\ndocker-compose up -d\n```");

        // Git
        KNOWLEDGE_BASE.put("git",
            "## Git Version Control Guide\n\n" +
            "**Essential Commands:**\n```bash\ngit init                    # Initialize repo\ngit clone <url>             # Clone remote repo\ngit add .                   # Stage all changes\ngit commit -m \"message\"     # Commit\ngit push origin main        # Push to remote\ngit pull                    # Pull latest changes\ngit branch feature-x        # Create branch\ngit checkout feature-x      # Switch branch\ngit merge feature-x         # Merge branch\n```\n\n" +
            "**Branching Strategy:** Use `main` for production, `develop` for staging, `feature/*` for new features.");

        // Database / SQL
        KNOWLEDGE_BASE.put("sql",
            "## SQL Database Guide\n\n" +
            "**Basic Operations:**\n```sql\nCREATE TABLE users (id SERIAL PRIMARY KEY, name VARCHAR(100), email VARCHAR(255) UNIQUE);\nINSERT INTO users (name, email) VALUES ('John', 'john@example.com');\nSELECT * FROM users WHERE name LIKE '%John%';\nUPDATE users SET name = 'Jane' WHERE id = 1;\nDELETE FROM users WHERE id = 1;\n```\n\n" +
            "**JOINs:** INNER JOIN, LEFT JOIN, RIGHT JOIN, FULL OUTER JOIN.");

        KNOWLEDGE_BASE.put("database", KNOWLEDGE_BASE.get("sql"));
        KNOWLEDGE_BASE.put("mongodb", 
            "## MongoDB Guide\n\n```javascript\n// Insert\ndb.users.insertOne({name: 'John', age: 30});\n// Find\ndb.users.find({age: {$gt: 25}});\n// Update\ndb.users.updateOne({name: 'John'}, {$set: {age: 31}});\n// Delete\ndb.users.deleteOne({name: 'John'});\n```");

        // Node.js
        KNOWLEDGE_BASE.put("node",
            "## Node.js Guide\n\n" +
            "**Express API:**\n```javascript\nconst express = require('express');\nconst app = express();\napp.use(express.json());\n\napp.get('/api/users', (req, res) => {\n  res.json([{id: 1, name: 'John'}]);\n});\n\napp.listen(3000, () => console.log('Server running on port 3000'));\n```\n\n" +
            "**Setup:** `npm init -y && npm install express`");

        // TypeScript
        KNOWLEDGE_BASE.put("typescript",
            "## TypeScript Guide\n\n```typescript\ninterface User {\n  id: number;\n  name: string;\n  email: string;\n}\n\nconst getUser = async (id: number): Promise<User> => {\n  const res = await fetch(`/api/users/${id}`);\n  return res.json();\n};\n```\n\n**Setup:** `npm install -g typescript && tsc --init`");

        // CSS / HTML
        KNOWLEDGE_BASE.put("css",
            "## CSS Styling Guide\n\n**Flexbox Layout:**\n```css\n.container {\n  display: flex;\n  justify-content: center;\n  align-items: center;\n  gap: 1rem;\n}\n```\n\n**CSS Grid:**\n```css\n.grid {\n  display: grid;\n  grid-template-columns: repeat(3, 1fr);\n  gap: 1rem;\n}\n```\n\n**Responsive Design:** Use `@media (max-width: 768px) { ... }` for mobile styles.");

        KNOWLEDGE_BASE.put("html",
            "## HTML Guide\n\n```html\n<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n  <title>My Page</title>\n</head>\n<body>\n  <header><h1>Welcome</h1></header>\n  <main><p>Content here</p></main>\n  <footer>&copy; 2026</footer>\n</body>\n</html>\n```");

        // Kubernetes
        KNOWLEDGE_BASE.put("kubernetes",
            "## Kubernetes Guide\n\n```yaml\napiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: myapp\nspec:\n  replicas: 3\n  selector:\n    matchLabels:\n      app: myapp\n  template:\n    spec:\n      containers:\n      - name: myapp\n        image: myapp:latest\n        ports:\n        - containerPort: 8080\n```\n\n**Commands:** `kubectl apply -f deployment.yaml`, `kubectl get pods`, `kubectl logs <pod>`");

        // Machine Learning
        KNOWLEDGE_BASE.put("machine learning",
            "## Machine Learning Guide\n\n**Getting Started with scikit-learn:**\n```python\nfrom sklearn.model_selection import train_test_split\nfrom sklearn.ensemble import RandomForestClassifier\n\nX_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)\nmodel = RandomForestClassifier(n_estimators=100)\nmodel.fit(X_train, y_train)\naccuracy = model.score(X_test, y_test)\n```\n\n**Key Libraries:** scikit-learn, TensorFlow, PyTorch, pandas, numpy.");

        // API
        KNOWLEDGE_BASE.put("api",
            "## REST API Design Guide\n\n**Best Practices:**\n- Use nouns for URLs: `/api/users`, `/api/products`\n- Use HTTP methods: GET (read), POST (create), PUT (update), DELETE (remove)\n- Return proper status codes: 200, 201, 400, 404, 500\n- Use pagination: `?page=1&limit=20`\n- Version your API: `/api/v1/users`\n- Use JSON for request/response bodies\n- Implement authentication (JWT, OAuth2)");

        // Firebase
        KNOWLEDGE_BASE.put("firebase",
            "## Firebase Guide\n\n**Setup:**\n```bash\nnpm install firebase\nfirebase init\nfirebase deploy\n```\n\n**Firestore CRUD:**\n```javascript\nimport { collection, addDoc, getDocs } from 'firebase/firestore';\n\n// Create\nawait addDoc(collection(db, 'users'), {name: 'John', age: 30});\n\n// Read\nconst snapshot = await getDocs(collection(db, 'users'));\nsnapshot.forEach(doc => console.log(doc.data()));\n```");

        // Linux
        KNOWLEDGE_BASE.put("linux",
            "## Linux Essential Commands\n\n```bash\nls -la          # List files with details\ncd /path        # Change directory\nmkdir -p dir    # Create directory\ncp -r src dest  # Copy recursively\nchmod 755 file  # Set permissions\ngrep -r \"text\" . # Search in files\nps aux          # List processes\nkill -9 PID     # Kill process\ndf -h           # Disk usage\nfree -m         # Memory usage\n```");

        // AWS / GCP / Cloud
        KNOWLEDGE_BASE.put("aws", "## AWS Guide\n\nKey Services: EC2 (compute), S3 (storage), RDS (database), Lambda (serverless), CloudFront (CDN).\n\n**Deploy:** `aws s3 sync ./build s3://my-bucket`\n**Lambda:** Write functions triggered by events (API Gateway, S3, SNS).");
        KNOWLEDGE_BASE.put("gcp", "## Google Cloud Guide\n\nKey Services: Compute Engine, Cloud Run, Cloud Storage, Firestore, BigQuery.\n\n**Deploy to Cloud Run:**\n```bash\ngcloud builds submit --tag gcr.io/PROJECT/myapp\ngcloud run deploy myapp --image gcr.io/PROJECT/myapp\n```");

        // Angular / Vue / Next.js
        KNOWLEDGE_BASE.put("angular", "## Angular Guide\n\n```bash\nnpm install -g @angular/cli\nng new my-app\ncd my-app && ng serve\n```\n\n**Component:** `ng generate component my-component`\n**Service:** `ng generate service my-service`");
        
        KNOWLEDGE_BASE.put("vue", "## Vue.js Guide\n\n```bash\nnpm create vue@latest my-app\ncd my-app && npm run dev\n```\n\n**Component:**\n```vue\n<template>\n  <div>{{ message }}</div>\n</template>\n<script setup>\nimport { ref } from 'vue';\nconst message = ref('Hello Vue!');\n</script>\n```");

        KNOWLEDGE_BASE.put("next", "## Next.js Guide\n\n```bash\nnpx create-next-app@latest my-app\ncd my-app && npm run dev\n```\n\n**App Router (Next.js 13+):**\n- `app/page.tsx` — Home page\n- `app/api/route.ts` — API routes\n- Use `'use client'` for client components\n- Server components by default");

        // Greetings
        KNOWLEDGE_BASE.put("hello", "Hello! I'm SupremeAI. I can help you with programming, development, DevOps, databases, and more. What would you like to know?");
        KNOWLEDGE_BASE.put("hi", KNOWLEDGE_BASE.get("hello"));
        KNOWLEDGE_BASE.put("hey", KNOWLEDGE_BASE.get("hello"));
        
        // LLM/AI explanations
        KNOWLEDGE_BASE.put("llm", "A Large Language Model (LLM) is an AI system trained on vast amounts of text data to understand and generate human-like language. Examples include GPT, Claude, Gemini, and Llama. LLMs can answer questions, write code, summarize text, and assist with various tasks through pattern recognition and prediction.");
        KNOWLEDGE_BASE.put("large language model", KNOWLEDGE_BASE.get("llm"));
        KNOWLEDGE_BASE.put("artificial intelligence", "Artificial Intelligence (AI) is the simulation of human intelligence in machines. It includes learning (gathering information), reasoning (using rules), and self-correction. AI is used in many applications like recommendation systems, image recognition, and natural language processing.");
        KNOWLEDGE_BASE.put("ai", "AI stands for Artificial Intelligence. It's the simulation of human intelligence in machines that can learn, reason, and self-correct. AI powers everything from recommendation systems to autonomous vehicles.");
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
        caps.put("topicCount", KNOWLEDGE_BASE.size());
        return caps;
    }

    @Override
    public Mono<String> generate(String prompt) {
        log.info("[StubLocalProvider] Generating real response for: {}", prompt);
        return Mono.just(generateRealResponse(prompt));
    }

    private String generateRealResponse(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "Please ask me a question — I can help with programming, development, DevOps, databases, and more.";
        }

        String p = prompt.toLowerCase().trim();

        // Handle "what is X" and similar factual questions FIRST
        // These should be answered directly, not with system architecture
        if (p.contains("what is") || p.contains("what are") || p.contains("explain") || p.contains("tell me about")) {
            // Check for specific concepts in the prompt
            for (Map.Entry<String, String> entry : KNOWLEDGE_BASE.entrySet()) {
                String key = entry.getKey();
                // For "what is X" questions, check if the topic keyword is in the prompt
                if (p.contains("what is " + key) || p.contains("what are " + key) || 
                    p.contains("explain " + key) || p.contains("tell me about " + key) ||
                    p.contains("about " + key)) {
                    return entry.getValue();
                }
            }
        }

        // Try exact topic match first, then partial match
        for (Map.Entry<String, String> entry : KNOWLEDGE_BASE.entrySet()) {
            if (p.contains(entry.getKey())) {
                log.info("[StubLocalProvider] Matched topic: {}", entry.getKey());
                return entry.getValue();
            }
        }

        // Multi-word topic matching (e.g., "create flutter app" → matches "flutter")
        String[] words = p.split("\\s+");
        for (String word : words) {
            if (word.length() > 2 && KNOWLEDGE_BASE.containsKey(word)) {
                log.info("[StubLocalProvider] Word-matched topic: {}", word);
                return KNOWLEDGE_BASE.get(word);
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