# Step 1: Use prebuilt JAR if available, otherwise build
FROM gradle:8.7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN chmod +x gradlew && ./gradlew build --no-daemon -x test --parallel --max-workers=2 --stacktrace
RUN set -eux; \
    JAR_FILE=$(ls /home/gradle/src/build/libs/*.jar | grep -v -- '-plain\.jar$' | head -n 1); \
    cp "$JAR_FILE" /home/gradle/src/build/libs/app.jar

# Step 2: Create the runtime image using Eclipse Temurin (More stable)
FROM eclipse-temurin:17-jdk-jammy

# Install git (required for GitIntegrationService)
RUN apt-get update && apt-get install -y git curl ca-certificates && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the single runtime jar from the build stage.
COPY --from=build /home/gradle/src/build/libs/app.jar app.jar

# Set PORT environment variable (Cloud Run requirement)
# This overrides the default 8080 if PORT env var is set
ENV PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:InitiatingHeapOccupancyPercent=35 -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Expose the port Spring Boot runs on
EXPOSE 8080

# Add health check for Cloud Run
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# Start the application with proper JVM settings for containers
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

