plugins {
    id("com.android.application") version "8.4.2" apply false
    kotlin("android") version "1.9.24" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
