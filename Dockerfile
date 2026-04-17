# Step 1: Use Gradle for building the JAR
FROM gradle:8.7-jdk21 AS build
WORKDIR /home/gradle/src

# Copy build metadata first so dependency layers can be reused across source-only changes.
COPY --chown=gradle:gradle gradle gradle
COPY --chown=gradle:gradle gradlew gradlew
COPY --chown=gradle:gradle settings.gradle.kts settings.gradle.kts
COPY --chown=gradle:gradle build.gradle.kts build.gradle.kts
COPY --chown=gradle:gradle gradle.properties gradle.properties
COPY --chown=gradle:gradle supremeai-intellij-plugin/build.gradle.kts supremeai-intellij-plugin/build.gradle.kts
COPY --chown=gradle:gradle supremeai-intellij-plugin/settings.gradle.kts supremeai-intellij-plugin/settings.gradle.kts

RUN chmod +x gradlew \
    && ./gradlew dependencies --no-daemon > /tmp/gradle-dependencies.log

COPY --chown=gradle:gradle src src
COPY --chown=gradle:gradle supremeai-intellij-plugin supremeai-intellij-plugin

# Set build memory limits and build the app
RUN printf 'org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m\norg.gradle.daemon=false\n' > gradle.properties \
    && ./gradlew clean build --no-daemon -x test --parallel

# Find the built JAR and rename it to app.jar
RUN set -eux; \
    JAR_FILE=$(ls /home/gradle/src/build/libs/*.jar | grep -v -- '-plain\.jar$' | head -n 1); \
    cp "$JAR_FILE" /home/gradle/src/build/libs/app.jar

# Step 2: Create the runtime image using Eclipse Temurin
FROM eclipse-temurin:21-jdk-jammy

# Install git and curl for runtime needs
RUN apt-get update && apt-get install -y git curl ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /home/gradle/src/build/libs/app.jar app.jar

# Expose port (Documentation only)
EXPOSE 8080

# Start the application.
# Cloud Run automatically sets the PORT environment variable.
ENTRYPOINT ["java", "-jar", "app.jar"]
