import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  val localProperties = Properties()
  val localPropertiesFile = rootProject.file("local.properties")
  if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
  }

  defaultConfig {
    applicationId = "com.aistudio.tessera.xtrkna"
    minSdk = 26
    targetSdk = 35
    versionCode = 77
    versionName = "2.0.24"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    manifestPlaceholders["MAPS_API_KEY"] = "DUMMY_KEY"
    manifestPlaceholders["redirectSchemeName"] = "tessera"
  }

  signingConfigs {
    val keystorePath = "${rootDir}/my-upload-key.jks"
    val keystoreFile = file(keystorePath)
    if (keystoreFile.exists()) {
      create("release") {
        storeFile = keystoreFile
        storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD") ?: "android123"
        keyAlias = "upload"
        keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: "android123"
        enableV1Signing = true
        enableV2Signing = true
      }
    } else {
      // Graceful fallback to debug signature if release key is not found (avoids local build errors)
      create("release") {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = localProperties.getProperty("DEBUG_STORE_PASSWORD") ?: "android"
        keyAlias = "androiddebugkey"
        keyPassword = localProperties.getProperty("DEBUG_KEY_PASSWORD") ?: "android"
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = localProperties.getProperty("DEBUG_STORE_PASSWORD") ?: "android"
      keyAlias = "androiddebugkey"
      keyPassword = localProperties.getProperty("DEBUG_KEY_PASSWORD") ?: "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
    implementation("androidx.browser:browser:1.8.0")
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.coil.svg)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.firebase.firestore)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.maps.compose)
  implementation(libs.play.services.maps)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
  

  
  // RSS Parser
  implementation("com.prof18.rssparser:rssparser:6.0.10")

  // Gemini Generative AI
  implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
  
  implementation("androidx.health.connect:connect-client:1.1.0-alpha07")
  implementation("androidx.glance:glance-appwidget:1.1.0")
  implementation("androidx.glance:glance-material3:1.1.0")
  implementation("androidx.biometric:biometric:1.2.0-alpha05")
  implementation("androidx.fragment:fragment-ktx:1.6.2")
  
  // Lottie for animations
  implementation("com.airbnb.android:lottie-compose:6.4.0")

  // MediaPipe LLM Inference for Local Gemma Models
  implementation("com.google.mediapipe:tasks-genai:0.10.14")

  // Glassmorphism blur (Haze)
  implementation("dev.chrisbanes.haze:haze:1.1.1")
  
  // HTML Parser (Jsoup)
  implementation("org.jsoup:jsoup:1.17.2")
}

abstract class CopyApkTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.Input
    abstract val versionName: org.gradle.api.provider.Property<String>

    @get:org.gradle.api.tasks.InputDirectory
    abstract val inputDir: org.gradle.api.file.DirectoryProperty

    @get:org.gradle.api.tasks.OutputDirectory
    abstract val outputDir: org.gradle.api.file.DirectoryProperty

    @get:org.gradle.api.tasks.Input
    abstract val buildType: org.gradle.api.provider.Property<String>

    @org.gradle.api.tasks.TaskAction
    fun run() {
        val ver = versionName.get()
        val type = buildType.get()
        val inDir = inputDir.get().asFile
        val outDir = outputDir.get().asFile

        if (outDir.exists()) {
            outDir.listFiles { file ->
                file.isFile && (file.name == "app-$type.apk" || (file.name.startsWith("app-$type-") && file.name.endsWith(".apk")))
            }?.forEach { it.delete() }
        } else {
            outDir.mkdirs()
        }

        val inputFile = File(inDir, "app-$type.apk")
        if (inputFile.exists()) {
            val outputFile = File(outDir, "app-$type-$ver.apk")
            inputFile.copyTo(outputFile, overwrite = true)
        }

        // Keep README.md in sync with the new APK version automatically
        val readmeFile = File(outDir.parentFile, "README.md")
        if (readmeFile.exists()) {
            val originalContent = readmeFile.readText()
            val releaseRegex = Regex("""app-release-[^/\s)]+\.apk""")
            val debugRegex = Regex("""app-debug-[^/\s)]+\.apk""")
            var updatedContent = originalContent.replace(releaseRegex, "app-release-$ver.apk")
            updatedContent = updatedContent.replace(debugRegex, "app-debug-$ver.apk")
            if (updatedContent != originalContent) {
                readmeFile.writeText(updatedContent)
            }
        }
    }
}

val verName = provider { android.defaultConfig.versionName ?: "1.0.1" }

tasks.register<CopyApkTask>("copyReleaseApk") {
    dependsOn("assembleRelease")
    versionName.set(verName)
    buildType.set("release")
    inputDir.set(layout.buildDirectory.dir("outputs/apk/release"))
    outputDir.set(rootProject.layout.projectDirectory.dir(".build-outputs"))
}

tasks.register<CopyApkTask>("copyDebugApk") {
    dependsOn("assembleDebug")
    versionName.set(verName)
    buildType.set("debug")
    inputDir.set(layout.buildDirectory.dir("outputs/apk/debug"))
    outputDir.set(rootProject.layout.projectDirectory.dir(".build-outputs"))
}

tasks.configureEach {
    if (name == "assembleRelease") {
        finalizedBy("copyReleaseApk")
    }
    if (name == "assembleDebug") {
        finalizedBy("copyDebugApk")
    }
}

// Ensure BuildConfig is regenerated when .env changes
tasks.withType<com.android.build.gradle.tasks.GenerateBuildConfig> {
    inputs.file(rootProject.file(".env"))
}

