plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// The person-facing version comes from the GitHub Release tag (e.g. v1.2.3), passed in
// by the CI workflow as -PversionNameOverride=1.2.3 -PversionCodeOverride=<run number>.
// Building locally (Android Studio, or ./gradlew without those flags) falls back to a
// clearly-marked dev version so it's never confused with a real release build.
val appVersionName: String = (project.findProperty("versionNameOverride") as String?) ?: "0.0.0-dev"
val appVersionCode: Int = (project.findProperty("versionCodeOverride") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.stephaneperez.notepad"
    compileSdk = 34

    // Fixed debug signing key, committed to the repo (app/debug.keystore — see README,
    // "Fixed debug signing key"). Without this, every CI run signs with a fresh,
    // ephemeral debug key (a new build machine each time), which forces a full
    // uninstall before every install and destroys the on-device Keystore encryption
    // key along with it. A stable signature turns every new build into a normal
    // in-place update instead.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.stephaneperez.notepad"
        minSdk = 26
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // Persist wrap / lineNumbers preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
