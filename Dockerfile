# Step 1: Build the Java application
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon -x test

# Step 2: Create the runtime image
FROM openjdk:17-jdk-slim

# Install git (required for GitIntegrationService)
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Expose the port Spring Boot runs on
EXPOSE 8080

# Environment variables (to be provided by Cloud Provider)
# ENV FIREBASE_SERVICE_ACCOUNT_JSON=""
# ENV TAVILY_API_KEY=""
# ENV OPENAI_API_KEY=""

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
