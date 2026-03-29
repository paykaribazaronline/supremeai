# Step 1: Build the Java application using Gradle
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon -x test

# Step 2: Create the runtime image using Eclipse Temurin (More stable)
FROM eclipse-temurin:17-jdk-jammy

# Install git (required for GitIntegrationService)
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the built jar from the build stage
# Note: Ensure the jar filename matches. If it has a version, use a wildcard.
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Expose the port Spring Boot runs on
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
