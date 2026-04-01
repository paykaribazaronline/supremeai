# Step 1: Use prebuilt JAR if available, otherwise build
FROM gradle:8.7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon -x test --parallel --max-workers=2

# Step 2: Create the runtime image using Eclipse Temurin (More stable)
FROM eclipse-temurin:17-jdk-jammy

# Install git (required for GitIntegrationService)
RUN apt-get update && apt-get install -y git curl && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the built jar from the build stage
# Note: Ensure the jar filename matches. If it has a version, use a wildcard.
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Set PORT environment variable (Cloud Run requirement)
# This overrides the default 8080 if PORT env var is set
ENV PORT=8080

# Expose the port Spring Boot runs on
EXPOSE 8080

# Add health check for Cloud Run
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Start the application with proper JVM settings for containers
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-XX:+UseG1GC", "-jar", "app.jar"]
