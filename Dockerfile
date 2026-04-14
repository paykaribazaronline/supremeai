# Step 1: Use Gradle for building the JAR
FROM gradle:8.7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

# Set build memory limits and build the app
RUN printf 'org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m\norg.gradle.daemon=false\n' > gradle.properties \
    && chmod +x gradlew \
    && ./gradlew clean build --no-daemon -x test --parallel

# Find the built JAR and rename it to app.jar
RUN set -eux; \
    JAR_FILE=$(ls /home/gradle/src/build/libs/*.jar | grep -v -- '-plain\.jar$' | head -n 1); \
    cp "$JAR_FILE" /home/gradle/src/build/libs/app.jar

# Step 2: Create the runtime image using Eclipse Temurin
FROM eclipse-temurin:17-jdk-jammy

# Install git and curl for runtime needs
RUN apt-get update && apt-get install -y git curl ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /home/gradle/src/build/libs/app.jar app.jar

# Cloud Run injects the PORT environment variable.
# Spring Boot automatically picks up the PORT env var if server.port is configured correctly.
ENV PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Expose port (Documentation only)
EXPOSE 8080

# Start the application.
# We use $PORT to ensure it listens on the correct port assigned by Cloud Run.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
