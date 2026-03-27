plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "3.0-Phase1"

repositories {
    mavenCentral()
    google()
}

dependencies {
    // Firebase Admin SDK (complete suite)
    implementation("com.google.firebase:firebase-admin:9.2.0")
    implementation("com.google.cloud:google-cloud-firestore:3.15.0")
    implementation("com.google.cloud:google-cloud-storage:2.34.0")
    
    // Authentication & Google Cloud - SECURITY
    implementation("com.google.auth:google-auth-library-oauth2-http:1.14.0")
    implementation("com.google.cloud:google-cloud-core:2.40.0")
    implementation("com.google.cloud:google-cloud-secretmanager:2.40.0")

    // HTTP Client for AI APIs
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")

    // Logging - STRUCTURED LOGGING
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("ch.qos.logback:logback-core:1.4.14")

    // Configuration Management - EXTERNALIZED CONFIG
    implementation("org.springframework.boot:spring-boot-configuration-processor:3.2.3")
    implementation("org.springframework.boot:spring-boot-starter-web:3.2.3")
    implementation("org.springframework.boot:spring-boot-starter-security:3.2.3")
    
    // Resilience & Error Handling
    implementation("io.github.resilience4j:resilience4j-core:2.1.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.1.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.1.0")

    // Rate Limiting
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")

    // Metrics & Monitoring
    implementation("io.micrometer:micrometer-core:1.12.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.3")

    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Testing - ENHANCED
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.3")
    testImplementation("org.springframework.security:spring-security-test:6.2.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("org.example.Main")
}

// Configure UTF-8 encoding for all compilation tasks
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "org.example.Main",
            "Implementation-Version" to project.version
        )
    }
}