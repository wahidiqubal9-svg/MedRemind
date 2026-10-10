plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
    // Applied in :app only when app/google-services.json exists (see app/build.gradle.kts),
    // so local/CI builds without a Firebase project still succeed.
    id("com.google.gms.google-services") version "4.5.0" apply false
}
