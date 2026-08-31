plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "dev.soranerai.selectivewebviewtest"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.soranerai.selectivewebviewtest"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-debug"
    }
}
