plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)

    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.authapp"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.authapp"

        minSdk = 24
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Material
    implementation(libs.material)
    implementation(libs.cardview)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)

    // Navigation
    implementation(
        libs.androidx.navigation.fragment.ktx
    )

    implementation(
        libs.androidx.navigation.ui.ktx
    )

    // Hilt
    implementation(libs.hilt.android)

    kapt(
        libs.hilt.compiler
    )

    // Firebase
    implementation(
        platform(
            "com.google.firebase:firebase-bom:33.1.0"
        )
    )

    implementation(
        "com.google.firebase:firebase-auth-ktx"
    )

    implementation(
        "com.google.firebase:firebase-firestore-ktx"
    )

    // Coroutines
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // Image loading
    implementation(
        "io.coil-kt:coil:2.6.0"
    )

    // Unit Testing
    testImplementation(libs.junit)

    testImplementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1"
    )

    testImplementation(
        "io.mockk:mockk:1.13.10"
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )
}

kapt {
    correctErrorTypes = true
}