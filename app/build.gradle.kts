
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.arhideandseek"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.arhideandseek"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("com.google.ar:core:1.52.0")
}
