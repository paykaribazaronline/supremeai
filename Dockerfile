# Stage 1: Extract layers from the FAT JAR
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /builder
COPY build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 2: Final Runtime Image
FROM eclipse-temurin:21-jre-alpine

# Minimal health-check tooling (single layer)
RUN apk add --no-cache curl ca-certificates

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy extracted layers in dependency order for optimal Docker layer caching
# Dependencies change rarely → cached longer
COPY --from=builder --chown=spring:spring /builder/dependencies/ ./
# Spring loader rarely changes
COPY --from=builder --chown=spring:spring /builder/spring-boot-loader/ ./
# Snapshot deps change occasionally
COPY --from=builder --chown=spring:spring /builder/snapshot-dependencies/ ./
# Application code changes most often → cached least
COPY --from=builder --chown=spring:spring /builder/application/ ./

# Ensure all files are owned by the spring user
RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=2 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Duser.timezone=UTC"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
