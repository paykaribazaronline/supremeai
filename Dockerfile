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
USER spring:spring

WORKDIR /app

# Copy extracted layers from builder for better caching and faster I/O
COPY --from=builder --chown=spring:spring /builder/dependencies/ ./
COPY --from=builder --chown=spring:spring /builder/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /builder/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /builder/application/ ./

# Generate AppCDS archive to significantly reduce class-loading time at startup
# We use a dummy profile to avoid needing a DB connection during build
RUN java -XX:ArchiveClassesAtExit=application.jsa -Dspring.profiles.active=none -Dspring.main.lazy-initialization=true -Dspring.context.exit=onRefresh org.springframework.boot.loader.launch.JarLauncher

EXPOSE 8080

# JVM tuning for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -XX:SharedArchiveFile=application.jsa -Djava.security.egd=file:/dev/./urandom -Duser.timezone=UTC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
