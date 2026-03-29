package org.example.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Phase 3: Template Manager
 * 
 * Provides project templates for multiple frameworks.
 * Each template includes:
 * - Directory structure (proper MVC/component architecture)
 * - Boilerplate files (package.json, pubspec.yaml, build.gradle, etc.)
 * - Configuration files (.env, .gitignore, eslintrc, etc.)
 * - README with setup instructions
 * 
 * Supported frameworks:
 * - React (TypeScript)
 * - Node.js (Express.js)
 * - Flutter (Dart)
 * - Python (FastAPI)
 * - Java (Spring Boot)
 */
public class TemplateManager {
    private final Path templatePath;
    private final FileOrchestrator fileOrchestrator;
    private final Map<String, TemplateConfig> templateConfigs = new HashMap<>();

    public TemplateManager(String templateDir, FileOrchestrator fileOrchestrator) {
        this.templatePath = Paths.get(templateDir);
        this.fileOrchestrator = fileOrchestrator;
        initializeTemplates();
    }

    /**
     * Configuration for each template type
     */
    private static class TemplateConfig {
        String name;
        String[] directories;
        Map<String, String> files;
        String description;

        TemplateConfig(String name, String description, String[] dirs, Map<String, String> files) {
            this.name = name;
            this.description = description;
            this.directories = dirs;
            this.files = files;
        }
    }

    private void initializeTemplates() {
        // React Template (TypeScript + Vite)
        templateConfigs.put("REACT", new TemplateConfig(
            "React",
            "Modern React app with TypeScript and Vite",
            new String[]{"src", "src/components", "src/hooks", "src/pages", "src/services", "src/types", "public", "dist"},
            Map.ofEntries(
                Map.entry("package.json", getReactPackageJson()),
                Map.entry("tsconfig.json", getReactTsConfig()),
                Map.entry(".gitignore", getGitignore()),
                Map.entry("README.md", "# React Project\n\nModern React app with TypeScript\n\n## Setup\nnpm install\nnpm run dev"),
                Map.entry(".env.example", "VITE_API_URL=http://localhost:3000"),
                Map.entry(".eslintrc.json", getEslintConfig())
            )
        ));

        // Node.js Template (Express.js)
        templateConfigs.put("NODEJS", new TemplateConfig(
            "Node.js",
            "Express.js REST API server",
            new String[]{"src", "src/routes", "src/models", "src/middleware", "src/controllers", "src/services", "tests", "config"},
            Map.ofEntries(
                Map.entry("package.json", getNodePackageJson()),
                Map.entry("tsconfig.json", getNodeTsConfig()),
                Map.entry(".gitignore", getGitignore()),
                Map.entry("README.md", "# Node.js API\n\nExpress.js REST API\n\n## Setup\nnpm install\nnpm run dev"),
                Map.entry(".env.example", "PORT=3000\nNODE_ENV=development\nDB_URL=mongodb://localhost")
            )
        ));

        // Flutter Template (Dart)
        templateConfigs.put("FLUTTER", new TemplateConfig(
            "Flutter",
            "Flutter mobile app",
            new String[]{"lib", "lib/models", "lib/screens", "lib/widgets", "lib/services", "test", "assets", "assets/images"},
            Map.ofEntries(
                Map.entry("pubspec.yaml", getFlutterPubspec()),
                Map.entry(".gitignore", getGitignore()),
                Map.entry("README.md", "# Flutter App\n\nFlutter mobile application\n\n## Setup\nflutter pub get\nflutter run"),
                Map.entry("lib/main.dart", "void main() {\n  runApp(const MyApp());\n}\n\nclass MyApp extends StatelessWidget {\n  const MyApp();\n  \n  @override\n  Widget build(BuildContext context) => MaterialApp(\n    home: Scaffold(\n      appBar: AppBar(title: const Text('Flutter App')),\n      body: const Center(child: Text('Hello World')),\n    ),\n  );\n}")
            )
        ));

        // Python Template (FastAPI)
        templateConfigs.put("PYTHON", new TemplateConfig(
            "Python",
            "FastAPI REST API server",
            new String[]{"app", "app/routes", "app/models", "app/services", "tests", "config"},
            Map.ofEntries(
                Map.entry("requirements.txt", "fastapi==0.104.1\nuvicorn==0.24.0\npydantic==2.5.0\npython-dotenv==1.0.0"),
                Map.entry(".gitignore", getGitignore()),
                Map.entry("README.md", "# FastAPI Project\n\nPython FastAPI REST API\n\n## Setup\npip install -r requirements.txt\nuvicorn app.main:app --reload"),
                Map.entry(".env.example", "DEBUG=True\nDATABASE_URL=sqlite:///./test.db"),
                Map.entry("app/main.py", "from fastapi import FastAPI\n\napp = FastAPI(title='API')\n\n@app.get('/')\nasync def root():\n    return {'message': 'Hello World'}")
            )
        ));

        // Java Spring Boot Template
        templateConfigs.put("JAVA", new TemplateConfig(
            "Java",
            "Spring Boot REST API",
            new String[]{"src/main/java", "src/main/resources", "src/test/java", "src/main/java/com/example/controller",
                        "src/main/java/com/example/service", "src/main/java/com/example/model"},
            Map.ofEntries(
                Map.entry("pom.xml", getSpringBootPom()),
                Map.entry(".gitignore", getGitignore()),
                Map.entry("README.md", "# Spring Boot API\n\nJava Spring Boot REST API\n\n## Setup\nmvn clean install\nmvn spring-boot:run"),
                Map.entry("src/main/resources/application.properties", "spring.application.name=api\nserver.port=8080\nspring.jpa.hibernate.ddl-auto=update")
            )
        ));
    }

    /**
     * Initialize a project with the specified template
     */
    public void initializeProject(String projectId, String templateType) throws IOException {
        templateType = templateType.toUpperCase();
        TemplateConfig config = templateConfigs.get(templateType);
        
        if (config == null) {
            throw new IllegalArgumentException("Unknown template type: " + templateType);
        }
        
        System.out.println("📂 [TEMPLATE] Initializing " + config.name + " project: " + projectId);
        
        // Create all directories
        for (String dir : config.directories) {
            fileOrchestrator.writeFile(projectId, dir + "/.keep", "");
        }
        
        // Create all template files
        for (Map.Entry<String, String> file : config.files.entrySet()) {
            fileOrchestrator.writeFile(projectId, file.getKey(), file.getValue());
        }
        
        System.out.println("✅ [TEMPLATE] " + config.name + " structure ready for " + projectId);
    }

    /**
     * List all available templates
     */
    public List<Map<String, String>> listTemplates() {
        List<Map<String, String>> templates = new ArrayList<>();
        
        for (TemplateConfig config : templateConfigs.values()) {
            Map<String, String> info = new HashMap<>();
            info.put("name", config.name);
            info.put("description", config.description);
            info.put("directories", String.valueOf(config.directories.length));
            info.put("files", String.valueOf(config.files.size()));
            templates.add(info);
        }
        
        return templates;
    }

    /**
     * Get template info by type
     */
    public Map<String, Object> getTemplateInfo(String templateType) {
        templateType = templateType.toUpperCase();
        TemplateConfig config = templateConfigs.get(templateType);
        
        if (config == null) {
            return Map.of("error", "Template not found");
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("name", config.name);
        info.put("description", config.description);
        info.put("directories", config.directories);
        info.put("files", config.files.keySet());
        
        return info;
    }

    // ============================================================================
    // TEMPLATE CONTENT GENERATORS
    // ============================================================================

    private String getReactPackageJson() {
        return """
{
  "name": "react-app",
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "lint": "eslint .",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "@vitejs/plugin-react": "^4.2.0",
    "eslist": "^8.54.0",
    "typescript": "^5.3.0",
    "vite": "^5.0.0"
  }
}""";
    }

    private String getReactTsConfig() {
        return """
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "esModuleInterop": true,
    "allowSyntheticDefaultImports": true,
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "resolveJsonModule": true,
    "moduleResolution": "bundler"
  }
}""";
    }

    private String getNodePackageJson() {
        return """
{
  "name": "node-api",
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "node --loader ts-node/esm src/index.ts",
    "build": "tsc",
    "start": "node dist/index.js",
    "test": "jest"
  },
  "dependencies": {
    "express": "^4.18.0",
    "dotenv": "^16.3.0",
    "cors": "^2.8.5",
    "body-parser": "^1.20.0"
  },
  "devDependencies": {
    "@types/express": "^4.17.0",
    "@types/node": "^20.0.0",
    "typescript": "^5.3.0",
    "ts-node": "^10.9.0",
    "jest": "^29.7.0"
  }
}""";
    }

    private String getNodeTsConfig() {
        return """
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "lib": ["ES2020"],
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true
  },
  "include": ["src"],
  "exclude": ["node_modules", "dist"]
}""";
    }

    private String getFlutterPubspec() {
        return """
name: flutter_app
description: Flutter application
version: 0.1.0

environment:
  sdk: '>=3.0.0 <4.0.0'

dependencies:
  flutter:
    sdk: flutter
  cupertino_icons: ^1.0.0
  provider: ^6.0.0
  http: ^1.1.0

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.0

flutter:
  uses-material-design: true
  assets:
    - assets/images/""";
    }

    private String getSpringBootPom() {
        return """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
    http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  
  <groupId>com.example</groupId>
  <artifactId>api</artifactId>
  <version>0.1.0</version>
  <packaging>jar</packaging>
  
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.0</version>
  </parent>
  
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
  
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>""";
    }

    private String getGitignore() {
        return """
# IDEs
.idea/
.vscode/
*.swp
*.swo
*~
.project
.classpath
.c9/
*.launch
.settings/
*.sublime-workspace

# Dependencies
node_modules/
.pub-cache/
.pub/
/build/
/target/
/.flutter-plugins
/.flutter-plugins-dependencies

# Environment
.env
.env.local
.env.*.local

# Generated files
/dist/
/lib/generated
*.g.dart

# OS
.DS_Store
Thumbs.db

# Misc
.cache
.tmp""";
    }

    private String getEslintConfig() {
        return """
{
  "env": {
    "browser": true,
    "es2021": true
  },
  "extends": [
    "eslint:recommended",
    "plugin:react/recommended"
  ],
  "parser": "@typescript-eslint/parser",
  "parserOptions": {
    "ecmaVersion": "latest",
    "sourceType": "module"
  },
  "plugins": [
    "react",
    "@typescript-eslint"
  ],
  "rules": {
    "react/react-in-jsx-scope": "off"
  }
}""";
    }
}
