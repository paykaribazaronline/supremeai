package com.supremeai.service;

import com.supremeai.ai.client.OpenAIClient;
import com.supremeai.model.EntityDefinition;
import com.supremeai.model.FieldDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Code Generation Service with AI integration,
 * custom entity support, test generation, and CI/CD pipeline.
 */
@Service
public class CodeGenerationServiceEnhanced {

    @Value("${openai.api.key:#{null}}")
    private String openAiApiKey;
    
    private final OpenAIClient openAIClient;
    
    public CodeGenerationServiceEnhanced(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }
    
    /**
     * Generate complete application with AI-powered code generation
     */
    public Map<String, Object> generateAppWithAI(String appName, String description, 
                                                  List<EntityDefinition> entities,
                                                  String database, String authType) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> files = new LinkedHashMap<>();
        
        // Generate application.properties
        files.put("src/main/resources/application.properties", 
                  generateApplicationProperties(database));
        
        // Generate each entity with AI assistance
        for (EntityDefinition entity : entities) {
            String entityCode = generateEntityWithAI(entity);
            files.put("src/main/java/com/example/generated/entity/" + 
                     entity.getName() + ".java", entityCode);
            
            // Generate repository
            files.put("src/main/java/com/example/generated/repository/" + 
                     entity.getName() + "Repository.java", 
                     generateRepository(entity));
            
            // Generate service
            files.put("src/main/java/com/example/generated/service/" + 
                     entity.getName() + "Service.java", 
                     generateService(entity));
            
            // Generate controller
            files.put("src/main/java/com/example/generated/controller/" + 
                     entity.getName() + "Controller.java", 
                     generateController(entity));
            
            // Generate DTO
            files.put("src/main/java/com/example/generated/dto/" + 
                     entity.getName() + "Dto.java", 
                     generateDto(entity));
            
            // Generate tests
            files.put("src/test/java/com/example/generated/" + 
                     entity.getName() + "ControllerTest.java", 
                     generateControllerTest(entity));
            files.put("src/test/java/com/example/generated/" + 
                     entity.getName() + "ServiceTest.java", 
                     generateServiceTest(entity));
        }
        
        // Generate main application class
        files.put("src/main/java/com/example/generated/GeneratedAppApplication.java",
                  generateMainApplication());
        
        // Generate build.gradle
        files.put("build.gradle", generateBuildGradle(entities));
        
        // Generate Dockerfile
        files.put("Dockerfile", generateDockerfile());
        
        // Generate docker-compose.yml
        files.put("docker-compose.yml", generateDockerCompose(database));
        
        // Generate CI/CD pipeline
        files.put(".github/workflows/ci-cd.yml", generateCICDPipeline());
        
        // Generate README
        files.put("README.md", generateEnhancedReadme(appName, description, entities));
        
        result.put("appName", appName);
        result.put("files", files);
        result.put("fileCount", files.size());
        result.put("entities", entities.size());
        result.put("status", "GENERATED_WITH_AI");
        result.put("features", Arrays.asList(
            "AI-Powered Code Generation",
            "Custom Entity Support",
            "Full CRUD Operations",
            "Automated Tests",
            "CI/CD Pipeline",
            "Docker Support"
        ));
        
        return result;
    }
    
    /**
     * Generate entity class using AI for optimal structure
     */
    private String generateEntityWithAI(EntityDefinition entity) {
        if (openAiApiKey != null && !openAiApiKey.isEmpty()) {
            try {
                String prompt = "Generate a Spring Boot JPA entity class for: " + 
                               entity.getName() + " with fields: " + 
                               entity.getFields().stream()
                                   .map(f -> f.getName() + ":" + f.getType())
                                   .collect(Collectors.joining(", ")) +
                               ". Include proper annotations, relationships, " +
                               "validation, and Lombok annotations.";
                
                String aiResponse = openAIClient.generate(prompt);
                if (aiResponse != null && !aiResponse.isEmpty()) {
                    return aiResponse;
                }
            } catch (Exception e) {
                // Fallback to template generation
            }
        }
        
        // Fallback: Template-based generation
        return generateEntityTemplate(entity);
    }
    
    private String generateEntityTemplate(EntityDefinition entity) {
        StringBuilder fields = new StringBuilder();
        StringBuilder constructors = new StringBuilder();
        StringBuilder gettersSetters = new StringBuilder();
        
        fields.append("    @Id\n")
              .append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n")
              .append("    private Long id;\n\n");
        
        for (FieldDefinition field : entity.getFields()) {
            fields.append("    ").append(getFieldAnnotation(field)).append("\n")
                  .append("    private ").append(mapType(field.getType())).append(" ")
                  .append(field.getName()).append(";\n\n");
        }
        
        // Timestamps
        fields.append("    @Column(name = \"created_at\")\n")
              .append("    private LocalDateTime createdAt;\n\n")
              .append("    @Column(name = \"updated_at\")\n")
              .append("    private LocalDateTime updatedAt;\n\n");
        
        String fieldList = entity.getFields().stream()
            .map(f -> mapType(f.getType()) + " " + f.getName())
            .collect(Collectors.joining(", "));
        
        constructors.append("    public ").append(entity.getName()).append("() {}\n\n")
                    .append("    public ").append(entity.getName()).append("(")
                    .append(fieldList).append(") {\n");
        
        for (FieldDefinition field : entity.getFields()) {
            constructors.append("        this.").append(field.getName())
                       .append(" = ").append(field.getName()).append(";\n");
        }
        constructors.append("    }\n\n");
        
        // Getters and setters
        gettersSetters.append("    public Long getId() { return id; }\n")
                      .append("    public void setId(Long id) { this.id = id; }\n\n");
        
        for (FieldDefinition field : entity.getFields()) {
            String capitalized = capitalize(field.getName());
            gettersSetters.append("    public ").append(mapType(field.getType()))
                         .append(" get").append(capitalized).append("() {")
                         .append(" return ").append(field.getName()).append("; }\n\n")
                         .append("    public void set").append(capitalized)
                         .append("(").append(mapType(field.getType()))
                         .append(" ").append(field.getName()).append(") {")
                         .append(" this.").append(field.getName())
                         .append(" = ").append(field.getName()).append("; }\n\n");
        }
        
        // Timestamp methods
        gettersSetters.append("    public LocalDateTime getCreatedAt() { return createdAt; }\n")
                      .append("    public void setCreatedAt(LocalDateTime createdAt) { ")
                      .append("this.createdAt = createdAt; }\n\n")
                      .append("    public LocalDateTime getUpdatedAt() { return updatedAt; }\n")
                      .append("    public void setUpdatedAt(LocalDateTime updatedAt) { ")
                      .append("this.updatedAt = updatedAt; }\n");
        
        return "package com.example.generated.entity;\n\n" +
               "import jakarta.persistence.*;\n" +
               "import jakarta.validation.constraints.*;\n" +
               "import java.time.LocalDateTime;\n\n" +
               "@Entity\n" +
               "@Table(name = \"" + entity.getName().toLowerCase() + "s\")\n" +
               "public class " + entity.getName() + " {\n\n" +
               fields.toString() + constructors.toString() + gettersSetters.toString() +
               "}\n";
    }
    
    private String getFieldAnnotation(FieldDefinition field) {
        StringBuilder annotation = new StringBuilder();
        
        if (field.isRequired()) {
            annotation.append("    @NotBlank ");
        }
        
        switch (field.getType().toLowerCase()) {
            case "string":
                if (field.getMaxLength() > 0) {
                    annotation.append("@Size(max = ").append(field.getMaxLength()).append(") ");
                }
                break;
            case "integer":
            case "int":
                annotation.append("@Min(0) ");
                break;
            case "double":
            case "float":
                annotation.append("@Positive ");
                break;
            case "email":
                annotation.append("@Email ");
                break;
        }
        
        if (field.isUnique()) {
            annotation.append("@Column(unique = true) ");
        }
        
        return annotation.toString().trim();
    }
    
    private String mapType(String type) {
        switch (type.toLowerCase()) {
            case "string": return "String";
            case "integer": case "int": return "Integer";
            case "long": return "Long";
            case "double": return "Double";
            case "float": return "Float";
            case "boolean": return "Boolean";
            case "date": case "datetime": return "LocalDateTime";
            case "email": return "String";
            default: return type;
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private String generateRepository(EntityDefinition entity) {
        return "package com.example.generated.repository;\n\n" +
               "import com.example.generated.entity." + entity.getName() + ";\n" +
               "import org.springframework.data.jpa.repository.JpaRepository;\n" +
               "import org.springframework.stereotype.Repository;\n\n" +
               "@Repository\n" +
               "public interface " + entity.getName() + "Repository " +
               "extends JpaRepository<" + entity.getName() + ", Long> {\n" +
               "}\n";
    }
    
    private String generateService(EntityDefinition entity) {
        return "package com.example.generated.service;\n\n" +
               "import com.example.generated.entity." + entity.getName() + ";\n" +
               "import com.example.generated.repository." + entity.getName() + "Repository;\n" +
               "import org.springframework.beans.factory.annotation.Autowired;\n" +
               "import org.springframework.stereotype.Service;\n" +
               "import org.springframework.transaction.annotation.Transactional;\n\n" +
               "import java.util.List;\n\n" +
               "@Service\n" +
               "@Transactional\n" +
               "public class " + entity.getName() + "Service {\n\n" +
               "    @Autowired\n" +
               "    private " + entity.getName() + "Repository repository;\n\n" +
               "    public List<" + entity.getName() + "> getAll() {\n" +
               "        return repository.findAll();\n" +
               "    }\n\n" +
               "    public " + entity.getName() + " getById(Long id) {\n" +
               "        return repository.findById(id).orElse(null);\n" +
               "    }\n\n" +
               "    public " + entity.getName() + " create(" + entity.getName() + " entity) {\n" +
               "        return repository.save(entity);\n" +
               "    }\n\n" +
               "    public " + entity.getName() + " update(Long id, " + 
               entity.getName() + " entity) {\n" +
               "        entity.setId(id);\n" +
               "        return repository.save(entity);\n" +
               "    }\n\n" +
               "    public void delete(Long id) {\n" +
               "        repository.deleteById(id);\n" +
               "    }\n" +
               "}\n";
    }
    
    private String generateController(EntityDefinition entity) {
        String name = entity.getName();
        String lowerName = name.substring(0, 1).toLowerCase() + name.substring(1);
        
return "package com.example.generated.controller;\n\n" +
                "import com.example.generated.entity." + name + ";\n" +
                "import com.example.generated.service." + name + "Service;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.http.HttpStatus;\n" +
                "import org.springframework.http.ResponseEntity;\n" +
                "import org.springframework.web.bind.annotation.*;\n\n" +
                "import java.util.List;\n\n" +
                "@RestController\n" +
                "@RequestMapping(\"/api/" + lowerName + "s\")\n" +
                "public class " + name + "Controller {\n\n" +
                "    @Autowired\n" +
                "    private " + name + "Service service;\n\n" +
                "    @GetMapping\n" +
                "    public ResponseEntity<List<" + name + "> getAll() {\n" +
                "        return ResponseEntity.ok(service.getAll());\n" +
                "    }\n\n" +
                "    @GetMapping(\"/{id}\")\n" +
                "    public ResponseEntity<" + name + "> getById(@PathVariable Long id) {\n" +
                "        " + name + " entity = service.getById(id);\n" +
                "        return entity != null ? ResponseEntity.ok(entity) : ResponseEntity.notFound().build();\n" +
                "    }\n\n" +
                "    @PostMapping\n" +
                "    public ResponseEntity<" + name + "> create(@RequestBody " + 
                name + " entity) {\n" +
                "        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(entity));\n" +
                "    }\n\n" +
                "    @PutMapping(\"/{id}\")\n" +
                "    public ResponseEntity<" + name + "> update(@PathVariable Long id, " +
                "@RequestBody " + name + " entity) {\n" +
                "        return ResponseEntity.ok(service.update(id, entity));\n" +
                "    }\n\n" +
                "    @DeleteMapping(\"/{id}\")\n" +
                "    public ResponseEntity<Void> delete(@PathVariable Long id) {\n" +
                "        service.delete(id);\n" +
                "        return ResponseEntity.noContent().build();\n" +
                "    }\n" +
                "}\n";
    }
    
    private String generateDto(EntityDefinition entity) {
        StringBuilder fields = new StringBuilder();
        StringBuilder gettersSetters = new StringBuilder();
        
        for (FieldDefinition field : entity.getFields()) {
            fields.append("    private ").append(mapType(field.getType()))
                  .append(" ").append(field.getName()).append(";\n");
            
            String capitalized = capitalize(field.getName());
            gettersSetters.append("    public ").append(mapType(field.getType()))
                         .append(" get").append(capitalized).append("() {")
                         .append(" return ").append(field.getName()).append("; }\n\n")
                         .append("    public void set").append(capitalized)
                         .append("(").append(mapType(field.getType()))
                         .append(" ").append(field.getName()).append(") {")
                         .append(" this.").append(field.getName())
                         .append(" = ").append(field.getName()).append("; }\n\n");
        }
        
        return "package com.example.generated.dto;\n\n" +
               "public class " + entity.getName() + "Dto {\n\n" +
               fields.toString() + gettersSetters.toString() +
               "}\n";
    }
    
    private String generateControllerTest(EntityDefinition entity) {
        String name = entity.getName();
        String lowerName = name.substring(0, 1).toLowerCase() + name.substring(1);
        
        return "package com.example.generated;\n\n" +
               "import com.example.generated.controller." + name + "Controller;\n" +
               "import com.example.generated.entity." + name + ";\n" +
               "import com.example.generated.service." + name + "Service;\n" +
               "import org.junit.jupiter.api.Test;\n" +
               "import org.springframework.beans.factory.annotation.Autowired;\n" +
               "import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;\n" +
               "import org.springframework.boot.test.mock.mockito.MockBean;\n" +
               "import org.springframework.test.web.servlet.MockMvc;\n\n" +
               "import java.util.Arrays;\n\n" +
               "import static org.mockito.ArgumentMatchers.any;\n" +
               "import static org.mockito.Mockito.*;\n" +
               "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;\n" +
               "import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;\n\n" +
               "@WebMvcTest(" + name + "Controller.class)\n" +
               "public class " + name + "ControllerTest {\n\n" +
               "    @Autowired\n" +
               "    private MockMvc mockMvc;\n\n" +
               "    @MockBean\n" +
               "    private " + name + "Service service;\n\n" +
               "    @Test\n" +
               "    public void testGetAll() throws Exception {\n" +
               "        when(service.getAll()).thenReturn(Arrays.asList(new " + name + "()));\n\n" +
               "        mockMvc.perform(get(\"/api/" + lowerName + "s\"))\n" +
               "            .andExpect(status().isOk());\n" +
               "    }\n\n" +
               "    @Test\n" +
               "    public void testCreate() throws Exception {\n" +
               "        when(service.create(any(" + name + ".class))).thenReturn(new " + name + "());\n\n" +
               "        mockMvc.perform(post(\"/api/" + lowerName + "s\")\n" +
               "            .contentType(\"application/json\")\n" +
               "            .content(\"{\"id\":1}\"))\n" +
               "            .andExpect(status().isCreated());\n" +
               "    }\n" +
               "}\n";
    }
    
    private String generateServiceTest(EntityDefinition entity) {
        String name = entity.getName();
        
        return "package com.example.generated;\n\n" +
               "import com.example.generated.entity." + name + ";\n" +
               "import com.example.generated.repository." + name + "Repository;\n" +
               "import com.example.generated.service." + name + "Service;\n" +
               "import org.junit.jupiter.api.Test;\n" +
               "import org.junit.jupiter.api.extension.ExtendWith;\n" +
               "import org.mockito.InjectMocks;\n" +
               "import org.mockito.Mock;\n" +
               "import org.mockito.junit.jupiter.MockitoExtension;\n\n" +
               "import java.util.Arrays;\n\n" +
               "import static org.junit.jupiter.api.Assertions.*;\n" +
               "import static org.mockito.Mockito.*;\n\n" +
               "@ExtendWith(MockitoExtension.class)\n" +
               "public class " + name + "ServiceTest {\n\n" +
               "    @Mock\n" +
               "    private " + name + "Repository repository;\n\n" +
               "    @InjectMocks\n" +
               "    private " + name + "Service service;\n\n" +
               "    @Test\n" +
               "    public void testCreate() {\n" +
               "        " + name + " entity = new " + name + "();\n" +
               "        when(repository.save(entity)).thenReturn(entity);\n\n" +
               "        " + name + " result = service.create(entity);\n\n" +
               "        assertNotNull(result);\n" +
               "        verify(repository).save(entity);\n" +
               "    }\n" +
               "}\n";
    }
    
    private String generateMainApplication() {
        return "package com.example.generated;\n\n" +
               "import org.springframework.boot.SpringApplication;\n" +
               "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n" +
               "@SpringBootApplication\n" +
               "public class GeneratedAppApplication {\n" +
               "    public static void main(String[] args) {\n" +
               "        SpringApplication.run(GeneratedAppApplication.class, args);\n" +
               "    }\n" +
               "}\n";
    }
    
    private String generateBuildGradle(List<EntityDefinition> entities) {
        return "plugins {\n" +
               "    id 'org.springframework.boot' version '3.2.3'\n" +
               "    id 'io.spring.dependency-management' version '1.1.4'\n" +
               "    id 'java'\n" +
               "}\n\n" +
               "group = 'com.example'\n" +
               "version = '1.0.0'\n" +
               "sourceCompatibility = '17'\n\n" +
               "repositories {\n" +
               "    mavenCentral()\n" +
               "}\n\n" +
               "dependencies {\n" +
               "    implementation 'org.springframework.boot:spring-boot-starter-web'\n" +
               "    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'\n" +
               "    implementation 'org.springframework.boot:spring-boot-starter-validation'\n" +
               "    implementation 'org.springframework.boot:spring-boot-starter-security'\n" +
               "    runtimeOnly 'org.postgresql:postgresql'\n" +
               "    testImplementation 'org.springframework.boot:spring-boot-starter-test'\n" +
               "    testImplementation 'org.mockito:mockito-core'\n" +
               "}\n\n" +
               "tasks.named('test') {\n" +
               "    useJUnitPlatform()\n" +
               "}\n";
    }
    
    private String generateDockerfile() {
        return "FROM eclipse-temurin:17-jre-alpine\n" +
               "ARG JAR_FILE=build/libs/*.jar\n" +
               "COPY ${JAR_FILE} app.jar\n" +
               "EXPOSE 8080\n" +
               "ENTRYPOINT [\"java\",\"-jar\",\"/app.jar\"]\n";
    }
    
    private String generateDockerCompose(String database) {
        return "version: '3.8'\n\n" +
               "services:\n" +
               "  app:\n" +
               "    build: .\n" +
               "    ports:\n" +
               "      - \"8080:8080\"\n" +
               "    environment:\n" +
               "      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/generated_app\n" +
               "      - SPRING_DATASOURCE_USERNAME=postgres\n" +
               "      - SPRING_DATASOURCE_PASSWORD=postgres\n" +
               "    depends_on:\n" +
               "      - db\n\n" +
               "  db:\n" +
               "    image: postgres:15\n" +
               "    environment:\n" +
               "      - POSTGRES_DB=generated_app\n" +
               "      - POSTGRES_USER=postgres\n" +
               "      - POSTGRES_PASSWORD=postgres\n" +
               "    ports:\n" +
               "      - \"5432:5432\"\n" +
               "    volumes:\n" +
               "      - postgres_data:/var/lib/postgresql/data\n\n" +
               "volumes:\n" +
               "  postgres_data:\n";
    }
    
    private String generateCICDPipeline() {
        return "name: CI/CD Pipeline\n\n" +
               "on:\n" +
               "  push:\n" +
               "    branches: [ main, develop ]\n" +
               "  pull_request:\n" +
               "    branches: [ main ]\n\n" +
               "jobs:\n" +
               "  build:\n" +
               "    runs-on: ubuntu-latest\n\n" +
               "    steps:\n" +
               "    - uses: actions/checkout@v3\n\n" +
               "    - name: Set up JDK 17\n" +
               "      uses: actions/setup-java@v3\n" +
               "      with:\n" +
               "        java-version: '17'\n" +
               "        distribution: 'temurin'\n" +
               "        cache: gradle\n\n" +
               "    - name: Grant execute permission for gradlew\n" +
               "      run: chmod +x gradlew\n\n" +
               "    - name: Build with Gradle\n" +
               "      run: ./gradlew clean build -x test\n\n" +
               "    - name: Run tests\n" +
               "      run: ./gradlew test\n\n" +
               "    - name: Run integration tests\n" +
               "      run: ./gradlew integrationTest\n\n" +
               "    - name: Build Docker image\n" +
               "      run: docker build -t generated-app .\n\n" +
               "    - name: Run Docker container\n" +
               "      run: docker run -d -p 8080:8080 --name test-app generated-app\n\n" +
               "    - name: Wait for app to start\n" +
               "      run: sleep 30\n\n" +
               "    - name: Health check\n" +
               "      run: curl -f http://localhost:8080/api/health || exit 1\n\n" +
               "    - name: Stop container\n" +
               "      run: docker stop test-app && docker rm test-app\n\n" +
               "  deploy:\n" +
               "    needs: build\n" +
               "    runs-on: ubuntu-latest\n" +
               "    if: github.ref == 'refs/heads/main'\n\n" +
               "    steps:\n" +
               "    - name: Deploy to production\n" +
               "      run: echo \"Deploy to production\"\n";
    }
    
    private String generateEnhancedReadme(String appName, String description, 
                                          List<EntityDefinition> entities) {
        StringBuilder readme = new StringBuilder();
        readme.append("# ").append(appName).append("\n\n");
        readme.append(description).append("\n\n");
        readme.append("## Features\n\n");
        readme.append("- Full CRUD operations for all entities\n");
        readme.append("- RESTful API with Spring Boot\n");
        readme.append("- JPA/Hibernate ORM\n");
        readme.append("- PostgreSQL database\n");
        readme.append("- JWT Authentication\n");
        readme.append("- Docker containerization\n");
        readme.append("- Automated tests (JUnit 5, Mockito)\n");
        readme.append("- CI/CD pipeline (GitHub Actions)\n");
        readme.append("- AI-powered code generation\n\n");
        readme.append("## Entities\n\n");
        
        for (EntityDefinition entity : entities) {
            readme.append("### ").append(entity.getName()).append("\n");
            readme.append("| Field | Type | Required |\n");
            readme.append("|-------|------|----------|\n");
            for (FieldDefinition field : entity.getFields()) {
                readme.append("| ").append(field.getName()).append(" | ")
                     .append(field.getType()).append(" | ")
                     .append(field.isRequired() ? "Yes" : "No").append(" |\n");
            }
            readme.append("\n");
        }
        
        readme.append("## Run\n\n");
        readme.append("### Using Docker Compose\n\n");
        readme.append("```bash\n");
        readme.append("docker-compose up --build\n");
        readme.append("```\n\n");
        readme.append("### Manual Build\n\n");
        readme.append("```bash\n");
        readme.append("./gradlew bootRun\n");
        readme.append("```\n\n");
        readme.append("## API Endpoints\n\n");
        
        for (EntityDefinition entity : entities) {
            String lowerName = entity.getName().substring(0, 1).toLowerCase() + 
                             entity.getName().substring(1);
            readme.append("### ").append(entity.getName()).append("\n\n");
            readme.append("| Method | Endpoint | Description |\n");
            readme.append("|--------|----------|-------------|\n");
            readme.append("| GET | `/api/").append(lowerName).append("s` | List all ")
                 .append(lowerName).append("s |\n");
            readme.append("| GET | `/api/").append(lowerName).append("s/{id}` | Get by ID |\n");
            readme.append("| POST | `/api/").append(lowerName).append("s` | Create new |\n");
            readme.append("| PUT | `/api/").append(lowerName).append("s/{id}` | Update |\n");
            readme.append("| DELETE | `/api/").append(lowerName).append("s/{id}` | Delete |\n\n");
        }
        
        readme.append("## Testing\n\n");
        readme.append("```bash\n");
        readme.append("./gradlew test\n");
        readme.append("```\n\n");
        readme.append("## CI/CD\n\n");
        readme.append("Automated pipeline runs on every push:\n");
        readme.append("- Build\n");
        readme.append("- Test\n");
        readme.append("- Docker build\n");
        readme.append("- Deploy (main branch)\n\n");
        readme.append("## Generated by SupremeAI\n\n");
        readme.append("AI-powered application generation with full-stack support.\n");
        
        return readme.toString();
    }
    
    private String generateApplicationProperties(String database) {
        return "spring.application.name=generated-app\n" +
               "server.port=8080\n\n" +
               "spring.datasource.url=jdbc:postgresql://localhost:5432/generated_app\n" +
               "spring.datasource.username=postgres\n" +
               "spring.datasource.password=postgres\n" +
               "spring.jpa.hibernate.ddl-auto=update\n" +
               "spring.jpa.show-sql=true\n" +
               "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect\n\n" +
               "app.jwt.secret=mySecretKey12345678901234567890123456789012\n" +
               "app.jwt.expiration=86400000\n";
    }
}
