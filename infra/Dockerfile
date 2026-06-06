# Minimal runtime image
FROM eclipse-temurin:21-jre-jammy

# Light health-check tooling
RUN apt-get update && apt-get install -y --no-install-recommends \
      curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the pre-built JAR from the build runner
# Gradle outputs to .gradle/build/libs (per build.gradle.kts:17)
COPY build/libs/*.jar app.jar

EXPOSE 8080

# JVM tuning for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
