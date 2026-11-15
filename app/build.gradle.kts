plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ctoadi.notstore"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Different from the older com.ctoadi.notstore build so both can be installed side by side.
        applicationId = "com.ctoadi.notstore.v2"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // Compose is much slower in debug builds. Release builds are optimized by R8 and
            // signed with the debug key so they can be installed directly while developing.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    lint {
        // The release-time lint pass needs extra downloads; run `./gradlew lint` separately.
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    // Installs the ahead-of-time compilation profiles that Compose ships with.
    implementation(libs.androidx.profileinstaller)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
