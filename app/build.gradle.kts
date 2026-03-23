plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.medicamentos"
    compileSdk = 35 // Actualizado a 35 para mejor compatibilidad con Android 15

    defaultConfig {
        applicationId = "com.example.medicamentos" // Corregido 'ejemplo' por 'example' para que coincida con el namespace
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // --- SOLUCIÓN PARA EL AVISO DE 16 KB ---
    packaging {
        jniLibs {
            // Esto fuerza a que las librerías nativas se empaqueten sin comprimir
            // permitiendo que el sistema las alinee a 16 KB automáticamente.
            useLegacyPackaging = false
        }
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures { compose = true }

    // Asegúrate de que esta versión coincida con tu versión de Kotlin en libs.versions.toml
    composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit - Asegúrate de que en libs.versions.toml esta versión sea la 16.0.0 o superior
    implementation(libs.play.services.mlkit.text.recognition)

    // Permisos
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
}