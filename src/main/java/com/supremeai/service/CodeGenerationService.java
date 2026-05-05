package com.supremeai.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CodeGenerationService {

    /**
     * Generate a simple skeleton application based on flat spec (legacy).
     */
    public Map<String, Object> generate(Map<String, Object> spec) {
        return generateFromContext(Collections.emptyMap());
    }

    /**
     * Generate application from orchestration context (decisions: db, architecture, etc).
     * Context keys expected: "architecture", "database", "apiStyle", "authType", "frontend", "deployment"
     */
    public Map<String, Object> generateFromContext(Map<String, String> decisions) {
        String appName = "GeneratedApp";
        Map<String, String> files = new LinkedHashMap<>();

        // Derive tech stack from decisions (with defaults)
        String architecture = decisions.getOrDefault("architecture", "monolith");
        String database = decisions.getOrDefault("database", "PostgreSQL");
        String apiStyle = decisions.getOrDefault("apiStyle", "REST");
        String authType = decisions.getOrDefault("authType", "JWT");
        String frontend = decisions.getOrDefault("frontend", "React");
        String deployment = decisions.getOrDefault("deployment", "GCP");

        // Build dependencies based on decisions
        List<String> dependencies = new ArrayList<>();
        dependencies.add("implementation(\"org.springframework.boot:spring-boot-starter-web\")");
        dependencies.add("implementation(\"org.springframework.boot:spring-boot-starter-validation\")");
        dependencies.add("implementation(\"org.springframework.boot:spring-boot-starter-data-jpa\")");
        dependencies.add("implementation(\"org.springframework.boot:spring-boot-starter-security\")");
        
        // Database driver
        switch (database.toLowerCase()) {
            case "postgresql": 
                dependencies.add("runtimeOnly(\"org.postgresql:postgresql\")");
                break;
            case "mysql": 
                dependencies.add("runtimeOnly(\"com.mysql:mysql-connector-j\")");
                break;
            case "mongodb": 
                dependencies.add("implementation(\"org.springframework.boot:spring-boot-starter-data-mongodb\")");
                break;
        }

        // JWT auth
        if (authType.equalsIgnoreCase("JWT")) {
            dependencies.add("implementation(\"io.jsonwebtoken:jjwt-api:0.12.5\")");
            dependencies.add("runtimeOnly(\"io.jsonwebtoken:jjwt-impl:0.12.5\")");
            dependencies.add("runtimeOnly(\"io.jsonwebtoken:jjwt-jackson:0.12.5\")");
        }

        // Build.gradle
        String depsBlock = String.join(",\n                ", dependencies);
        String buildGradle = """
            plugins {
                id("org.springframework.boot") version "3.2.3"
                id("io.spring.dependency-management") version "1.1.4"
                java
            }
            group = "com.example"
            version = "1.0.0"
            java {
                sourceCompatibility = JavaVersion.VERSION_17
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                %s
            }
            tasks.getByName("test") {
                useJUnitPlatform()
            }
            """.formatted(depsBlock);
        files.put("build.gradle.kts", buildGradle);

        // Application class
        String appClass = """
            package com.example.generated;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            
            @SpringBootApplication
            public class GeneratedAppApplication {
                public static void main(String[] args) {
                    SpringApplication.run(GeneratedAppApplication.class, args);
                }
            }
            """;
        files.put("src/main/java/com/example/generated/GeneratedAppApplication.java", appClass);

        // Application Properties
        String appProps = """
            spring.application.name=generated-app
            server.port=8080
            
            # Database Configuration
            spring.datasource.url=jdbc:postgresql://localhost:5432/generated_app
            spring.datasource.username=postgres
            spring.datasource.password=postgres
            spring.jpa.hibernate.ddl-auto=update
            spring.jpa.show-sql=true
            spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
            
            # JWT Configuration
            app.jwt.secret=mySecretKey12345678901234567890123456789012
            app.jwt.expiration=86400000
            
            # File Upload
            spring.servlet.multipart.max-file-size=10MB
            spring.servlet.multipart.max-request-size=10MB
            """;
        files.put("src/main/resources/application.properties", appProps);

        // Entity - Product
        String productEntity = """
            package com.example.generated.entity;
            
            import jakarta.persistence.*;
            import java.time.LocalDateTime;
            
            @Entity
            @Table(name = "products")
            public class Product {
                @Id
                @GeneratedValue(strategy = GenerationType.IDENTITY)
                private Long id;
                
                @Column(nullable = false)
                private String name;
                
                @Column(length = 1000)
                private String description;
                
                @Column(nullable = false)
                private Double price;
                
                private Integer stock;
                
                private String category;
                
                @Column(name = "created_at")
                private LocalDateTime createdAt;
                
                @Column(name = "updated_at")
                private LocalDateTime updatedAt;
                
                @PrePersist
                protected void onCreate() {
                    createdAt = LocalDateTime.now();
                    updatedAt = LocalDateTime.now();
                }
                
                @PreUpdate
                protected void onUpdate() {
                    updatedAt = LocalDateTime.now();
                }
                
                // Constructors
                public Product() {}
                
                public Product(String name, String description, Double price, Integer stock, String category) {
                    this.name = name;
                    this.description = description;
                    this.price = price;
                    this.stock = stock;
                    this.category = category;
                }
                
                // Getters and Setters
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
                
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                
                public String getDescription() { return description; }
                public void setDescription(String description) { this.description = description; }
                
                public Double getPrice() { return price; }
                public void setPrice(Double price) { this.price = price; }
                
                public Integer getStock() { return stock; }
                public void setStock(Integer stock) { this.stock = stock; }
                
                public String getCategory() { return category; }
                public void setCategory(String category) { this.category = category; }
                
                public LocalDateTime getCreatedAt() { return createdAt; }
                public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
                
                public LocalDateTime getUpdatedAt() { return updatedAt; }
                public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
            }
            """;
        files.put("src/main/java/com/example/generated/entity/Product.java", productEntity);

        // Repository
        String productRepository = """
            package com.example.generated.repository;
            
            import com.example.generated.entity.Product;
            import org.springframework.data.jpa.repository.JpaRepository;
            import org.springframework.stereotype.Repository;
            import java.util.List;
            
            @Repository
            public interface ProductRepository extends JpaRepository<Product, Long> {
                List<Product> findByCategory(String category);
                List<Product> findByNameContainingIgnoreCase(String name);
                List<Product> findByPriceBetween(Double minPrice, Double maxPrice);
            }
            """;
        files.put("src/main/java/com/example/generated/repository/ProductRepository.java", productRepository);

        // DTO
        String productDto = """
            package com.example.generated.dto;
            
            public class ProductDto {
                private Long id;
                private String name;
                private String description;
                private Double price;
                private Integer stock;
                private String category;
                
                public ProductDto() {}
                
                public ProductDto(Long id, String name, String description, Double price, Integer stock, String category) {
                    this.id = id;
                    this.name = name;
                    this.description = description;
                    this.price = price;
                    this.stock = stock;
                    this.category = category;
                }
                
                // Getters and Setters
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
                
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                
                public String getDescription() { return description; }
                public void setDescription(String description) { this.description = description; }
                
                public Double getPrice() { return price; }
                public void setPrice(Double price) { this.price = price; }
                
                public Integer getStock() { return stock; }
                public void setStock(Integer stock) { this.stock = stock; }
                
                public String getCategory() { return category; }
                public void setCategory(String category) { this.category = category; }
            }
            """;
        files.put("src/main/java/com/example/generated/dto/ProductDto.java", productDto);

        // Service
        String productService = """
            package com.example.generated.service;
            
            import com.example.generated.dto.ProductDto;
            import com.example.generated.entity.Product;
            import com.example.generated.repository.ProductRepository;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Service;
            import org.springframework.transaction.annotation.Transactional;
            
            import java.util.List;
            import java.util.Optional;
            import java.util.stream.Collectors;
            
            @Service
            @Transactional
            public class ProductService {
                
                @Autowired
                private ProductRepository productRepository;
                
                public List<ProductDto> getAllProducts() {
                    return productRepository.findAll().stream()
                            .map(this::convertToDto)
                            .collect(Collectors.toList());
                }
                
                public Optional<ProductDto> getProductById(Long id) {
                    return productRepository.findById(id)
                            .map(this::convertToDto);
                }
                
                public List<ProductDto> getProductsByCategory(String category) {
                    return productRepository.findByCategory(category).stream()
                            .map(this::convertToDto)
                            .collect(Collectors.toList());
                }
                
                public ProductDto createProduct(ProductDto productDto) {
                    Product product = new Product();
                    product.setName(productDto.getName());
                    product.setDescription(productDto.getDescription());
                    product.setPrice(productDto.getPrice());
                    product.setStock(productDto.getStock());
                    product.setCategory(productDto.getCategory());
                    
                    Product saved = productRepository.save(product);
                    return convertToDto(saved);
                }
                
                public Optional<ProductDto> updateProduct(Long id, ProductDto productDto) {
                    return productRepository.findById(id).map(product -> {
                        product.setName(productDto.getName());
                        product.setDescription(productDto.getDescription());
                        product.setPrice(productDto.getPrice());
                        product.setStock(productDto.getStock());
                        product.setCategory(productDto.getCategory());
                        
                        Product updated = productRepository.save(product);
                        return convertToDto(updated);
                    });
                }
                
                public void deleteProduct(Long id) {
                    productRepository.deleteById(id);
                }
                
                private ProductDto convertToDto(Product product) {
                    return new ProductDto(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getStock(),
                        product.getCategory()
                    );
                }
            }
            """;
        files.put("src/main/java/com/example/generated/service/ProductService.java", productService);

        // Controller
        String productController = """
            package com.example.generated.controller;
            
            import com.example.generated.dto.ProductDto;
            import com.example.generated.service.ProductService;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.http.HttpStatus;
            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.*;
            
            import java.util.List;
            import java.util.Optional;
            
            @RestController
            @RequestMapping("/api/products")
            public class ProductController {
                
                @Autowired
                private ProductService productService;
                
                @GetMapping
                public ResponseEntity<List<ProductDto>> getAllProducts() {
                    return ResponseEntity.ok(productService.getAllProducts());
                }
                
                @GetMapping("/{id}")
                public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
                    Optional<ProductDto> product = productService.getProductById(id);
                    return product.map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
                }
                
                @GetMapping("/category/{category}")
                public ResponseEntity<List<ProductDto>> getProductsByCategory(@PathVariable String category) {
                    return ResponseEntity.ok(productService.getProductsByCategory(category));
                }
                
                @PostMapping
                public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) {
                    ProductDto created = productService.createProduct(productDto);
                    return ResponseEntity.status(HttpStatus.CREATED).body(created);
                }
                
                @PutMapping("/{id}")
                public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto) {
                    Optional<ProductDto> updated = productService.updateProduct(id, productDto);
                    return updated.map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
                }
                
                @DeleteMapping("/{id}")
                public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
                    productService.deleteProduct(id);
                    return ResponseEntity.noContent().build();
                }
            }
            """;
        files.put("src/main/java/com/example/generated/controller/ProductController.java", productController);

        // Health controller
        String healthCtrl = """
            package com.example.generated.controller;
            
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;
            
            import java.util.Map;
            
            @RestController
            @RequestMapping("/api")
            public class HealthController {
                @GetMapping("/health")
                public Map<String, String> health() {
                    return Map.of("status", "UP", "database", "%s", "architecture", "%s");
                }
                
                @GetMapping("/info")
                public Map<String, String> info() {
                    return Map.of("app", "GeneratedApp", "version", "1.0.0");
                }
            }
            """.formatted(database, architecture);
        files.put("src/main/java/com/example/generated/controller/HealthController.java", healthCtrl);

        // Dockerfile
        String dockerfile = """
            FROM eclipse-temurin:17-jre-alpine
            ARG JAR_FILE=build/libs/*.jar
            COPY ${JAR_FILE} app.jar
            ENTRYPOINT ["java","-jar","/app.jar"]
            """;
        files.put("Dockerfile", dockerfile);

        // README
        String readme = """
            # Generated Application
            
            Architecture: %s
            Database: %s
            API Style: %s
            Auth: %s
            Frontend: %s
            Deployment: %s
            
            ## Features
            
            - Full CRUD operations for Products
            - RESTful API with Spring Boot
            - JPA/Hibernate ORM
            - PostgreSQL database
            - JWT Authentication
            - Docker support
            
            ## Run
            
            ./gradlew bootRun
            
            ## API Endpoints
            
            ### Products
            - GET /api/products - List all products
            - GET /api/products/{id} - Get product by ID
            - GET /api/products/category/{category} - Get products by category
            - POST /api/products - Create new product
            - PUT /api/products/{id} - Update product
            - DELETE /api/products/{id} - Delete product
            
            ### Health
            - GET /api/health - Health check
            - GET /api/info - App info
            
            Generated by SupremeAI with full CRUD support.
            """.formatted(architecture, database, apiStyle, authType, frontend, deployment);
        files.put("README.md", readme);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appName", appName);
        result.put("files", files);
        result.put("fileCount", files.size());
        result.put("status", "GENERATED");
        result.put("decisions", decisions);
        result.put("message", "App generated with full CRUD support (Sprint 3)");
        return result;
    }
}
