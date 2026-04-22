# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:21-jre-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the pre-built JAR file from the repo root
COPY app.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the jar file with necessary exports for Gson/Java Time
ENTRYPOINT ["java", "--add-opens", "java.base/java.time.chrono=ALL-UNNAMED", "--add-opens", "java.base/java.time=ALL-UNNAMED", "-jar", "app.jar"]
