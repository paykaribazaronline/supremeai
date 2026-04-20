# Use a slim JRE for the runtime - this makes the image much smaller
FROM eclipse-temurin:21-jre-jammy

# Install runtime dependencies
RUN apt-get update && apt-get install -y curl ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# The JAR is copied from the build context (uploaded by GitHub Actions)
COPY build/libs/app.jar app.jar

# Cloud Run sets the PORT env var
ENV PORT=8080
EXPOSE 8080

# Optimized JVM settings for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
