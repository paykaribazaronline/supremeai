plugins {
    id("java")
    // Trigger CI/CD Pipeline
    id("application")
    id("jacoco")
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.supremeai"
version = "6.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    google()
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
    implementation("com.google.cloud:google-cloud-firestore:3.15.0")
    implementation("com.google.cloud:google-cloud-storage:2.34.0")

    // Authentication & Google Cloud - SECURITY
    implementation("com.google.auth:google-auth-library-oauth2-http:1.14.0")
    implementation("com.google.cloud:google-cloud-core:2.40.0")
    implementation("com.google.cloud:google-cloud-secretmanager:2.40.0")
    implementation("com.google.cloud:google-cloud-bigquery:2.31.0")
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

    // Logging - STRUCTURED LOGGING
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("ch.qos.logback:logback-core:1.4.14")

    // JWT Authentication
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // Configuration Management - EXTERNALIZED CONFIG
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Removed spring-boot-starter-webflux - conflicts with servlet stack causing non-web startup
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
    // Note: opentelemetry-exporter-jaeger moved, using basic exporter instead
    // implementation("io.opentelemetry:opentelemetry-exporter-jaeger:1.36.0")
    
    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Lombok - Annotation Processing
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

    // Testing - ENHANCED
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
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

tasks.test {
    useJUnitPlatform()
    // Enable parallel test execution
    maxParallelForks = Runtime.getRuntime().availableProcessors().div(2).coerceAtLeast(1)
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
