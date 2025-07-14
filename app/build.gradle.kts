plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")               // Para el procesamiento de anotaciones
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")// Para Hilt
}

android {
    namespace = "com.jhone.app_inventory"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jhone.app_inventory"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        compose = true
        viewBinding = true   // Habilita View Binding si lo deseas
        // dataBinding = true  // Descomenta esta línea si prefieres Data Binding
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.0" // Ajusta según la versión actual
    }
}

dependencies {
    // 1. Dependencias básicas
    implementation("androidx.core:core-ktx:1.15.0")

    // 2. Lifecycle (MVVM: ViewModel y LiveData)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

    // 3. Activity y Jetpack Compose
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // 4. Navigation Component para Compose
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // 6. Kotlin Coroutines (Comunicación con Servicios Web)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 7. Hilt (Inyección de Dependencias)
    implementation("com.google.dagger:hilt-android:2.49")
    kapt("com.google.dagger:hilt-compiler:2.49")
    // Hilt Navigation Compose (si usas Hilt junto con Compose)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Import the Firebase BoM
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:32.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3") // Corrutinas con Firebase
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // 8. Dependencias para Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
