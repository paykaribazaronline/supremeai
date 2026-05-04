plugins {
    id("java")
    // Trigger CI/CD Pipeline
    id("application")
    id("jacoco")
    id("org.springframework.boot") version "3.3.4"
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
        mavenBom("com.google.cloud:spring-cloud-gcp-dependencies:5.1.2")
    }
}

dependencies {
    // Firebase Admin SDK (complete suite)
    implementation("com.google.firebase:firebase-admin:9.2.0")
    // Firestore client provided by spring-cloud-gcp-starter-data-firestore (version managed by BOM)
    implementation("com.google.cloud:google-cloud-storage")

    // Authentication & Google Cloud - SECURITY
    implementation("com.google.auth:google-auth-library-oauth2-http:1.14.0")
    implementation("com.google.cloud:google-cloud-core")
    implementation("com.google.cloud:google-cloud-secretmanager")
    implementation("com.google.cloud:google-cloud-bigquery")
    implementation("software.amazon.awssdk:secretsmanager:2.25.36")
    implementation("software.amazon.awssdk:regions:2.25.36")
    implementation("com.azure:azure-identity:1.12.2")
    implementation("com.azure:azure-security-keyvault-secrets:4.8.2")

    // HTTP Client for AI APIs
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON Processing - all three Jackson artifacts must be same version to avoid NoSuchMethodError
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    // Jackson Afterburner module for optimized JSON serialization (20-30% faster)
    implementation("com.fasterxml.jackson.module:jackson-module-afterburner:2.17.0")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.17.1")

    // Logging - STRUCTURED LOGGING
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.8")
    implementation("ch.qos.logback:logback-core:1.5.8")

    // JWT Authentication
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // Configuration Management - EXTERNALIZED CONFIG
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
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
    implementation("io.github.resilience4j:resilience4j-core:2.1.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.1.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.1.0")

    // Rate Limiting
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")

    // AOP - required for @Aspect (KingModeAuditAspect)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Metrics & Monitoring
    implementation("io.micrometer:micrometer-core:1.12.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.3")

    // Distributed Tracing - OpenTelemetry (using stable versions)
    implementation("io.opentelemetry:opentelemetry-api:1.36.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.36.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.36.0")
    
    // Redis caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.lettuce:lettuce-core:6.3.0.RELEASE")
    implementation("org.apache.commons:commons-pool2:2.12.0")
    // Removed jedis to avoid dependency conflict and reduce footprint

    // Database Connection Pooling
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Lombok - Annotation Processing
    // Lombok - annotation processing for getters/builders
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")

    // Testing - ENHANCED
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
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
