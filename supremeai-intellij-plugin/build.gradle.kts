plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.15.0"
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
}

group = "com.supremeai"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.2")
    type.set("IC") // IntelliJ Community
    plugins.set(listOf("android", "Kotlin"))
}

tasks {
    patchPluginXml {
        changeNotes.set("""
          Initial release of SupremeAI Assistant.
          - App Generation
          - Code Learning Mode
        """.trimIndent())
    }
}
