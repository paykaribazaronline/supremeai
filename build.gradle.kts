plugins {
    id("java")
    // Trigger CI/CD Pipeline
    id("application")
    id("jacoco")
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.supremeai"
version = "6.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven {
        name = "GoogleMavenCentralMirror"
        url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
    }
    maven {
        name = "MavenCentralMirror"
        url = uri("https://repo1.maven.org/maven2/")
    }
}

dependencyManagement {
    imports {
        // Spring Cloud GCP BOM for version alignment
        mavenBom("com.google.cloud:spring-cloud-gcp-dependencies:8.0.2")
    }
}

dependencies {
    // Firebase Admin SDK (complete suite)
    implementation("com.google.firebase:firebase-admin:9.8.0")
    // Firestore client provided by spring-cloud-gcp-starter-data-firestore (version managed by BOM)
    implementation("com.google.cloud:google-cloud-storage")

    // Authentication & Google Cloud - SECURITY
    implementation("com.google.auth:google-auth-library-oauth2-http:1.46.0")
    implementation("com.google.cloud:google-cloud-core")
    implementation("com.google.cloud:google-cloud-secretmanager")
    implementation("com.google.cloud:google-cloud-bigquery")
    implementation("software.amazon.awssdk:secretsmanager:2.44.0")
    implementation("software.amazon.awssdk:regions:2.44.0")
    implementation("com.azure:azure-identity:1.18.3")
    implementation("com.azure:azure-security-keyvault-secrets:4.10.6")

    // HTTP Client for AI APIs
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")

    // JSON Processing - all three Jackson artifacts must be same version to avoid NoSuchMethodError
    implementation("com.fasterxml.jackson.core:jackson-core:2.21.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.21.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.3")
    // Jackson Afterburner module for optimized JSON serialization (20-30% faster)
    implementation("com.fasterxml.jackson.module:jackson-module-afterburner:2.21.3")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.22.2")

    // Logging - STRUCTURED LOGGING
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("ch.qos.logback:logback-core:1.5.32")

    // JWT Authentication
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // Configuration Management - EXTERNALIZED CONFIG
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")  // Required for WebClient in SimulatorDeploymentService
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Removed spring-boot-starter-data-jpa to avoid conflict with Firestore reactive repositories

    // Spring Cloud GCP - Firestore
    implementation("com.google.cloud:spring-cloud-gcp-starter-data-firestore")

    // Database
    runtimeOnly("com.h2database:h2")

    // Resilience & Error Handling
    implementation("io.github.resilience4j:resilience4j-core:2.4.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.4.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.4.0")

    // Rate Limiting
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:8.0.1")

    // AOP - required for @Aspect (KingModeAuditAspect)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Metrics & Monitoring
    implementation("io.micrometer:micrometer-core:1.16.5")
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.5")

    // Distributed Tracing - OpenTelemetry (using stable versions)
    implementation("io.opentelemetry:opentelemetry-api:1.61.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.61.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.61.0")
    
    // Redis caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.lettuce:lettuce-core:7.5.1.RELEASE")
    implementation("org.apache.commons:commons-pool2:2.13.1")
    // Removed jedis to avoid dependency conflict and reduce footprint

    // Database Connection Pooling
    implementation("com.zaxxer:HikariCP:7.0.2")

    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.4")

    // Lombok - Annotation Processing
    // Lombok - annotation processing for getters/builders
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    testCompileOnly("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")

    // Testing - ENHANCED
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Configure UTF-8 encoding for all compilation tasks
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-Xlint:deprecation",      // Enable deprecation warnings
        "-Xlint:unchecked"         // Enable unchecked warnings
    ))
}

tasks.withType<JavaExec> {
    jvmArgs(
        "--add-opens", "java.base/java.time.chrono=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED"
    )
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks =
        (findProperty("test.maxParallelForks") as String?)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    // Run coverage only if explicitly requested
    val runCoverage = (findProperty("runCoverage") as String?)?.toBoolean() ?: false
    if (runCoverage) {
        finalizedBy(tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification)
    }
}

val jacocoExclusions = listOf(
    "**/model/**",
    "**/config/**",
    "**/exception/**",
    "**/dto/**",
    "**/*Application*",
    "**/*Configuration*",
    "**/*Config*",
    "**/*Properties*",
    "**/*Exception*",
    "**/*Controller*",
    "**/*Aspect*"
)

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(jacocoExclusions)
            }
        })
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    val minLineCoverage =
        (findProperty("jacoco.line.minimum") as String?)?.toBigDecimalOrNull()
            ?: "0.10".toBigDecimal()
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(jacocoExclusions)
            }
        })
    )
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = minLineCoverage
            }
        }
    }
}

application {
    mainClass.set("com.supremeai.Application")
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}

tasks.jar {
    enabled = false
}
