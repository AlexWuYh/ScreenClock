plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.screenclock"
    compileSdk {
        // API 37 ships as platforms/android-37.0. release(37) resolves that base platform.
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.screenclock"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "0.4.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The long-lived release key stays outside git; CI reconstructs it from Actions Secrets.
    // Local debug builds and unit tests do not require signing credentials.
    val releaseSigning = System.getenv("SC_RELEASE_KEYSTORE")?.takeIf { it.isNotBlank() }?.let { path ->
        signingConfigs.create("release") {
            storeFile = file(path)
            storePassword = requireNotNull(System.getenv("SC_RELEASE_STORE_PASSWORD"))
            keyAlias = "screenclock-release"
            keyPassword = requireNotNull(System.getenv("SC_RELEASE_KEY_PASSWORD"))
        }
    }

    buildTypes {
        release {
            releaseSigning?.let { signingConfig = it }
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.lunar)
    testImplementation(libs.junit)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
