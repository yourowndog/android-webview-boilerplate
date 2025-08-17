plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.daemon.portal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.daemon.portal"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String","HOME_URL","\"https://chatgpt.com/g/XXXXXXXX-the-daemon\"")
        buildConfigField("boolean","FORCE_DESKTOP_MODE","true")
        buildConfigField("boolean","ALLOW_THIRD_PARTY_COOKIES","true")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
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
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.webkit:webkit:1.11.0")
}
