plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.10"
    id("org.jetbrains.intellij.platform") version "2.2.0"
}

group = "com.supremeai"
version = "1.2.0"

repositories {
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        androidStudio("2024.3.2.15")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("org.jetbrains.android")
    }
    
    // Regular dependencies
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.springframework:spring-messaging:6.1.10")
    implementation("org.springframework:spring-websocket:6.1.10")
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:1.21")
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
            "-opt-in=org.jetbrains.kotlin.analysis.api.KaExperimentalApi"
        )
    }
}

intellijPlatform {
    pluginConfiguration {
        name.set("SupremeAI Assistant")
        description.set("SupremeAI Assistant is a powerful AI-driven tool for Android Studio that helps you generate entire Android applications from natural language prompts, provides real-time code learning and context-aware suggestions, and offers K2 Mode compatibility for the latest IDE performance.")
        ideaVersion {
            sinceBuild.set("243")
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
