plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Enable Firebase only once the config file is dropped in (app/google-services.json).
// This keeps every other build working when Firebase isn't set up.
val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.medremind.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.medremind.app"
        minSdk = 26
        targetSdk = 35
        // CI passes -PversionCode/-PversionName so every cloud build increments.
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 183
        versionName = (project.findProperty("versionName") as String?)?.toString() ?: "0.182.0"
    }

    signingConfigs {
        // Signed with the same debug keystore so the optimized release build can
        // update over the test (debug) install without uninstalling.
        create("deviceDebug") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            // A proper (non-debuggable) release APK, like the ones that installed
            // fine — but without R8 for much faster builds on this device. Set
            // VARIANT=release-minify in release.sh (or flip these to true) for the
            // small Play-ready build.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("deviceDebug")
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        // The bundled lint crashes on some Compose files; it isn't needed for
        // building the APK, so don't let it block release builds.
        checkReleaseBuilds = false
        abortOnError = false
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
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    // Periodic background check for caregiver reminders (delivery while closed).
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    // QR generation (core) + in-app QR scanning for caregiver pairing.
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Firebase backend (auth, database, push). Activated by google-services.json;
    // the libraries stay inert until FirebaseApp is initialised from that config.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")

    // Google sign-in (account picker) + Firebase credential exchange.
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}