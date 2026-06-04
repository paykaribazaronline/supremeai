package com.supremeai.generation;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

@Service
public class FullStackCodeGenerator {

    public Map<String, String> generateApp(String requirements, String platform) {
        Map<String, String> output = new HashMap<>();
        
        output.put("status", "success");
        output.put("platform", platform);
        output.put("backend", generateSpringBootBackend(requirements));
        output.put("frontend", generateReactFrontend(requirements));
        output.put("buildScript", generateBuildScript());
        output.put("dockerfile", generateDockerfile());
        output.put("readme", generateReadme());
        
        return output;
    }
    
    private String generateSpringBootBackend(String requirements) {
        return """
            package com.generated.app;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            import org.springframework.data.jpa.repository.JpaRepository;
            import org.springframework.stereotype.Repository;
            import org.springframework.stereotype.Service;
            import org.springframework.web.bind.annotation.*;
            import jakarta.persistence.*;
            import java.util.List;
            
            @SpringBootApplication
            public class Application {
                public static void main(String[] args) {
                    SpringApplication.run(Application.class, args);
                }
            }
            
            @Entity
            class Product {
                @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
                private Long id;
                private String name;
                private String description;
                private Double price;
                
                public Product() {}
                
                public Product(String name, String description, Double price) {
                    this.name = name;
                    this.description = description;
                    this.price = price;
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
            }
            
            @Repository
            interface ProductRepository extends JpaRepository<Product, Long> {}
            
            @Service
            class ProductService {
                private final ProductRepository repository;
                
                public ProductService(ProductRepository repository) {
                    this.repository = repository;
                }
                
                public List<Product> getAllProducts() {
                    return repository.findAll();
                }
                
                public Product createProduct(Product product) {
                    return repository.save(product);
                }
            }
            
            @RestController
            @RequestMapping("/api/products")
            class ProductController {
                private final ProductService service;
                
                public ProductController(ProductService service) {
                    this.service = service;
                }
                
                @GetMapping
                public List<Product> getAll() {
                    return service.getAllProducts();
                }
                
                @PostMapping
                public Product create(@RequestBody Product product) {
                    return service.createProduct(product);
                }
            }
            
            @RestController
            @RequestMapping("/api")
            class HealthController {
                @GetMapping("/health")
                public Map<String, String> health() {
                    return Map.of("status", "UP", "generated", "true");
                }
            }
            """;
    }
    
    private String generateReactFrontend(String requirements) {
        return """
            import React, { useState, useEffect } from 'react';
            import ReactDOM from 'react-dom/client';
            
            function App() {
              const [products, setProducts] = useState([]);
              const [name, setName] = useState('');
              const [description, setDescription] = useState('');
              const [price, setPrice] = useState('');
              
              useEffect(() => {
                fetch('/api/products')
                  .then(res => res.json())
                  .then(data => setProducts(data));
              }, []);
              
              const addProduct = () => {
                fetch('/api/products', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({ name, description, price: parseFloat(price) })
                })
                .then(res => res.json())
                .then(product => setProducts([...products, product]));
              };
              
              return (
                <div className="App">
                  <h1>Generated Full-Stack App</h1>
                  <p>Built by SupremeAI</p>
                  
                  <h2>Products</h2>
                  <ul>
                    {products.map(p => (
                      <li key={p.id}>{p.name} - ${p.price}</li>
                    ))}
                  </ul>
                  
                  <h2>Add Product</h2>
                  <input 
                    placeholder="Name" 
                    value={name}
                    onChange={e => setName(e.target.value)}
                  />
                  <input 
                    placeholder="Description" 
                    value={description}
                    onChange={e => setDescription(e.target.value)}
                  />
                  <input 
                    placeholder="Price" 
                    type="number"
                    value={price}
                    onChange={e => setPrice(e.target.value)}
                  />
                  <button onClick={addProduct}>Add</button>
                </div>
              );
            }
            
            const root = ReactDOM.createRoot(document.getElementById('root'));
            root.render(<App />);
            """;
    }
    
    private String generateBuildScript() {
        return """
            plugins {
                id 'org.springframework.boot' version '3.2.3'
                id 'io.spring.dependency-management' version '1.1.4'
                id 'java'
            }
            
            group = 'com.generated'
            version = '1.0.0'
            sourceCompatibility = '17'
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation 'org.springframework.boot:spring-boot-starter-web'
                implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
                runtimeOnly 'org.postgresql:postgresql'
                testImplementation 'org.springframework.boot:spring-boot-starter-test'
            }
            
            tasks.named('test') {
                useJUnitPlatform()
            }
            """;
    }
    
    private String generateDockerfile() {
        return """
            FROM eclipse-temurin:17-jre-alpine
            ARG JAR_FILE=build/libs/*.jar
            COPY ${JAR_FILE} app.jar
            EXPOSE 8080
            ENTRYPOINT ["java","-jar","/app.jar"]
            """;
    }
    
    private String generateReadme() {
        return """
            # Generated Full-Stack Application
            
            A complete Spring Boot backend with React frontend.
            
            ## Features
            
            - RESTful API with Spring Boot
            - React frontend with state management
            - PostgreSQL database with JPA
            - Docker support
            - CRUD operations
            
            ## Run
            
            ./gradlew bootRun
            
            ## API Endpoints
            
            - GET /api/products - List all products
            - POST /api/products - Create product
            - GET /api/health - Health check
            
            Generated by SupremeAI with full-stack support.
            """;
    }
}
