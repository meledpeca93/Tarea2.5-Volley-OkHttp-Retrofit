plugins {
    id("com.android.application")
}

android {
    namespace = "com.melvinpena.comparativa"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.melvinpena.comparativa"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // Development default; replace with the confirmed API URL before live testing.
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5000/\"")
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")
}
