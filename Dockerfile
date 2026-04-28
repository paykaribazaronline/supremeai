# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:21-jre-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the pre-built JAR file from the repo root
COPY app.jar app.jar

# Create a non-root user for security
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
