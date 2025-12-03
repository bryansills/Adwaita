plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ninja.bryansills.adwaita"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "ninja.bryansills.adwaita"
        minSdk = 7
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("release/app-debug.jks")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            signingConfig = signingConfigs["debug"]
//            isShrinkResources = true
//            isMinifyEnabled = true // causes the app to crash on API 7???
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("com.android.support:appcompat-v7:25.4.0")
    implementation("androidx.multidex:multidex:2.0.1")
}