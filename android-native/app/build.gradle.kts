plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val betaKeystorePath = providers.environmentVariable("MI_PLATA_KEYSTORE_PATH").orNull
val betaKeystorePassword = providers.environmentVariable("MI_PLATA_KEYSTORE_PASSWORD").orNull
val betaKeyAlias = providers.environmentVariable("MI_PLATA_KEY_ALIAS").orNull
val betaKeyPassword = providers.environmentVariable("MI_PLATA_KEY_PASSWORD").orNull

android {
    namespace = "com.bustos.miplata"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bustos.miplata"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "0.1.0-beta.6"
    }

    signingConfigs {
        create("beta") {
            storeFile = betaKeystorePath?.let { file(it) }
            storePassword = betaKeystorePassword
            keyAlias = betaKeyAlias
            keyPassword = betaKeyPassword
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".preview"
            signingConfig = signingConfigs.getByName("beta")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // This stable Compose BOM stays compatible with compileSdk 36 and AGP 8.13.
    val composeBom = platform("androidx.compose:compose-bom:2025.12.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.datastore:datastore-preferences:1.3.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
