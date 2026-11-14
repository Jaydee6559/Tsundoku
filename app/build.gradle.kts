plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Room writes its database schema here so future upgrades (migrations) can be checked. Commit this folder.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

// ---------------------------------------------------------------------------
// Release signing + versioning come ONLY from environment variables.
// In CI they are filled from GitHub Secrets; nothing secret lives in this repo.
// ---------------------------------------------------------------------------
fun env(name: String): String? = providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }

val releaseKeystorePath = env("TSUNDOKU_KEYSTORE_PATH")
val hasReleaseSigning = releaseKeystorePath != null &&
    env("TSUNDOKU_KEYSTORE_PASSWORD") != null &&
    env("TSUNDOKU_KEY_ALIAS") != null &&
    env("TSUNDOKU_KEY_PASSWORD") != null

// CI passes the run number so every published build has a higher versionCode (Android requires it for updates).
val buildNumber = env("TSUNDOKU_BUILD_NUMBER")?.toIntOrNull() ?: 1
// v1.0: every published build is "1.0.<build number>" (e.g. 1.0.25).
val baseVersionName = "1.0"

android {
    namespace = "com.tsundoku.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tsundoku.app"
        minSdk = 30 // Android 11: first version with "All files access"
        targetSdk = 35
        versionCode = buildNumber
        versionName = if (env("TSUNDOKU_BUILD_NUMBER") != null) "$baseVersionName.$buildNumber" else baseVersionName
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = env("TSUNDOKU_KEYSTORE_PASSWORD")
                keyAlias = env("TSUNDOKU_KEY_ALIAS")
                keyPassword = env("TSUNDOKU_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug" // debug and release can be installed side by side
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Signed only when the key is provided (CI). Without it, the release APK is left unsigned.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        buildConfig = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
