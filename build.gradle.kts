import java.math.RoundingMode

plugins {
    id("java")
    // Trigger CI/CD Pipeline
    id("application")
    id("jacoco")
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.freefair.lombok") version "8.10"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "com.supremeai"
version = "6.0.1"

rootProject.layout.buildDirectory.set(file("$rootDir/.gradle/build"))

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
    maven {
        url = uri("https://repo.spring.io/milestone")
    }
}

dependencyManagement {
    imports {
        // Spring Cloud GCP BOM for version alignment
        mavenBom("com.google.cloud:spring-cloud-gcp-dependencies:5.1.2")
        // Spring AI BOM
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0-M1")
    }
}

dependencies {
    // Firebase Admin SDK (complete suite)
    implementation("com.google.firebase:firebase-admin:9.2.0")
    // Firestore client provided by spring-cloud-gcp-starter-data-firestore (version managed by BOM)
    // Note: google-cloud-storage removed — uploads use Telegram/Teldrive; no GCS object storage in codebase
    implementation("com.google.code.gson:gson:2.11.0")

    // Authentication & Google Cloud - SECURITY
    implementation("com.google.auth:google-auth-library-oauth2-http:1.14.0")
    implementation("com.google.cloud:google-cloud-core")
    implementation("com.google.cloud:google-cloud-secretmanager")
    implementation("com.google.cloud:google-cloud-bigquery")
    implementation("com.google.cloud:google-cloud-run:0.43.0")

    // Removed software.amazon.awssdk and com.azure to reduce JAR size (~40MB saving)
    // Consolidated cloud secrets to Google Cloud Secret Manager

    // HTTP Client for AI APIs
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Spring AI
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")

    // JSON Processing - Consolidated Jackson artifacts
    val jacksonVersion = "2.18.1"
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-afterburner:$jacksonVersion")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("com.microsoft.playwright:playwright:1.40.0")

    // Logging - STRUCTURED LOGGING
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("ch.qos.logback:logback-core:1.5.12")

    // JWT Authentication
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Configuration Management - EXTERNALIZED CONFIG
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring Cloud GCP - Firestore & Secret Manager
    implementation("com.google.cloud:spring-cloud-gcp-starter-data-firestore")
    implementation("com.google.cloud:google-cloud-secretmanager")
    implementation("com.google.cloud:google-cloud-firestore")
    implementation("io.projectreactor:reactor-core:3.5.1")

    // Google Cloud Pub/Sub client
    implementation("com.google.cloud:google-cloud-pubsub")

    // Database
    implementation("com.h2database:h2") // Required for runtime if DATABASE_URL defaults to H2
    runtimeOnly("org.postgresql:postgresql:42.7.3")

    // Resilience & Error Handling
    implementation("io.github.resilience4j:resilience4j-core:2.1.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.1.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.1.0")
    implementation("io.github.resilience4j:resilience4j-reactor:2.1.0")

    // Rate Limiting
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")

    // AOP
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Metrics & Monitoring
    implementation("io.micrometer:micrometer-core:1.12.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.3")

    // API Documentation - SpringDoc OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Error Tracking - Sentry
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.14.0")
    implementation("io.sentry:sentry-logback:7.14.0")

    // Distributed Tracing - OpenTelemetry
    implementation("io.opentelemetry:opentelemetry-api:1.36.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.36.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.36.0")
    implementation("com.google.cloud:google-cloud-logging-logback")
    
    // Redis caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.apache.commons:commons-pool2:2.12.0")

    // Database Connection Pooling
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Lombok handled by plugin, but added directly for strict annotation processing
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("jakarta.validation:jakarta.validation-api")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("com.google.cloud:google-cloud-bigquery")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

// Configure UTF-8 encoding for all compilation tasks with performance optimizations
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("--enable-preview"))
}

tasks.withType<Test> {
    // Enable preview features for test execution
    jvmArgs("--enable-preview")
    // Java 21+ module access — required by Mockito / reflection in Spring Boot context
    jvmArgs("--add-opens", "java.base/java.time.chrono=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    useJUnitPlatform()
}

tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.addAll(listOf("--enable-preview"))
}

tasks.withType<JavaExec> {
    jvmArgs(
        "--enable-preview",
        "--add-opens", "java.base/java.time.chrono=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED"
    )
}

tasks.bootRun {
    systemProperty("spring.profiles.active", "local")
    environment("FIRESTORE_EMULATOR_HOST", "127.0.0.1:8081")
    environment("FIREBASE_AUTH_EMULATOR_HOST", "127.0.0.1:9099")
    environment("LOCAL_JWT_SECRET", "this_is_a_32_char_very_secret_local_key_12345")
}

tasks.test {
    useJUnitPlatform()
    environment("FIRESTORE_EMULATOR_HOST", "localhost:8081")
    environment("FIREBASE_AUTH_EMULATOR_HOST", "localhost:9099")
    systemProperty("spring.profiles.active", "test")
    
    maxParallelForks = (findProperty("test.maxParallelForks") as String?)?.toIntOrNull()?.coerceAtLeast(1) 
        ?: Runtime.getRuntime().availableProcessors().coerceAtMost(4)
        
    systemProperties["junit.jupiter.execution.parallel.enabled"] = "true"
    systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
    systemProperties["junit.jupiter.execution.parallel.config.strategy"] = "dynamic"

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
    dependsOn(tasks.jacocoTestReport)
    val minLineCoverage =
        (findProperty("jacoco.line.minimum") as String?)?.toBigDecimalOrNull()
            ?: "0.00".toBigDecimal()
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

// Auto-ratchet: after successful coverage verification, update gradle.properties
// with the actual coverage so it can only stay equal or increase
tasks.register("jacocoRatchet") {
    group = "verification"
    description = "Auto-updates jacoco.line.minimum in gradle.properties after successful tests"
    dependsOn(tasks.jacocoTestCoverageVerification)

    doLast {
        val xmlReport = file("${layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml")
        if (!xmlReport.exists()) {
            logger.warn("Jacoco XML report not found, skipping ratchet update")
            return@doLast
        }

        // Parse XML to extract line coverage ratio
        val xmlText = xmlReport.readText()
        // Find the last (bundle-level) LINE counter: <counter type="LINE" missed="X" covered="Y"/>
        val lineCounterRegex = Regex("""<counter type="LINE" missed="(\d+)" covered="(\d+)"/>""")
        val matches = lineCounterRegex.findAll(xmlText).toList()
        if (matches.isEmpty()) {
            logger.warn("No LINE counter found in Jacoco report, skipping ratchet update")
            return@doLast
        }
        // Last match is the bundle-level summary
        val lastMatch = matches.last()
        val missed = lastMatch.groupValues[1].toBigDecimal()
        val covered = lastMatch.groupValues[2].toBigDecimal()
        val total = missed + covered
        if (total == BigDecimal.ZERO) {
            logger.warn("No lines found in Jacoco report, skipping ratchet update")
            return@doLast
        }
        val currentRatio = covered.divide(total, 2, RoundingMode.DOWN)

        // Read existing minimum
        val oldMinimum = (findProperty("jacoco.line.minimum") as String?)?.toBigDecimalOrNull()
            ?: BigDecimal.ZERO

        if (currentRatio >= oldMinimum) {
            // Update gradle.properties with new ratchet value
            val propsFile = file("gradle.properties")
            val lines = propsFile.readLines().toMutableList()
            val propKey = "jacoco.line.minimum"
            val newValue = currentRatio.toPlainString()
            var found = false
            for (i in lines.indices) {
                if (lines[i].startsWith("$propKey=")) {
                    lines[i] = "$propKey=$newValue"
                    found = true
                    break
                }
            }
            if (!found) {
                lines.add("")
                lines.add("# Jacoco coverage ratchet — auto-updated, must equal or exceed last successful push")
                lines.add("$propKey=$newValue")
            }
            propsFile.writeText(lines.joinToString("\n") + "\n")
            logger.lifecycle("✅ Coverage ratchet updated: $oldMinimum → $newValue (covered=$covered, missed=$missed)")
        } else {
            logger.lifecycle("Coverage $currentRatio is below ratchet $oldMinimum — gradle.properties NOT updated")
        }
    }
}

// Wire ratchet into coverage flow
tasks.jacocoTestCoverageVerification {
    finalizedBy("jacocoRatchet")
}

application {
    mainClass.set("com.supremeai.Application")
}

tasks.bootJar {
    archiveFileName.set("app.jar")
    layered {
        enabled = true
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true
}

spotless {
    java {
        googleJavaFormat("1.22.0")
        target("src/**/*.java")
    }
    format("gradle", {
        target("*.gradle", "*.gradle.kts")
    })
}

// Verify Spotless plugin available
tasks.named("spotlessCheck")

tasks.jar {
    enabled = false
}

// Add build performance optimizations
tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    // Exclude unnecessary files to reduce JAR size
    excludes.addAll(listOf(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA"
    ))

    // Optimize compression
    entryCompression = org.gradle.api.tasks.bundling.ZipEntryCompression.DEFLATED
}

// Test configuration is unified above in tasks.test
