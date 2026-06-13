plugins {
    alias(libs.plugins.android.application)
    kotlin("plugin.serialization") version "2.1.10"
}

android {
    namespace = "one.ethanthesleepy.androidew"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ethanthesleepyone.androidew.cdn"
        minSdk = 26
        targetSdk = 36
        versionCode = 28
        versionName = "2.6.2-cdn-pathfix"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
        }

        buildConfigField("String", "APP_VERSION_NUM", "\"${versionName}\"")
        buildConfigField("String", "SERVER_VERSION_NUM", "\"1.1.0-zhcht-mt\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("lovelive.keystore")
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEYSTORE_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.kotlinx.serialization.json)
}
