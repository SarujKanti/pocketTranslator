plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.skd.dictionary"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.skd.dictionary"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
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

    dependencies {

        // Lifecycle (MVVM)
        implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
        implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")

        // ML Kit Translation (core feature)
        implementation ("com.google.mlkit:translate:17.0.3")

        // Coroutines
        implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

        // RecyclerView (for language list later)
        implementation ("androidx.recyclerview:recyclerview:1.3.2")

        implementation ("com.squareup.retrofit2:retrofit:2.11.0")
        implementation ("com.squareup.retrofit2:converter-gson:2.11.0")
    }

}