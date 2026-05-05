# Multi-stage Docker build for SupremeAI Backend
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy gradle files
COPY gradle ./gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Download dependencies (cache layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source
COPY src ./src

# Debug: list files to verify they are present
RUN find src -maxdepth 3

# Build the application JAR
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create logs directory for logback
RUN mkdir -p logs && chmod 777 logs

# Copy the built JAR
COPY --from=builder /app/build/libs/app.jar app.jar

# Create non-root user
RUN groupadd -r supremeai && useradd -r -g supremeai supremeai
USER supremeai

ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=cloud
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
