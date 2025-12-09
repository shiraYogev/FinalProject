import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)

}

// Load GEMINI_API_KEY from local.properties (project root).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Fallbacks: gradle.properties or environment variable if needed
val geminiKey: String = localProps.getProperty("GEMINI_API_KEY")
    ?: (project.findProperty("GEMINI_API_KEY") as String?)
    ?: System.getenv("GEMINI_API_KEY")
    ?: ""

// (אופציונלי לדיבוג: לא מדפיסים את המפתח עצמו)
println("GEMINI_API_KEY present in build? ${geminiKey.isNotEmpty()} length=${geminiKey.length}")

val googleMapsKey: String = localProps.getProperty("GOOGLE_MAPS_API_KEY")
    ?: (project.findProperty("GOOGLE_MAPS_API_KEY") as String?)
    ?: System.getenv("GOOGLE_MAPS_API_KEY")
    ?: ""

// (אופציונלי לדיבוג)
println("GOOGLE_MAPS_API_KEY present in build? ${googleMapsKey.isNotEmpty()} length=${googleMapsKey.length}")

android {
    namespace = "com.example.finalprojectappraisal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.finalprojectappraisal"
        minSdk = 26
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Expose key to BuildConfig
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", googleMapsKey)
        manifestPlaceholders["googleMapsApiKey"] = googleMapsKey

        // ✅ Enable VectorDrawableCompat for all API levels
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
        }
    }


}

dependencies {
    // Core Android libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase BoM (Bill of Materials) - מנהל את כל גרסאות Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Google Play Services & Auth
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // UI Components
    implementation("androidx.cardview:cardview:1.0.0")

    // AI & Cloud Services
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    //implementation("com.google.ai.client.generativeai:generativeai:0.2.1")
    implementation("com.google.cloud:google-cloud-storage:2.22.5") {
        exclude(group = "com.google.api.grpc", module = "proto-google-common-protos")
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation(libs.firebase.storage)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // AndroidX + UI
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation ("com.google.android.flexbox:flexbox:3.0.0")

    // Lifecycle (כי את משתמשת ב-LiveData)
    implementation("androidx.lifecycle:lifecycle-livedata:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.9.4") // :contentReference[oaicite:4]{index=4}


    // Google Places API
    //implementation ("com.google.android.libraries.places:places:3.3.0")
    implementation("com.google.android.libraries.places:places:5.0.0")

    implementation ("androidx.recyclerview:recyclerview:1.3.2")



}

