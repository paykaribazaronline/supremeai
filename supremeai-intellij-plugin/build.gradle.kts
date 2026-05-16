plugins {
    java
    org.jetbrains.kotlin.jvm version "2.1.10"
    org.jetbrains.intellij.platform version "2.2.0"
   kotlin("plugin.serialization") version "2.1.10"
}

group = "com.supremeai"
version = "1.2.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.google.com")
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    intellijPlatform {
        defaultRepositories()
        google()
        mavenCentral()
    }
}



dependencies {
    intellijPlatform {
        androidStudio("2024.2.1.11")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
    }

    // Regular dependencies
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.springframework:spring-messaging:6.1.10")
    implementation("org.springframework:spring-websocket:6.1.10")
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:1.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Firebase Data Connect dependencies for generated code
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-dataconnect")
    implementation("com.google.firebase:firebase-auth")
}

    // Regular dependencies
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.springframework:spring-messaging:6.1.10")
    implementation("org.springframework:spring-websocket:6.1.10")
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:1.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Firebase Data Connect dependencies for generated code
    implementation("com.google.firebase:firebase-dataconnect-ktx:1.0.0")
    implementation("com.google.firebase:firebase-dataconnect-generated-annotations:1.0.0")
}

    
    // Regular dependencies
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.springframework:spring-messaging:6.1.10")
    implementation("org.springframework:spring-websocket:6.1.10")
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:1.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        freeCompilerArgs.addAll(
            "-Xsuppress-version-warnings",
            "-Xallow-kotlin-package",
            "-Xcontext-receivers",
            "-opt-in=org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            "-Xbackend-threads=8"
        )
    }
}

intellijPlatform {
    pluginConfiguration {
        name.set("SupremeAI Assistant")
        description.set("SupremeAI Assistant is a powerful AI-driven tool for Android Studio that helps you generate entire Android applications from natural language prompts, provides real-time code learning and context-aware suggestions, and offers K2 Mode compatibility for the latest IDE performance.")
        ideaVersion {
            sinceBuild.set("242")
            untilBuild.set("253.*")
        }
        
        vendor {
            name.set("SupremeAI")
        }
    }
    
    buildSearchableOptions = false
    instrumentCode = false
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}
