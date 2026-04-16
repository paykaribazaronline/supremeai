plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij.platform") version "2.2.0"
}

group = "com.supremeai"
version = "1.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        freeCompilerArgs.addAll(
            "-Xsuppress-version-warnings",
            "-Xuse-k2",
            "-Xallow-kotlin-package",
            "-Xcontext-receivers"
        )
    }
}

intellijPlatform {
    pluginConfiguration {
        name.set("SupremeAI Assistant")
        description.set("Fully Compatible with K2 Mode")
        ideaVersion {
            sinceBuild.set("243")
            untilBuild.set("251.*")
        }
        
        vendor {
            name.set("SupremeAI")
        }
        

    }
    
    buildSearchableOptions = false
    instrumentCode = true
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}



// Final fix for "Plugin is incompatible with the Kotlin plugin in K2 mode" error
tasks.withType<org.jetbrains.intellij.platform.gradle.tasks.BuildPluginTask> {
    doFirst {
        val pluginXml = destinationDirectory.file("META-INF/plugin.xml").get().asFile
        if (pluginXml.exists()) {
            var content = pluginXml.readText()
            if (!content.contains("k2-support.xml")) {
                content = content.replace(
                    "</idea-plugin>",
                    """
  <depends config-file="k2-support.xml">org.jetbrains.kotlin</depends>
</idea-plugin>
""".trimIndent()
                )
                pluginXml.writeText(content)
            }
        }
    }
}
