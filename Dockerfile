# Stage 1: Extract layers from the FAT JAR
FROM eclipse-temurin:21-jre-jammy AS builder
WORKDIR /builder
COPY app.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 2: Final Runtime Image
FROM eclipse-temurin:21-jre-jammy

# Light health-check tooling
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# Copy extracted layers from builder for better caching and faster I/O
COPY --from=builder --chown=spring:spring /builder/dependencies/ ./
COPY --from=builder --chown=spring:spring /builder/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /builder/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /builder/application/ ./

# Ensure all files are owned by the spring user
RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

# JVM tuning for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Duser.timezone=UTC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
