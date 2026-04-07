import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun resolveLocalOrGradleProperty(name: String): String? =
    providers.gradleProperty(name).orNull ?: localProperties.getProperty(name)

fun asBuildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val emulatorApiBaseUrl = resolveLocalOrGradleProperty("emergencyLaneEmulatorBaseUrl") ?: "http://10.0.2.2:8000/api/"
val debugLanApiBaseUrl = resolveLocalOrGradleProperty("emergencyLaneDebugLanBaseUrl")
val configuredApiBaseUrl = resolveLocalOrGradleProperty("emergencyLaneApiBaseUrl")
val resolvedApiBaseUrl = configuredApiBaseUrl ?: debugLanApiBaseUrl ?: emulatorApiBaseUrl
val resolvedApiBaseUrlSource = when {
    configuredApiBaseUrl != null -> "gradle/local override"
    debugLanApiBaseUrl != null -> "debug LAN default"
    else -> "emulator fallback"
}
val lanApiBaseUrlHint = debugLanApiBaseUrl ?: "http://<LAN_IP>:8000/api/"

android {
    namespace = "com.yrd.emergencylanemobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yrd.emergencylanemobile"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", asBuildConfigString(resolvedApiBaseUrl))
        buildConfigField("String", "API_BASE_URL_SOURCE", asBuildConfigString(resolvedApiBaseUrlSource))
        buildConfigField("String", "API_BASE_URL_EMULATOR", asBuildConfigString(emulatorApiBaseUrl))
        buildConfigField("String", "API_BASE_URL_LAN_HINT", asBuildConfigString(lanApiBaseUrlHint))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
