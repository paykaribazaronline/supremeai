package com.supremeai;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@Profile("test-cloud")
public class CloudModelTest {



    @Bean
    public CommandLineRunner testRunner() {AIProviderFactorypublic CloudModelTest(AIProviderFactory factory) {
AIProviderFactory    this.factory = factory;
AIProviderFactory}

        return args -> {
            System.out.println("🚀 Starting Cloud Native Model Integration Test...");
            
            String[] models = {"gcp_qwen", "gcp_llama", "gcp_phi", "hf_deepseek"};
            
            for (String modelName : models) {
                try {
                    System.out.println("\n--- Testing Model: " + modelName + " ---");
                    AIProvider provider = factory.getProvider(modelName);
                    String response = provider.generate("Write a one-line greeting.").block();
                    System.out.println("✅ Response from " + modelName + ": " + response);
                } catch (Exception e) {
                    System.err.println("❌ Failed to test " + modelName + ": " + e.getMessage());
                }
            }
            
            System.out.println("\n🎯 Integration Test Complete.");
            System.exit(0);
        };
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CloudModelTest.class);
        app.setAdditionalProfiles("test-cloud");
        app.run(args);
    }
}
