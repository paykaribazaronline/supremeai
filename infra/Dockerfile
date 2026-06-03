# STAGE 1: Build the JAR from source
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar -x test -x jacocoTestReport --no-daemon --stacktrace

# STAGE 2: Minimal runtime image
FROM eclipse-temurin:21-jre-jammy

# Light health-check tooling
RUN apt-get update && apt-get install -y --no-install-recommends \
      curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# JVM tuning for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
