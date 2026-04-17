plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
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
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
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
        description.set("Fully Compatible with K2 Mode")
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
