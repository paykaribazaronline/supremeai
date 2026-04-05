import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
    // Firebase Google Services plugin
    id("com.google.gms.google-services")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

fun resolveSecret(propertyKey: String, envKey: String): String? {
    val fromFile = keystoreProperties.getProperty(propertyKey)?.trim()
    if (!fromFile.isNullOrEmpty()) {
        return fromFile
    }
    return System.getenv(envKey)?.trim()?.takeIf { it.isNotEmpty() }
}

val signingStoreFile = resolveSecret("storeFile", "ANDROID_STORE_FILE")
val signingStorePassword = resolveSecret("storePassword", "ANDROID_STORE_PASSWORD")
val signingKeyAlias = resolveSecret("keyAlias", "ANDROID_KEY_ALIAS")
val signingKeyPassword = resolveSecret("keyPassword", "ANDROID_KEY_PASSWORD")

val hasReleaseSigning = !signingStoreFile.isNullOrEmpty() &&
    !signingStorePassword.isNullOrEmpty() &&
    !signingKeyAlias.isNullOrEmpty() &&
    !signingKeyPassword.isNullOrEmpty()

val requireReleaseSigning = (System.getenv("REQUIRE_RELEASE_SIGNING") ?: "false")
    .equals("true", ignoreCase = true)

android {
    namespace = "com.example.supremeai_admin"
    compileSdk = 36  // Updated from flutter.compileSdkVersion (35) to 36 for androidx.core compatibility
    ndkVersion = "27.0.12077973"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "supremeai.com"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = 24  // Explicitly pinned to verified compatibility level from recent builds
        targetSdk = 36  // Updated from flutter.targetSdkVersion to 36 for androidx.core compatibility
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                if (requireReleaseSigning) {
                    throw GradleException(
                        "Release signing is required but missing. Provide keystore.properties " +
                            "(storeFile/storePassword/keyAlias/keyPassword) or ANDROID_STORE_* env vars."
                    )
                }

                // Trial builds can fall back to debug signing when release secrets are absent.
                logger.warn("⚠️ Release build is using DEBUG signing because release signing is not configured.")
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }
}

flutter {
    source = "../.."
}
