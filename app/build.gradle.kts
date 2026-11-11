plugins {
    id("com.android.application")
}

android {
    namespace = "com.melvinpena.comparativa"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.melvinpena.comparativa"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")
}
