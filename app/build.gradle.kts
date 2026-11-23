plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Build-Nummer aus GitHub Actions → jede neue APK installiert sich über die alte
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
// Signierschlüssel kommt im Workflow aus dem Secret KEYSTORE_B64 (ohne Secret: unsignierter Testbuild)
val keystoreFile: String? = System.getenv("KEYSTORE_FILE")

android {
    namespace = "de.philipdecker.minimal"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.philipdecker.minimal"
        minSdk = 26
        targetSdk = 36
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storeType = "pkcs12"
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = "minimal"
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (keystoreFile != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
}
