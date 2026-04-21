# Use a slim JRE for the runtime - this makes the image much smaller
FROM eclipse-temurin:21-jre-jammy

# Install runtime dependencies including curl for health checks
RUN apt-get update && apt-get install -y curl ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# The JAR is copied from the build context (uploaded by GitHub Actions)
# Only copy app.jar to avoid issues with multiple JARs
COPY build/libs/app.jar app.jar

# Cloud Run sets the PORT env var
ENV PORT=8080
EXPOSE 8080

# Optimized JVM settings for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Health check with timeout and retry for robustness
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f --connect-timeout 3 --max-time 10 http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
