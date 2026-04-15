plugins {
    id("java")
    // Use the latest 1.x version for better Ladybug support
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

group = "com.supremeai"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

intellij {
    // AS Ladybug (253) is stable with 2024.1.6 as build base
    version.set("2024.1.6") 
    type.set("IC") 
    // This is the correct list for AS Ladybug
    plugins.set(listOf("java", "org.jetbrains.kotlin"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("261")
        changeNotes.set("""
          - Resolved 'kotlin.plugin.k1.xml' xi:include resolution error
          - Full Support for Android Studio Ladybug (Build 253+)
          - Seamless K2 Mode integration
        """.trimIndent())
    }

    runIde {
        jvmArgs("-Didea.kotlin.plugin.use.k2=true")
    }
}
