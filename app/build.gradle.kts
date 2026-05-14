import java.util.Properties
import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Read .env file for API keys. Keep this UTF-8 so Vietnamese comments/text never get mangled.
val envFile = rootProject.file(".env")
val envProps = Properties()
if (envFile.exists()) {
    envFile.readLines(Charsets.UTF_8).forEach { line ->
        if (line.isNotBlank() && !line.startsWith("#")) {
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) {
                envProps[parts[0].trim()] = parts[1].trim().trim('"').trim('\'')
            }
        }
    }
}

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

fun Properties.getIntOrDefault(name: String, defaultValue: Int): Int =
    getProperty(name)?.toIntOrNull() ?: defaultValue

fun Properties.getSortedKeys(regex: Regex, baseName: String? = null): List<String> {
    return stringPropertyNames()
        .filter { key -> key.matches(regex) || key == baseName }
        .sortedWith(
            compareBy<String> { key ->
                if (key == baseName) 0 else key.substringAfterLast("_").toIntOrNull() ?: Int.MAX_VALUE
            }.thenBy { it }
        )
        .mapNotNull { key -> getProperty(key)?.takeIf { it.isNotBlank() } }
}

fun buildConfigStringArray(values: List<String>): String =
    values.joinToString(prefix = "{", postfix = "}") { it.asBuildConfigString() }

extensions.configure<ApplicationExtension> {
    namespace = "com.novel.assistant"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.novel.assistant"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject API keys from .env
        val mainKeys = envProps.getSortedKeys(
            regex = Regex("""GEMINI_API_KEY_\d+"""),
            baseName = "GEMINI_API_KEY"
        )
        val memoryKeys = envProps.getSortedKeys(Regex("""GEMINI_API_KEY_MEMORY_\d+"""))
        val generatorKeys = envProps.getSortedKeys(Regex("""GEMINI_API_KEY_GENERATOR_\d+"""))

        buildConfigField("String[]", "GEMINI_MAIN_KEYS",
            buildConfigStringArray(mainKeys))
        buildConfigField("String[]", "GEMINI_MEMORY_KEYS",
            buildConfigStringArray(memoryKeys))
        buildConfigField("String[]", "GEMINI_GENERATOR_KEYS",
            buildConfigStringArray(generatorKeys))
        buildConfigField("String", "GEMINI_MODEL_MAIN",
            envProps.getProperty("GEMINI_MODEL_MAIN", "gemini-2.0-flash").asBuildConfigString())
        buildConfigField("String", "GEMINI_MODEL_MEMORY",
            envProps.getProperty("GEMINI_MODEL_MEMORY", "gemini-2.0-flash").asBuildConfigString())
        buildConfigField("int", "GEMINI_MAX_OUTPUT_TOKENS_MAIN",
            envProps.getIntOrDefault("GEMINI_MAX_OUTPUT_TOKENS_MAIN", 8192).toString())
        buildConfigField("int", "GEMINI_MAX_OUTPUT_TOKENS_MEMORY",
            envProps.getIntOrDefault("GEMINI_MAX_OUTPUT_TOKENS_MEMORY", 2048).toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network (Gemini REST API - không dùng deprecated SDK)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // DataStore
    implementation(libs.datastore.preferences)

    // Gson
    implementation(libs.gson)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // WorkManager
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
