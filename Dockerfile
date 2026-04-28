# Multi-stage Docker build for SupremeAI Backend
# Stage 1: Build the JAR with Gradle
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle files first for cached dependencies
COPY gradle ./gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src ./src

# Build the application JAR (skip tests for faster build)
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

# Stage 2: Create runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/app.jar app.jar

# Create non-root user for security
RUN addgroup -g 1001 -S supremeai && adduser -u 1001 -S supremeai -G supremeai
USER supremeai

# Expose the port the app runs on
EXPOSE 8080

# Set environment variables for Cloud Run
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=cloud
ENV SERVER_PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx2g -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# Run the jar file with necessary opens for reflection
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS --add-opens java.base/java.time.chrono=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED -jar /app/app.jar"]
