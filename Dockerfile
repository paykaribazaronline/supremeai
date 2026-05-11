# Multi-stage Docker build for SupremeAI Backend
# Stage 1: Build
FROM gradle:8.7-jdk21 AS builder

WORKDIR /app

# Copy only the files needed for dependency resolution to leverage Docker cache
COPY gradle ./gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy source
COPY src ./src

# Build the application JAR
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create logs directory
RUN mkdir -p logs && chmod 755 logs

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/app.jar app.jar

# Create non-root user for security
RUN groupadd -r supremeai && useradd -r -g supremeai supremeai
RUN chown -R supremeai:supremeai /app
USER supremeai

# Environment variables for production
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=cloud
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication --enable-preview"

# Health check for Cloud Run / Kubernetes
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
