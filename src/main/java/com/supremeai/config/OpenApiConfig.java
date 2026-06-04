package com.supremeai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI Configuration for SupremeAI. Enables automated API documentation at
 * /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI supremeAiOpenAPI() {
    final String securitySchemeName = "bearerAuth";
    return new OpenAPI()
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(
            new Components()
                .addSecuritySchemes(
                    securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .info(
            new Info()
                .title("SupremeAI Studio API")
                .description(
                    "Enterprise-Grade Multi-Agent AI Orchestration & Cloud App Development API")
                .version("v6.0.1")
                .contact(
                    new Contact()
                        .name("SupremeAI Support")
                        .email("support@supremeai.com")
                        .url("https://supremeai.com"))
                .license(new License().name("Apache 2.0").url("http://springdoc.org")));
  }
}
